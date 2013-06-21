package cz.cesnet.shongo.connector.api.jade.multipoint.rooms;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ListRooms extends ConnectorCommand
{
    public ListRooms()
    {
    }

    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Getting list of all rooms");
        return getMultipoint(connector).getRoomList();
    }
}
