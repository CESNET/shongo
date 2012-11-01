package cz.cesnet.shongo.jade.ontology.actions.multipoint.monitoring;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.jade.ontology.ConnectorAgentAction;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ListRooms extends ConnectorAgentAction
{
    public ListRooms()
    {
    }

    @Override
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.info("GetRoomList agent action");
        return getMultipoint(connector).getRoomList();
    }

    @Override
    public String toString()
    {
        return "ListRooms agent action";
    }
}
