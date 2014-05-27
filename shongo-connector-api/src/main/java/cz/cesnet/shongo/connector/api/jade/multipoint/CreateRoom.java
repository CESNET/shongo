package cz.cesnet.shongo.connector.api.jade.multipoint;

import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 * @see {@link cz.cesnet.shongo.connector.api.RoomService#createRoom}
 */
public class CreateRoom extends ConnectorCommand<String>
{
    private Room room;

    public CreateRoom()
    {
    }

    public CreateRoom(Room room)
    {
        this.room = room;
    }

    public Room getRoom()
    {
        return room;
    }

    public void setRoom(Room room)
    {
        this.room = room;
    }

    @Override
    public String execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Creating room {}", room);
        return getMultipoint(connector).createRoom(room);
    }

    public String toString()
    {
        return String.format(CreateRoom.class.getSimpleName() + " (room: %s)", room.toString());
    }
}
