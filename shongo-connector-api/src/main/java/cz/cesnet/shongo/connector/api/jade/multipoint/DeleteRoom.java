package cz.cesnet.shongo.connector.api.jade.multipoint;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 * @see {@link cz.cesnet.shongo.connector.api.RoomService#deleteRoom}
 */
public class DeleteRoom extends ConnectorCommand
{
    private String roomId;

    public DeleteRoom()
    {
    }

    public DeleteRoom(String roomId)
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
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Deleting room {}", roomId);
        getMultipoint(connector).deleteRoom(roomId);
        return null;
    }

    public String toString()
    {
        return String.format(DeleteRoom.class.getSimpleName() + " (roomId: %s)", roomId);
    }
}
