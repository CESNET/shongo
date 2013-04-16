package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.api.Reservation;
import cz.cesnet.shongo.controller.executor.*;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Component of a domain controller which executes actions according to allocation plan which was created
 * by the {@link Scheduler}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Executor extends Component
        implements Component.WithThread, Component.EntityManagerFactoryAware, Component.ControllerAgentAware, Runnable
{
    /**
     * {@link Logger} for {@link Executor}
     */
    private static Logger logger = LoggerFactory.getLogger(Executor.class);

    /**
     * {@link EntityManagerFactory} used for loading {@link Executable}s for execution.
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see ControllerAgent
     */
    private ControllerAgent controllerAgent;

    /**
     * @see Configuration#EXECUTOR_PERIOD
     */
    private Duration period;

    /**
     * @see Configuration#EXECUTOR_EXECUTABLE_START
     */
    private Duration executableStart;

    /**
     * @see Configuration#EXECUTOR_EXECUTABLE_END
     */
    private Duration executableEnd;

    /**
     * @see Configuration#EXECUTOR_STARTING_DURATION_ROOM
     */
    private Duration startingDurationRoom;

    /**
     * @see Configuration#EXECUTOR_EXECUTABLE_NEXT_ATTEMPT
     */
    private Duration nextAttempt;

    /**
     * @return {@link #logger}
     */
    public Logger getLogger()
    {
        return logger;
    }

    /**
     * @return {@link #startingDurationRoom}
     */
    public Duration getStartingDurationRoom()
    {
        return startingDurationRoom;
    }

    /**
     * @return {@link #nextAttempt}
     */
    public Duration getNextAttempt()
    {
        return nextAttempt;
    }

    @Override
    public Thread getThread()
    {
        Thread thread = new Thread(this);
        thread.setName("executor");
        return thread;
    }

    /**
     * @return new {@link EntityManager}
     */
    public EntityManager getEntityManager()
    {
        return entityManagerFactory.createEntityManager();
    }

    @Override
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * @return {@link #controllerAgent}
     */
    public ControllerAgent getControllerAgent()
    {
        return controllerAgent;
    }

    @Override
    public void setControllerAgent(ControllerAgent controllerAgent)
    {
        this.controllerAgent = controllerAgent;
    }

    @Override
    public void init(Configuration configuration)
    {
        checkDependency(entityManagerFactory, EntityManagerFactory.class);
        super.init(configuration);

        period = configuration.getDuration(Configuration.EXECUTOR_PERIOD);
        executableStart = configuration.getDuration(Configuration.EXECUTOR_EXECUTABLE_START);
        executableEnd = configuration.getDuration(Configuration.EXECUTOR_EXECUTABLE_END);
        nextAttempt = configuration.getDuration(Configuration.EXECUTOR_EXECUTABLE_NEXT_ATTEMPT);
        startingDurationRoom = configuration.getDuration(Configuration.EXECUTOR_STARTING_DURATION_ROOM);
    }

    @Override
    public void run()
    {
        logger.debug("Executor started!");

        while (!Thread.interrupted()) {
            try {
                Thread.sleep(period.getMillis());
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                continue;
            }
            synchronized (ThreadLock.class) {
                execute(DateTime.now());
            }
        }

        logger.debug("Executor stopped!");
    }

    /**
     * Execute {@link Reservation}s which should be executed for given {@code interval}.
     *
     * @param referenceDateTime specifies date/time which should be used as "now" executing {@link Reservation}s
     * @return {@link cz.cesnet.shongo.controller.executor.ExecutionResult}
     */
    public ExecutionResult execute(DateTime referenceDateTime)
    {
        ExecutionResult executionResult = new ExecutionResult();

        // Globally synchronized (see ThreadLock documentation)
        //logger.info("Executor waiting for lock...............................");
        synchronized (ThreadLock.class) {
            //logger.info("Executor lock acquired...     (((((");

            logger.debug("Checking executables for execution at '{}'...", Temporal.formatDateTime(referenceDateTime));

            EntityManager entityManager = entityManagerFactory.createEntityManager();
            ExecutableManager executableManager = new ExecutableManager(entityManager);
            try {
                // List executables which should be stopped
                DateTime stopDateTime = referenceDateTime.minus(executableEnd);
                ExecutionPlan stoppingExecutionPlan = new ReverseExecutionPlan(
                        executableManager.listExecutablesForStop(referenceDateTime, stopDateTime));
                Set<Executable> stoppingExecutables = new HashSet<Executable>();
                while (!stoppingExecutionPlan.isEmpty()) {
                    Collection<Executable> executables = stoppingExecutionPlan.popExecutables();
                    for (Executable executable : executables) {
                        stoppingExecutables.add(executable);
                        ExecutorThread executorThread =
                                new ExecutorThread(ExecutorThread.Type.STOP, executable, this, stoppingExecutionPlan);
                        executorThread.start();
                    }
                    try {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException exception) {
                        logger.error("Execution interrupted.", exception);
                    }
                }
                entityManager.getTransaction().begin();
                for (Executable executable : stoppingExecutables) {
                    entityManager.refresh(executable);
                    if (executable.getState().equals(Executable.State.SKIPPED)) {
                        executable.setState(executable.getDefaultState());
                    }
                    if (!executable.getState().isStarted()) {
                        executionResult.addStoppedExecutable(executable);
                    }
                }
                entityManager.getTransaction().commit();

                // List executables which should be started
                DateTime startDateTime = referenceDateTime.minus(executableStart);
                ExecutionPlan startingExecutionPlan = new ExecutionPlan(
                        executableManager.listExecutablesForStart(referenceDateTime, startDateTime));
                Collection<Executable> startingExecutables = new ArrayList<Executable>();
                while (!startingExecutionPlan.isEmpty()) {
                    Collection<Executable> executables = startingExecutionPlan.popExecutables();
                    for (Executable executable : executables) {
                        startingExecutables.add(executable);
                        ExecutorThread executorThread =
                                new ExecutorThread(ExecutorThread.Type.START, executable, this, startingExecutionPlan);
                        executorThread.start();
                    }
                    try {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException exception) {
                        logger.error("Execution interrupted.", exception);
                    }
                }
                for (Executable executable : startingExecutables) {
                    entityManager.refresh(executable);
                    if (executable.getState().isStarted()) {
                        executionResult.addStartedExecutable(executable);
                    }
                }

                // List executables which should be updated
                ExecutionPlan updatingExecutionPlan = new ExecutionPlan(
                        executableManager.listExecutablesForUpdate(referenceDateTime));
                Collection<Executable> updatingExecutables = new ArrayList<Executable>();
                while (!updatingExecutionPlan.isEmpty()) {
                    Collection<Executable> executables = updatingExecutionPlan.popExecutables();
                    for (Executable executable : executables) {
                        updatingExecutables.add(executable);
                        ExecutorThread executorThread =
                                new ExecutorThread(ExecutorThread.Type.UPDATE, executable, this, updatingExecutionPlan);
                        executorThread.start();
                    }
                    try {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException exception) {
                        logger.error("Execution interrupted.", exception);
                    }
                }
                for (Executable executable : updatingExecutables) {
                    entityManager.refresh(executable);
                    if (!executable.getState().isModified()) {
                        executionResult.addUpdatedExecutable(executable);
                    }
                }
            }
            catch (Exception exception) {
                Reporter.reportInternalError(Reporter.InternalErrorType.EXECUTOR, exception);
            }
            finally {
                entityManager.close();
            }

            //logger.info("Executor releasing lock...    )))))");
        }
        //logger.info("Executor lock released...");

        return executionResult;
    }
}
