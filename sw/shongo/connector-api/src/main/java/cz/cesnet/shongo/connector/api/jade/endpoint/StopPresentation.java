package cz.cesnet.shongo.connector.api.jade.endpoint;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorAgentAction;

/**
 * Command for an endpoint to stop a presentation.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class StopPresentation extends ConnectorAgentAction
{
    @Override
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Stopping presentation");
        getEndpoint(connector).stopPresentation();
        return null;
    }
}
