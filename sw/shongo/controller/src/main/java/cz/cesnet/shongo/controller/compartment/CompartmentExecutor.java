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
     */
    public CompartmentExecutor(ControllerAgent controllerAgent, Long compartmentId,
            EntityManagerFactory entityManagerFactory)
    {
        this.controllerAgent = controllerAgent;
        this.compartmentId = compartmentId;
        this.entityManagerFactory = entityManagerFactory;
        setName(String.format("Executor-%d", compartmentId));

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        CompartmentManager compartmentManager = new CompartmentManager(entityManager);
        Compartment compartment = null;
        try {
            compartment = compartmentManager.get(compartmentId);
        }
        catch (EntityNotFoundException exception) {
            throw new IllegalStateException("Compartment was not found!");
        }
        entityManager.close();
        this.interval = compartment.getSlot();
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

        // Start
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
            if (compartment.getState() == Compartment.State.NOT_STARTED) {
                logger.info("Starting compartment '{}'...", compartmentId);

                // Create virtual rooms
                for (VirtualRoom virtualRoom : compartment.getVirtualRooms()) {
                    if (virtualRoom.getState() == VirtualRoom.State.NOT_CREATED) {
                        entityManager.getTransaction().begin();
                        virtualRoom.create(this);
                        entityManager.getTransaction().commit();
                    }
                }
                logger.info("Waiting for virtual rooms to be created...", compartmentId);
                try {
                    Thread.sleep(5000);
                }
                catch (InterruptedException exception) {
                    exception.printStackTrace();
                }

                // Assign aliases to endpoints
                for (Endpoint endpoint : compartment.getEndpoints()) {
                    endpoint.assignAliases(this);
                }

                // Connect endpoints
                for (Connection connection : compartment.getConnections()) {
                    if (connection.getState() == Connection.State.NOT_ESTABLISHED) {
                        entityManager.getTransaction().begin();
                        connection.establish(this);
                        entityManager.getTransaction().commit();
                    }
                }

                entityManager.getTransaction().begin();
                compartment.setState(Compartment.State.STARTED);
                entityManager.getTransaction().commit();
            }
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

        // End
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
            if (compartment.getState() == Compartment.State.STARTED) {
                logger.info("Stopping compartment '{}'...", compartmentId);

                // Disconnect endpoints
                for (Connection connection : compartment.getConnections()) {
                    if (connection.getState() == Connection.State.ESTABLISHED) {
                        entityManager.getTransaction().begin();
                        connection.close(this);
                        entityManager.getTransaction().commit();
                    }
                }
                // Stop virtual rooms
                for (VirtualRoom virtualRoom : compartment.getVirtualRooms()) {
                    if (virtualRoom.getState() == VirtualRoom.State.CREATED) {
                        entityManager.getTransaction().begin();
                        virtualRoom.delete(this);
                        entityManager.getTransaction().commit();
                    }
                }

                entityManager.getTransaction().begin();
                compartment.setState(Compartment.State.FINISHED);
                entityManager.getTransaction().commit();
            }
            entityManager.close();
        }
    }
}
