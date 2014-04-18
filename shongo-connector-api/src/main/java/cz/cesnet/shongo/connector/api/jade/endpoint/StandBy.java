package cz.cesnet.shongo.connector.api.jade.endpoint;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * Command to switch an endpoint to the standby mode.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class StandBy extends ConnectorCommand
{
    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Standbying the device");
        getEndpoint(connector).standBy();
        return null;
    }
}
