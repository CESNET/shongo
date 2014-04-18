package cz.cesnet.shongo.connector.api.jade.endpoint;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * Command to unmute the device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Unmute extends ConnectorCommand
{
    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Unmuting the device");
        getEndpoint(connector).unmute();
        return null;
    }
}
