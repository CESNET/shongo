package cz.cesnet.shongo.connector.api.jade.endpoint;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * Command for an endpoint to start a presentation.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class StartPresentation extends ConnectorCommand
{
    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        getEndpoint(connector).startPresentation();
        return null;
    }
}
