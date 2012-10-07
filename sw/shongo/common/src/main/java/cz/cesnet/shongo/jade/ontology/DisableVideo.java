package cz.cesnet.shongo.jade.ontology;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;

/**
 * Command for an endpoint to disable video.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class DisableVideo extends ConnectorAgentAction
{
    @Override
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.info("Disabling video");
        getEndpoint(connector).disableVideo();
        return null;
    }

    public String toString()
    {
        return "DisableVideo agent action";
    }
}
