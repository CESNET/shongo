package cz.cesnet.shongo.jade.ontology.actions.endpoint;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.jade.ontology.ConnectorAgentAction;

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
        getEndpoint(connector).mute();
        return null;
    }

    public String toString()
    {
        return "Mute agent action";
    }
}
