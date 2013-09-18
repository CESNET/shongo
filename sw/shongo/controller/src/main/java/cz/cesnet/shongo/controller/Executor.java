package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.api.Reservation;
import cz.cesnet.shongo.controller.executor.*;
import cz.cesnet.shongo.util.DateTimeFormatter;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.Collection;

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
     * @see Configuration#EXECUTOR_EXECUTABLE_MAX_ATTEMPT_COUNT
     */
    private int maxAttemptCount;

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

    /**
     * @return {@link #maxAttemptCount}
     */
    public int getMaxAttemptCount()
    {
        return maxAttemptCount;
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
        maxAttemptCount = configuration.getInt(Configuration.EXECUTOR_EXECUTABLE_MAX_ATTEMPT_COUNT);
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
     * @param dateTime specifies date/time which should be used as "now" executing {@link Reservation}s
     * @return {@link cz.cesnet.shongo.controller.executor.ExecutionResult}
     */
    public ExecutionResult execute(DateTime dateTime)
    {
        // Globally synchronized (see ThreadLock documentation)
        //logger.info("Executor waiting for lock...............................");
        synchronized (ThreadLock.class) {
            //logger.info("Executor lock acquired...     (((((");

            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.getInstance(DateTimeFormatter.Type.LONG);
            logger.debug("Checking executables for execution at '{}'...", dateTimeFormatter.formatDateTime(dateTime));

            EntityManager entityManager = entityManagerFactory.createEntityManager();
            ExecutableManager executableManager = new ExecutableManager(entityManager);
            try {
                // Create execution plan
                DateTime start = dateTime.plus(executableStart);
                DateTime stop = dateTime.plus(executableEnd);
                ExecutionPlan executionPlan = new ExecutionPlan(this);
                for (Executable executable : executableManager.listExecutablesForStart(start, maxAttemptCount)) {
                    executionPlan.addExecutionAction(new ExecutionAction.StartExecutableAction(executable));
                    Migration migration = executable.getMigration();
                    if (migration != null) {
                        executionPlan.addExecutionAction(new ExecutionAction.MigrationAction(migration));
                    }
                }
                for (Executable executable : executableManager.listExecutablesForUpdate(dateTime, maxAttemptCount)) {
                    executionPlan.addExecutionAction(new ExecutionAction.UpdateExecutableAction(executable));
                }
                for (Executable executable : executableManager.listExecutablesForStop(stop, maxAttemptCount)) {
                    executionPlan.addExecutionAction(new ExecutionAction.StopExecutableAction(executable));
                }
                executionPlan.build();

                // Perform execution plan
                while (!executionPlan.isEmpty()) {
                    Collection<ExecutionAction> executionActions = executionPlan.popExecutionActions();
                    for (ExecutionAction executionAction : executionActions) {
                        executionAction.start();
                    }
                    try {
                        Thread.sleep(100);
                    }
                    catch (InterruptedException exception) {
                        logger.error("Execution interrupted.", exception);
                    }
                }

                // Finish execution plan
                entityManager.getTransaction().begin();

                ExecutionResult executionResult = executionPlan.finish(entityManager);

                entityManager.getTransaction().commit();

                return executionResult;
            }
            catch (Exception exception) {
                Reporter.reportInternalError(Reporter.EXECUTOR, exception);
                return null;
            }
            finally {
                if (entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().rollback();
                }

                entityManager.close();
            }

            //logger.info("Executor releasing lock...    )))))");
        }
        //logger.info("Executor lock released...");
    }
}
