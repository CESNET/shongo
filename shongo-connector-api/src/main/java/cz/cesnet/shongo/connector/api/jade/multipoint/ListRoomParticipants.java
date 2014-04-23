package cz.cesnet.shongo.connector.api.jade.multipoint;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 * @see {@link cz.cesnet.shongo.connector.api.RoomService#listRoomParticipants}
 */
public class ListRoomParticipants extends ConnectorCommand
{
    private String roomId;

    public ListRoomParticipants()
    {
    }

    public ListRoomParticipants(String roomId)
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
        logger.debug("Getting list of all participants in room {}", roomId);
        return getMultipoint(connector).listRoomParticipants(roomId);
    }

    public String toString()
    {
        return String.format(ListRoomParticipants.class.getSimpleName() + " (roomId: %s)", roomId);
    }
}
