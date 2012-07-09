package cz.cesnet.shongo.connector.api;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface EndpointService
{
    /**
     * Dials a server.
     * @param server
     */
    void dial(String server);

    /**
     * Resets the device.
     */
    void resetDevice();
}
