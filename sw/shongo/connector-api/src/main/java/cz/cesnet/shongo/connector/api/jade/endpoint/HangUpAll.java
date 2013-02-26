package cz.cesnet.shongo.connector.api.jade.endpoint;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorAgentAction;

/**
 * Command to hang up all active calls.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class HangUpAll extends ConnectorAgentAction
{

    @Override
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Hanging up all calls");
        getEndpoint(connector).hangUpAll();
        return null;
    }
}
