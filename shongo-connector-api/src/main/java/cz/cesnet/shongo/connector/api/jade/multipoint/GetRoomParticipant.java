package cz.cesnet.shongo.connector.api.jade.multipoint;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * Gets user information and settings in a room.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 * @see {@link cz.cesnet.shongo.connector.api.RoomService#getRoomParticipant}
 */
public class GetRoomParticipant extends ConnectorCommand
{
    private String roomId;
    private String roomParticipantId;

    public GetRoomParticipant()
    {
    }

    public GetRoomParticipant(String roomId, String roomParticipantId)
    {
        this.roomId = roomId;
        this.roomParticipantId = roomParticipantId;
    }

    public String getRoomId()
    {
        return roomId;
    }

    public void setRoomId(String roomId)
    {
        this.roomId = roomId;
    }

    public String getRoomParticipantId()
    {
        return roomParticipantId;
    }

    public void setRoomParticipantId(String roomParticipantId)
    {
        this.roomParticipantId = roomParticipantId;
    }

    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Getting participant info for {} in room {}", roomParticipantId, roomId);
        return getMultipoint(connector).getRoomParticipant(roomId, roomParticipantId);
    }

    @Override
    public String toString()
    {
        return String.format(GetRoomParticipant.class.getSimpleName() + " (roomId: %s, roomParticipantId: %s)",
                roomId, roomParticipantId);
    }
}
