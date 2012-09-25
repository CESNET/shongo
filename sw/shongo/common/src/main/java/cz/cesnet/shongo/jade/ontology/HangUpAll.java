package cz.cesnet.shongo.jade.ontology;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class HangUpAll extends ConnectorAgentAction
{

    @Override
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.info(String.format("Hanging up all calls"));
        getEndpoint(connector).hangUpAll();
        return null;
    }
}
