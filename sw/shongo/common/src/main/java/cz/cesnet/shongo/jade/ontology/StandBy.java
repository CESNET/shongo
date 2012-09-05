package cz.cesnet.shongo.jade.ontology;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import jade.content.Concept;

/**
 * Command to switch an endpoint to the standby mode.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class StandBy extends ConnectorAgentAction
{
    @Override
    public Concept exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        getEndpoint(connector).standBy();
        return null;
    }
}
