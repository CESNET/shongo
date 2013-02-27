package cz.cesnet.shongo.connector.api.jade.multipoint.rooms;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
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
