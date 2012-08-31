package cz.cesnet.shongo.connector.api;

/**
 * Common connector API.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface CommonService
{
    /**
     * Connects to the device.
     *
     * @param address     address of the device to connect to
     * @param port        port on the device to connect to
     * @param username    username for authentication on the device
     * @param password    password for authentication on the device
     * @throws CommandException
     */
    void connect(String address, int port, String username, final String password) throws CommandException;

    /**
     * Disconnects from the device.
     *
     * @throws CommandException
     */
    void disconnect() throws CommandException;

    /**
     * Get information about connector.
     *
     * @return information about the connector
     */
    ConnectorInfo getConnectorInfo();

}
