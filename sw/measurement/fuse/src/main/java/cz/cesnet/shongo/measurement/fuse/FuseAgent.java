package cz.cesnet.shongo.measurement.fuse;

import cz.cesnet.shongo.measurement.common.Agent;

public class FuseAgent extends Agent {

    public static String brokerURL = "tcp://localhost:61616";

    /**
     * Constructor
     *
     * @param id   Agent id
     * @param name Agent name
     */
    public FuseAgent(String id, String name) {
        super(id, name);
    }

    @Override
    public void start() {
        logger.info("Starting FUSE agent...");
    }

    @Override
    public void stop() {
        logger.info("Stopping FUSE agent...");
    }
}
