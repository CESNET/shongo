package cz.cesnet.shongo.controller.compartment;

import cz.cesnet.shongo.controller.ControllerAgent;
import cz.cesnet.shongo.controller.Executor;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import org.joda.time.DateTime;
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

    /**
     * @see Executor
     */
    private Executor executor;

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
    private DateTime start;

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
    public CompartmentExecutor(Executor executor, ControllerAgent controllerAgent, Long compartmentId,
            EntityManagerFactory entityManagerFactory)
    {
        this.executor = executor;
        this.controllerAgent = controllerAgent;
        this.compartmentId = compartmentId;
        this.entityManagerFactory = entityManagerFactory;
        setName(String.format("Executor-%d", compartmentId));

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        Compartment compartment = getCompartment(entityManager);
        entityManager.close();
        this.start = compartment.getSlot().getStart();
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

    /**
     * @return {@link #compartmentId}
     */
    public Long getCompartmentId()
    {
        return compartmentId;
    }

    /**
     * @param entityManager
     * @return loaded compartment from the given {@code entityManager}
     */
    public Compartment getCompartment(EntityManager entityManager)
    {
        CompartmentManager compartmentManager = new CompartmentManager(entityManager);
        Compartment compartment = null;
        try {
            compartment = compartmentManager.get(compartmentId);
        }
        catch (EntityNotFoundException exception) {
            throw new IllegalStateException("Compartment was not found!");
        }
        return compartment;
    }

    @Override
    public void run()
    {
        // Wait for start
        start = start.plus(executor.getCompartmentStart());
        while (DateTime.now().isBefore(start)) {
            try {
                logger.debug("Waiting for compartment '{}' to start...",
                        compartmentId);
                Thread.sleep(executor.getCompartmentWaitingStart().getMillis());
            }
            catch (InterruptedException exception) {
            }
        }

        // Start
        {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            Compartment compartment = getCompartment(entityManager);

            boolean startedNow = false;
            if (compartment.getState() == Compartment.State.NOT_STARTED) {
                logger.info("Starting compartment '{}'...", compartmentId);
                entityManager.getTransaction().begin();
                compartment.setState(Compartment.State.STARTED);
                entityManager.getTransaction().commit();
                startedNow = true;
            }
            else {
                logger.info("Resuming compartment '{}'...", compartmentId);
            }

            // Create virtual rooms
            boolean virtualRoomCreated = false;
            for (VirtualRoom virtualRoom : compartment.getVirtualRooms()) {
                if (virtualRoom.getState() == VirtualRoom.State.NOT_CREATED) {
                    entityManager.getTransaction().begin();
                    virtualRoom.create(this);
                    entityManager.getTransaction().commit();
                    virtualRoomCreated = true;
                }
            }
            if (virtualRoomCreated) {
                logger.info("Waiting for virtual rooms to be created...");
                try {
                    Thread.sleep(5000);
                }
                catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
            }


            // TODO: persist already assigned aliases
            if (startedNow) {
                // Assign aliases to endpoints
                for (Endpoint endpoint : compartment.getEndpoints()) {
                    endpoint.assignAliases(this);
                }
            }

            // Connect endpoints
            for (Connection connection : compartment.getConnections()) {
                if (connection.getState() == Connection.State.NOT_ESTABLISHED) {
                    entityManager.getTransaction().begin();
                    connection.establish(this);
                    entityManager.getTransaction().commit();
                }
            }

            entityManager.close();
        }

        // Wait for end
        DateTime end;
        while (true) {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            Compartment compartment = getCompartment(entityManager);
            entityManager.close();
            end = compartment.getSlot().getEnd();
            end = end.plus(executor.getCompartmentEnd());
            if (DateTime.now().isAfter(end)) {
                break;
            }
            try {
                logger.debug("Waiting for for compartment '{}' to end...",
                        compartmentId);
                Thread.sleep(executor.getCompartmentWaitingEnd().getMillis());
            }
            catch (InterruptedException exception) {
            }
        }

        // End
        {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            Compartment compartment = getCompartment(entityManager);
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
