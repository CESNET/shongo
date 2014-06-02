package cz.cesnet.shongo.connector.api.jade.common;

import cz.cesnet.shongo.api.ConnectorStatus;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link cz.cesnet.shongo.connector.api.CommonService#getStatus}
 */
public class GetStatus extends ConnectorCommand<ConnectorStatus>
{
    @Override
    public ConnectorStatus execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        return connector.getStatus();
    }
}
