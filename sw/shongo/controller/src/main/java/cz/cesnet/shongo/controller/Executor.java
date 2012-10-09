package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.compartment.Compartment;
import cz.cesnet.shongo.controller.compartment.CompartmentExecutor;
import cz.cesnet.shongo.controller.compartment.CompartmentManager;
import cz.cesnet.shongo.controller.reservation.CompartmentReservation;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * {@link EntityManagerFactory} used for loading {@link Compartment}s for execution.
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
     * @see {@link Configuration#EXECUTOR_COMPARTMENT_WAITING_START}
     */
    private Duration compartmentWaitingStart;

    /**
     * @see {@link Configuration#EXECUTOR_COMPARTMENT_WAITING_END}
     */
    private Duration compartmentWaitingEnd;

    /**
     * Map of executed {@link CompartmentExecutor}s by {@link Compartment} identifiers.
     */
    private Map<Long, CompartmentExecutor> executorsById = new HashMap<Long, CompartmentExecutor>();


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
        super.init(configuration);

        period = configuration.getDuration(Configuration.EXECUTOR_PERIOD);
        lookupAhead = configuration.getDuration(Configuration.EXECUTOR_LOOKUP_AHEAD);
        compartmentStart = configuration.getDuration(Configuration.EXECUTOR_COMPARTMENT_START);
        compartmentEnd = configuration.getDuration(Configuration.EXECUTOR_COMPARTMENT_END);
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
            execute();
            try {
                Thread.sleep(period.getMillis());
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                continue;
            }
        }

        for (CompartmentExecutor executor : executorsById.values()) {
            if (executor.isAlive()) {
                logger.debug("Killing '{}'...", executor.getName());
                executor.stop();
            }
        }

        logger.info("Executor stopped!");
    }

    /**
     * Execute {@link cz.cesnet.shongo.controller.api.Reservation}s which should be executed.
     */
    private void execute()
    {
        logger.info("Checking compartments for execution...");
        Interval interval = new Interval(
                DateTime.now(), DateTime.now().plus(lookupAhead));
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        CompartmentManager compartmentManager = new CompartmentManager(entityManager);
        List<Compartment> compartments = compartmentManager.listCompartmentsForExecution(interval);
        for (Compartment compartment : compartments) {
            Long compartmentId = compartment.getId();
            if (executorsById.containsKey(compartmentId)) {
                continue;
            }
            CompartmentExecutor executor = new CompartmentExecutor(this, controllerAgent, compartment.getId(),
                    entityManagerFactory);
            executor.start();
            executorsById.put(compartmentId, executor);
        }
        entityManager.close();
    }
}
