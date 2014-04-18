package cz.cesnet.shongo.connector.api.jade.endpoint;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * Command to mute the device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Mute extends ConnectorCommand
{
    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Muting the device");
        getEndpoint(connector).mute();
        return null;
    }
}
