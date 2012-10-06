package cz.cesnet.shongo.jade.ontology;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;

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
