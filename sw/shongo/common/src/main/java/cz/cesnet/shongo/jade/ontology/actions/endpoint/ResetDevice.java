package cz.cesnet.shongo.jade.ontology.actions.endpoint;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.jade.ontology.ConnectorAgentAction;

/**
 * Command to reset a device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ResetDevice extends ConnectorAgentAction
{

    @Override
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.info("Resetting the device");
        getEndpoint(connector).resetDevice();
        return null;
    }

    public String toString()
    {
        return "ResetDevice agent action";
    }
}
