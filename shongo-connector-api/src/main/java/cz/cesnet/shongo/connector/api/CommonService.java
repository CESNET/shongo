package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.api.ConnectorStatus;
import cz.cesnet.shongo.api.jade.CommandException;

import java.util.List;

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
     * @param configuration
     * @throws cz.cesnet.shongo.api.jade.CommandException
     */
    public void connect(ConnectorConfiguration configuration) throws CommandException;

    /**
     * Disconnects from the device.
     *
     * @throws CommandException
     */
    public void disconnect() throws CommandException;

    /**
     * @return {@link cz.cesnet.shongo.api.ConnectorStatus}
     */
    public ConnectorStatus getStatus();

    /**
     * Lists names of all implemented methods supported by the implementing connector.
     *
     * @return collection of public methods implemented from an interface, not throwing CommandUnsupportedException
     */
    public List<String> getSupportedMethods();

}
