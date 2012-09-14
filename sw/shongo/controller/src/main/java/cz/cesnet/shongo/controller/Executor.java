package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.allocationaold.AllocatedCompartment;
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
import java.util.Map;

/**
 * Component of a domain controller which executes actions according to allocation plan which was created
 * by the {@link Scheduler}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Executor extends Component implements Component.WithThread, Component.EntityManagerFactoryAware, Runnable
{
    private static Logger logger = LoggerFactory.getLogger(Executor.class);

    /**
     * {@link EntityManagerFactory} used for loading {@link AllocatedCompartment}s for execution.
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * Period in which the executor works.
     */
    private Duration period = Duration.parse("PT15S");

    /**
     * Set of {@link AllocatedCompartment} identifiers which have been already executed.
     */
    private Map<Long, AllocatedCompartmentExecutor> executorsById = new HashMap<Long, AllocatedCompartmentExecutor>();

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

        for (AllocatedCompartmentExecutor executor : executorsById.values()) {
            if (executor.isAlive()) {
                logger.debug("Killing '{}'...", executor.getName());
                executor.stop();
            }
        }

        logger.info("Executor stopped!");
    }

    /**
     * Execute {@link AllocatedCompartment}s which should be executed.
     */
    private void execute()
    {
        logger.info("Checking allocated compartments for execution...");
        Interval interval = new Interval(
                DateTime.now().minus(Period.minutes(1)), DateTime.now().plus(Period.minutes(2)));
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationManager allocatedCompartmentManager = new ReservationManager(entityManager);
        throw new IllegalStateException("TODO: implement");
        /*List<AllocatedCompartment> allocatedCompartments = allocatedCompartmentManager.listByInterval(interval);
        for (AllocatedCompartment allocatedCompartment : allocatedCompartments) {
            Long allocatedCompartmentId = allocatedCompartment.getId();
            if (executorsById.containsKey(allocatedCompartmentId)) {
                continue;
            }
            AllocatedCompartmentExecutor executor = new AllocatedCompartmentExecutor(allocatedCompartment);
            executor.start();
            executorsById.put(allocatedCompartmentId, executor);
        }*/
    }

    /**
     * Thread for executing a single {@link AllocatedCompartment}.
     */
    private static class AllocatedCompartmentExecutor extends Thread
    {
        private static final Period START_BEFORE_PERIOD = Period.seconds(30);
        private static final Duration START_WAITING_PERIOD = Duration.standardSeconds(10);
        private static final Period END_BEFORE_PERIOD = Period.seconds(30);
        private static final Duration END_WAITING_PERIOD = Duration.standardSeconds(30);

        private AllocatedCompartment allocatedCompartment;
        private Interval interval;

        public AllocatedCompartmentExecutor(AllocatedCompartment allocatedCompartment)
        {
            this.allocatedCompartment = allocatedCompartment;
            this.interval = allocatedCompartment.getSlot();
            setName(String.format("Executor-%d", allocatedCompartment.getId()));
        }

        @Override
        public void run()
        {
            // Wait for start
            while (DateTime.now().plus(START_BEFORE_PERIOD).isBefore(interval.getStart())) {
                try {
                    logger.debug("Waiting for video conference for allocated compartment '{}' to start...",
                            allocatedCompartment.getId());
                    Thread.sleep(START_WAITING_PERIOD.getMillis());
                }
                catch (InterruptedException exception) {
                }
            }

            logger.info("Starting video conference for allocated compartment '{}'...", allocatedCompartment.getId());

            throw new IllegalStateException("TODO:");

            // Create virtual rooms
            /*for (AllocatedItem allocatedItem : allocatedCompartment.getChildReservations()) {
                boolean virtualRoom = false;
                if (allocatedItem instanceof AllocatedVirtualRoom) {
                    virtualRoom = true;
                    AllocatedVirtualRoom allocatedVirtualRoom = (AllocatedVirtualRoom) allocatedItem;
                    DeviceResource deviceResource = allocatedVirtualRoom.getDeviceResource();
                    StringBuilder message = new StringBuilder();
                    message.append(String.format("Starting virtual room on device '%d' for %d ports.",
                            deviceResource.getId(), allocatedVirtualRoom.getPortCount()));
                    if (deviceResource.hasIpAddress()) {
                        message.append(String.format(" Device has address '%s'.",
                                deviceResource.getAddress().getValue()));
                    }
                    logger.debug(message.toString());
                }
                if (allocatedItem instanceof AllocatedDevice) {
                    AllocatedDevice allocatedDevice = (AllocatedDevice) allocatedItem;
                    DeviceResource deviceResource = allocatedDevice.getDeviceResource();
                    List<Alias> aliases = allocatedDevice.getAliases();
                    for (Alias alias : aliases) {
                        StringBuilder message = new StringBuilder();
                        message.append(String.format("%s '%d' has allocated alias '%s'.",
                                (virtualRoom ? "Virtual room" : "Device"), deviceResource.getId(), alias.getValue()));
                        logger.debug(message.toString());
                    }
                }
            }
            // Connect devices
            for (Connection connection : allocatedCompartment.getConnections()) {
                String endpointFromString = "unknown device";
                AllocatedItem endpointFrom = connection.getEndpointFrom();
                if (endpointFrom instanceof AllocatedDevice) {
                    AllocatedDevice allocatedDevice = (AllocatedDevice) endpointFrom;
                    endpointFromString = String.format("device '%d'", allocatedDevice.getDeviceResource().getId());
                }
                else if (endpointFrom instanceof AllocatedExternalEndpoint) {
                    endpointFromString = "external endpoint";
                }
                if (connection instanceof ConnectionByAddress) {
                    ConnectionByAddress connectionByAddress = (ConnectionByAddress) connection;
                    StringBuilder message = new StringBuilder();
                    message.append(String.format("Dialing from %s to address '%s' in technology '%s'.",
                            endpointFromString, connectionByAddress.getAddress(),
                            connectionByAddress.getTechnology().getName()));
                    logger.debug(message.toString());
                }
                else if (connection instanceof ConnectionByAlias) {
                    ConnectionByAlias connectionByAlias = (ConnectionByAlias) connection;
                    StringBuilder message = new StringBuilder();
                    message.append(String.format("Dialing from %s to alias '%s' in technology '%s'.",
                            endpointFromString, connectionByAlias.getAlias().getValue(),
                            connectionByAlias.getAlias().getTechnology().getName()));
                    logger.debug(message.toString());
                }
            }

            // Wait for end
            while (DateTime.now().plus(END_BEFORE_PERIOD).isBefore(interval.getEnd())) {
                try {
                    logger.debug("Waiting for video conference for allocated compartment '{}' to end...",
                            allocatedCompartment.getId());
                    Thread.sleep(END_WAITING_PERIOD.getMillis());
                }
                catch (InterruptedException exception) {
                }
            }

            logger.info("Stopping video conference for allocated compartment '{}'...", allocatedCompartment.getId());

            // Disconnect devices
            for (Connection connection : allocatedCompartment.getConnections()) {
                String endpointFromString = "unknown device";
                AllocatedItem endpointFrom = connection.getEndpointFrom();
                if (endpointFrom instanceof AllocatedDevice) {
                    AllocatedDevice allocatedDevice = (AllocatedDevice) endpointFrom;
                    endpointFromString = String.format("device '%d'", allocatedDevice.getDeviceResource().getId());
                }
                else if (endpointFrom instanceof AllocatedExternalEndpoint) {
                    endpointFromString = "external endpoint";
                }
                StringBuilder message = new StringBuilder();
                message.append(String.format("Disconnecting %s.", endpointFromString));
                logger.debug(message.toString());
            }
            // Stop virtual rooms
            for (AllocatedItem allocatedItem : allocatedCompartment.getChildReservations()) {
                if (allocatedItem instanceof AllocatedVirtualRoom) {
                    AllocatedVirtualRoom allocatedVirtualRoom = (AllocatedVirtualRoom) allocatedItem;
                    DeviceResource deviceResource = allocatedVirtualRoom.getDeviceResource();
                    StringBuilder message = new StringBuilder();
                    message.append(String.format("Stopping virtual room on device '%d'.",
                            deviceResource.getId()));
                    logger.debug(message.toString());
                }
            }*/
        }
    }
}
