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
    private String roomParticipantId;

    public UnmuteParticipant()
    {
    }

    public UnmuteParticipant(String roomId, String roomParticipantId)
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
        logger.debug("Unmuting participant {} in room {}", roomParticipantId, roomId);
        getMultipoint(connector).unmuteParticipant(roomId, roomParticipantId);
        return null;
    }

    @Override
    public String toString()
    {
        return String.format(UnmuteParticipant.class.getSimpleName() + " (roomId: %s, roomParticipantId: %s)",
                roomId, roomParticipantId);
    }
}
