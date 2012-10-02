package cz.cesnet.shongo.controller.compartment;

import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 * Thread for executing a single {@link cz.cesnet.shongo.controller.compartment.Compartment}.
 */
public class CompartmentExecutor extends Thread
{
    private static Logger logger = LoggerFactory.getLogger(CompartmentExecutor.class);

    private static final Period START_BEFORE_PERIOD = Period.seconds(30);
    private static final Duration START_WAITING_PERIOD = Duration.standardSeconds(10);
    private static final Period END_BEFORE_PERIOD = Period.seconds(30);
    private static final Duration END_WAITING_PERIOD = Duration.standardSeconds(30);

    /**
     * @see cz.cesnet.shongo.controller.ControllerAgent
     */
    private ControllerAgent controllerAgent;

    /**
     * {@link Compartment} to be executed.
     */
    private Long compartmentId;

    /**
     * Interval for which the {@link #compartmentId} should be started.
     */
    private Interval interval;

    /**
     * {@link javax.persistence.EntityManagerFactory} used for loading {@link Compartment}s for execution.
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * Constructor.
     *
     * @param controllerAgent
     * @param compartmentId
     * @param interval
     */
    public CompartmentExecutor(ControllerAgent controllerAgent, Long compartmentId, Interval interval,
            EntityManagerFactory entityManagerFactory)
    {
        this.controllerAgent = controllerAgent;
        this.compartmentId = compartmentId;
        this.interval = interval;
        this.entityManagerFactory = entityManagerFactory;
        setName(String.format("Executor-%d", compartmentId));
    }

    /**
     * @return logger
     */
    public Logger getLogger()
    {
        return logger;
    }

    /**
     * @return {@link #controllerAgent}
     */
    public ControllerAgent getControllerAgent()
    {
        return controllerAgent;
    }

    @Override
    public void run()
    {
        // Wait for start
        while (DateTime.now().plus(START_BEFORE_PERIOD).isBefore(interval.getStart())) {
            try {
                logger.debug("Waiting for compartment '{}' to start...",
                        compartmentId);
                Thread.sleep(START_WAITING_PERIOD.getMillis());
            }
            catch (InterruptedException exception) {
            }
        }

        logger.info("Starting compartment '{}'...", compartmentId);
        {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            CompartmentManager compartmentManager = new CompartmentManager(entityManager);
            Compartment compartment = null;
            try {
                compartment = compartmentManager.get(compartmentId);
            }
            catch (EntityNotFoundException exception) {
                throw new IllegalStateException("Compartment was not found!");
            }

            entityManager.getTransaction().begin();

            // Create virtual rooms
            for (VirtualRoom virtualRoom : compartment.getVirtualRooms()) {
                virtualRoom.create(this);
            }

            // Assign aliases to endpoints
            for (Endpoint endpoint : compartment.getEndpoints()) {
                endpoint.assignAliases(this);
            }

            // Connect endpoints
            for (Connection connection : compartment.getConnections()) {
                connection.establish(this);
            }

            entityManager.getTransaction().commit();
            entityManager.close();
        }

        // Wait for end
        while (DateTime.now().plus(END_BEFORE_PERIOD).isBefore(interval.getEnd())) {
            try {
                logger.debug("Waiting for for compartment '{}' to end...",
                        compartmentId);
                Thread.sleep(END_WAITING_PERIOD.getMillis());
            }
            catch (InterruptedException exception) {
            }
        }

        logger.info("Stopping compartment '{}'...", compartmentId);
        {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            CompartmentManager compartmentManager = new CompartmentManager(entityManager);
            Compartment compartment = null;
            try {
                compartment = compartmentManager.get(compartmentId);
            }
            catch (EntityNotFoundException exception) {
                throw new IllegalStateException("Compartment was not found!");
            }

            entityManager.getTransaction().begin();

            // Disconnect endpoints
            for (Connection connection : compartment.getConnections()) {
                connection.close(this);
            }
            // Stop virtual rooms
            for (VirtualRoom virtualRoom : compartment.getVirtualRooms()) {
                virtualRoom.delete(this);
            }

            entityManager.getTransaction().commit();
            entityManager.close();
        }
    }
}
