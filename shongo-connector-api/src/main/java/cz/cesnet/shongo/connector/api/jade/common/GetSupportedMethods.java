package cz.cesnet.shongo.connector.api.jade.common;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * Command to get methods supported by the connector.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class GetSupportedMethods extends ConnectorCommand
{
    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        return connector.getSupportedMethods();
    }
}
