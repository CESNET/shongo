package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.api.Reservation;
import cz.cesnet.shongo.controller.executor.Executable;
import cz.cesnet.shongo.controller.executor.ExecutableManager;
import cz.cesnet.shongo.controller.executor.ExecutorThread;
import cz.cesnet.shongo.util.TemporalHelper;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * Component of a domain controller which executes actions according to allocation plan which was created
 * by the {@link Scheduler}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Executor extends Component
        implements Component.WithThread, Component.EntityManagerFactoryAware, Component.ControllerAgentAware, Runnable
{
    private static Logger logger = LoggerFactory.getLogger(Executor.class);

    /**
     * {@link EntityManagerFactory} used for loading {@link cz.cesnet.shongo.controller.executor.Compartment}s for execution.
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see ControllerAgent
     */
    private ControllerAgent controllerAgent;

    /**
     * @see {@link Configuration#EXECUTOR_PERIOD}
     */
    private Duration period;

    /**
     * @see {@link Configuration#EXECUTOR_LOOKUP_AHEAD}
     */
    private Duration lookupAhead;

    /**
     * @see {@link Configuration#EXECUTOR_COMPARTMENT_START}
     */
    private Duration compartmentStart;

    /**
     * @see {@link Configuration#EXECUTOR_COMPARTMENT_END}
     */
    private Duration compartmentEnd;

    /**
     * @see {@link Configuration#EXECUTOR_COMPARTMENT_WAITING_VIRTUAL_ROOM}
     */
    private Duration compartmentWaitingVirtualRoom;

    /**
     * @see {@link Configuration#EXECUTOR_COMPARTMENT_WAITING_START}
     */
    private Duration compartmentWaitingStart;

    /**
     * @see {@link Configuration#EXECUTOR_COMPARTMENT_WAITING_END}
     */
    private Duration compartmentWaitingEnd;

    /**
     * Map of executed {@link ExecutorThread}s by {@link cz.cesnet.shongo.controller.executor.Compartment} identifiers.
     */
    private Map<Long, ExecutorThread> executorThreadById = new HashMap<Long, ExecutorThread>();


    /**
     * @return {@link #compartmentStart}
     */
    public Duration getCompartmentStart()
    {
        return compartmentStart;
    }

    /**
     * @return {@link #compartmentEnd}
     */
    public Duration getCompartmentEnd()
    {
        return compartmentEnd;
    }

    /**
     * @return {@link #compartmentWaitingVirtualRoom}
     */
    public Duration getCompartmentWaitingVirtualRoom()
    {
        return compartmentWaitingVirtualRoom;
    }

    /**
     * @return {@link #compartmentWaitingStart}
     */
    public Duration getCompartmentWaitingStart()
    {
        return compartmentWaitingStart;
    }

    /**
     * @return {@link #compartmentWaitingEnd}
     */
    public Duration getCompartmentWaitingEnd()
    {
        return compartmentWaitingEnd;
    }

    @Override
    public Thread getThread()
    {
        Thread thread = new Thread(this);
        thread.setName("executor");
        return thread;
    }

    @Override
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
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
        lookupAhead = configuration.getDuration(Configuration.EXECUTOR_LOOKUP_AHEAD);
        compartmentStart = configuration.getDuration(Configuration.EXECUTOR_COMPARTMENT_START);
        compartmentEnd = configuration.getDuration(Configuration.EXECUTOR_COMPARTMENT_END);
        compartmentWaitingVirtualRoom = configuration.getDuration(
                Configuration.EXECUTOR_COMPARTMENT_WAITING_VIRTUAL_ROOM);
        compartmentWaitingStart = configuration.getDuration(Configuration.EXECUTOR_COMPARTMENT_WAITING_START);
        compartmentWaitingEnd = configuration.getDuration(Configuration.EXECUTOR_COMPARTMENT_WAITING_END);
    }

    @Override
    public void run()
    {
        logger.info("Executor started!");

        try {
            Thread.sleep(period.getMillis());
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        while (!Thread.interrupted()) {
            execute(DateTime.now());
            try {
                Thread.sleep(period.getMillis());
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                continue;
            }
        }

        for (ExecutorThread executorThread : executorThreadById.values()) {
            if (executorThread.isAlive()) {
                logger.debug("Killing '{}'...", executorThread.getName());
                executorThread.stop();
            }
        }

        logger.info("Executor stopped!");
    }

    /**
     * Execute {@link Reservation}s which should be executed for given {@code interval}.
     *
     * @param referenceDateTime specifies date/time which should be used as "now" executing {@link Reservation}s
     * @return list of executed {@link ExecutorThread}s
     */
    public List<ExecutorThread> execute(DateTime referenceDateTime)
    {
        List<ExecutorThread> executorThreads = new ArrayList<ExecutorThread>();
        Interval interval = new Interval(referenceDateTime, referenceDateTime.plus(lookupAhead));
        logger.info("Checking compartments for execution in '{}'...", TemporalHelper.formatInterval(interval));
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        List<Executable> executableList = executableManager.listExecutablesForExecution(interval);
        for (Executable executable : executableList) {
            Long compartmentId = executable.getId();
            if (executorThreadById.containsKey(compartmentId)) {
                continue;
            }
            ExecutorThread executorThread = new ExecutorThread(executable);
            executorThread.start(this, controllerAgent, entityManagerFactory);
            executorThreadById.put(compartmentId, executorThread);
            executorThreads.add(executorThread);
        }
        entityManager.close();
        return executorThreads;
    }

    /**
     * Wait for all {@link ExecutorThread}s from {@link #executorThreadById} to exit.
     */
    public void waitForThreads()
    {
        boolean active;
        do {
            active = false;
            for (ExecutorThread executorThread : executorThreadById.values()) {
                if (executorThread.isAlive()) {
                    active = true;
                }
            }
            try {
                Thread.sleep(100);
            }
            catch (InterruptedException exception) {
                throw new IllegalStateException(exception);
            }
        } while (active);

    }
}
