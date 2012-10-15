package cz.cesnet.shongo.jade.ontology.actions.endpoint;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.jade.ontology.ConnectorAgentAction;

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
        logger.info("Hanging up all calls");
        getEndpoint(connector).hangUpAll();
        return null;
    }

    public String toString()
    {
        return "HangUpAll agent action";
    }
}