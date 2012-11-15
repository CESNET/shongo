package cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.connector.api.ontology.ConnectorAgentAction;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CreateRoom extends ConnectorAgentAction
{
    private Room room;

    public CreateRoom()
    {
    }

    public CreateRoom(Room room)
    {
        this.room = room;
    }

    @Override
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.info("Creating room {}", room.getName());
        return getMultipoint(connector).createRoom(room);
    }

    public String toString()
    {
        return "CreateRoom agent action";
    }

    public Room getRoom()
    {
        return room;
    }

    public void setRoom(Room room)
    {
        this.room = room;
    }
}
