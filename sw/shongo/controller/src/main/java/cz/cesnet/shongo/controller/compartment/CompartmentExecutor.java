package cz.cesnet.shongo.controller.compartment;

import cz.cesnet.shongo.controller.ControllerAgent;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private Compartment compartment;

    /**
     * Interval for which the {@link #compartment} should be started.
     */
    private Interval interval;

    /**
     * Constructor.
     *
     * @param controllerAgent
     * @param compartment
     * @param interval
     */
    public CompartmentExecutor(ControllerAgent controllerAgent, Compartment compartment, Interval interval)
    {
        this.controllerAgent = controllerAgent;
        this.compartment = compartment;
        this.interval = interval;
        setName(String.format("Executor-%d", compartment.getId()));

        // Load collections
        compartment.getVirtualRooms();
        compartment.getEndpoints();
        compartment.getConnections();
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
                        compartment.getId());
                Thread.sleep(START_WAITING_PERIOD.getMillis());
            }
            catch (InterruptedException exception) {
            }
        }

        logger.info("Starting compartment '{}'...", compartment.getId());

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

        // Wait for end
        while (DateTime.now().plus(END_BEFORE_PERIOD).isBefore(interval.getEnd())) {
            try {
                logger.debug("Waiting for for compartment '{}' to end...",
                        compartment.getId());
                Thread.sleep(END_WAITING_PERIOD.getMillis());
            }
            catch (InterruptedException exception) {
            }
        }

        logger.info("Stopping compartment '{}'...", compartment.getId());

        // Disconnect endpoints
        for (Connection connection : compartment.getConnections()) {
            connection.close(this);
        }
        // Stop virtual rooms
        for (VirtualRoom virtualRoom : compartment.getVirtualRooms()) {
            virtualRoom.delete(this);
        }
    }
}
