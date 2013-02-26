package cz.cesnet.shongo.connector.api.jade.endpoint;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorAgentAction;

/**
 * Command to reboot a device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class RebootDevice extends ConnectorAgentAction
{

    @Override
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Rebooting the device");
        getEndpoint(connector).rebootDevice();
        return null;
    }
}
