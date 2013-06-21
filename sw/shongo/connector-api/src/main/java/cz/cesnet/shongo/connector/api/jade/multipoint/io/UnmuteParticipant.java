package cz.cesnet.shongo.connector.api.jade.multipoint.io;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class UnmuteParticipant extends ConnectorCommand
{
    private String roomId;
    private String roomUserId;

    public UnmuteParticipant()
    {
    }

    public UnmuteParticipant(String roomId, String roomUserId)
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
        logger.debug("Unmuting participant {} in room {}", roomUserId, roomId);
        getMultipoint(connector).unmuteParticipant(roomId, roomUserId);
        return null;
    }

    @Override
    public String toString()
    {
        return String.format(UnmuteParticipant.class.getSimpleName() + " (roomId: %s, roomUserId: %s)",
                roomId, roomUserId);
    }
}
