package cz.cesnet.shongo.connector.api.jade.multipoint;

import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 * @see {@link cz.cesnet.shongo.connector.api.RoomService#getRoom}
 */
public class GetRoom extends ConnectorCommand<Room>
{
    private String roomId;

    public GetRoom()
    {
    }

    public GetRoom(String roomId)
    {
        this.roomId = roomId;
    }

    public String getRoomId()
    {
        return roomId;
    }

    public void setRoomId(String roomId)
    {
        this.roomId = roomId;
    }

    @Override
    public Room execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Getting room {}", roomId);
        return getMultipoint(connector).getRoom(roomId);
    }

    @Override
    public String toString()
    {
        return String.format(GetRoom.class.getSimpleName() + " (roomId: %s)", roomId);
    }
}
