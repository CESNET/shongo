package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.util.Address;

import java.lang.reflect.Method;
import java.util.Collection;

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
     * @param address     device address to connect to
     * @param username    username for authentication on the device
     * @param password    password for authentication on the device
     * @throws cz.cesnet.shongo.api.CommandException
     */
    void connect(Address address, String username, final String password) throws CommandException;

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

    /**
     * Lists all implemented methods supported by the implementing connector.
     *
     * @return collection of public methods implemented from an interface, not throwing CommandUnsupportedException
     */
    Collection<Method> getSupportedMethods();

}
