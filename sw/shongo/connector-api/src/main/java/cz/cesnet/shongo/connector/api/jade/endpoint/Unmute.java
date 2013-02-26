package cz.cesnet.shongo.connector.api.jade.endpoint;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorAgentAction;

/**
 * Command to unmute the device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class Unmute extends ConnectorAgentAction
{
    @Override
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Unmuting the device");
        getEndpoint(connector).unmute();
        return null;
    }
}
