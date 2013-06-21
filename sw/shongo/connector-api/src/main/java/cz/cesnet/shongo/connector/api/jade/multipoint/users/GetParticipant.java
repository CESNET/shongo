package cz.cesnet.shongo.connector.api.jade.multipoint.users;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * Gets user information and settings in a room.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class GetParticipant extends ConnectorCommand
{
    private String roomId;
    private String roomUserId;

    public GetParticipant()
    {
    }

    public GetParticipant(String roomId, String roomUserId)
    {
        this.roomId = roomId;
        this.roomUserId = roomUserId;
    }

    public String getRoomId()
    {
        return roomId;
    }

    public void setRoomId(String roomId)
    {
        this.roomId = roomId;
    }

    public String getRoomUserId()
    {
        return roomUserId;
    }

    public void setRoomUserId(String roomUserId)
    {
        this.roomUserId = roomUserId;
    }

    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Getting participant info for {} in room {}", roomUserId, roomId);
        return getMultipoint(connector).getParticipant(roomId, roomUserId);
    }

    @Override
    public String toString()
    {
        return String.format(GetParticipant.class.getSimpleName() + " (roomId: %s, roomUserId: %s)",
                roomId, roomUserId);
    }
}
