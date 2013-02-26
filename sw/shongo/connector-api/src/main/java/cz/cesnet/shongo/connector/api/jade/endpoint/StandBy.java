package cz.cesnet.shongo.connector.api.jade.endpoint;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorAgentAction;

/**
 * Command to switch an endpoint to the standby mode.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class StandBy extends ConnectorAgentAction
{
    @Override
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Standbying the device");
        getEndpoint(connector).standBy();
        return null;
    }
}
