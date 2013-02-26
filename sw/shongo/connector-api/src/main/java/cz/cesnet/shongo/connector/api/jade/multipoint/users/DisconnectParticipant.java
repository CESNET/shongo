package cz.cesnet.shongo.connector.api.jade.multipoint.users;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorAgentAction;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class DisconnectParticipant extends ConnectorAgentAction
{
    private String roomId;
    private String roomUserId;

    public DisconnectParticipant()
    {
    }

    public DisconnectParticipant(String roomId, String roomUserId)
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
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Disconnecting participant {} from the room {}", roomUserId, roomId);
        getMultipoint(connector).disconnectParticipant(roomId, roomUserId);
        return null;
    }

    public String toString()
    {
        return String.format(DisconnectParticipant.class.getSimpleName() + " (roomId: %s, roomUserId: %s)",
                roomId, roomUserId);
    }
}
