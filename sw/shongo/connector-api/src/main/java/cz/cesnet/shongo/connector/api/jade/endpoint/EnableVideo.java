package cz.cesnet.shongo.connector.api.jade.endpoint;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * Command for an endpoint to enable video.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class EnableVideo extends ConnectorCommand
{
    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Enabling video");
        getEndpoint(connector).enableVideo();
        return null;
    }
}
