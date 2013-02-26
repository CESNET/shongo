package cz.cesnet.shongo.connector.api.jade.common;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorAgentAction;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class GetDeviceLoadInfo extends ConnectorAgentAction
{
    @Override
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        return connector.getDeviceLoadInfo();
    }
}
