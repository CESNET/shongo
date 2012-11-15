package cz.cesnet.shongo.connector.api.ontology.actions.multipoint.users;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.ontology.ConnectorAgentAction;

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

    @Override
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        getMultipoint(connector).disconnectParticipant(roomId, roomUserId);
        return null;
    }

    public String toString()
    {
        return String.format("DisconnectParticipant agent action (roomId: %s, roomUserId: %s)", roomId, roomUserId);
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
}
