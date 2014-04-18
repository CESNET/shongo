package cz.cesnet.shongo.connector.api.jade.multipoint.rooms;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class DisconnectParticipant extends ConnectorCommand
{
    private String roomId;
    private String roomParticipantId;

    public DisconnectParticipant()
    {
    }

    public DisconnectParticipant(String roomId, String roomParticipantId)
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
        logger.debug("Disconnecting participant {} from the room {}", roomParticipantId, roomId);
        getMultipoint(connector).disconnectRoomParticipant(roomId, roomParticipantId);
        return null;
    }

    public String toString()
    {
        return String.format(DisconnectParticipant.class.getSimpleName() + " (roomId: %s, roomParticipantId: %s)",
                roomId, roomParticipantId);
    }
}
