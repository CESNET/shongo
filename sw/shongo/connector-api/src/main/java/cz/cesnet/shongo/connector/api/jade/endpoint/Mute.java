package cz.cesnet.shongo.connector.api.jade.endpoint;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorAgentAction;

/**
 * Command to mute the device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Mute extends ConnectorAgentAction
{
    @Override
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Muting the device");
        getEndpoint(connector).mute();
        return null;
    }
}
