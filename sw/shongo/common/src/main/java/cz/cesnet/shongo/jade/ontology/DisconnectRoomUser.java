package cz.cesnet.shongo.jade.ontology;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class DisconnectRoomUser extends ConnectorAgentAction
{
    private String roomId;
    private String roomUserId;

    public DisconnectRoomUser()
    {
    }

    public DisconnectRoomUser(String roomId, String roomUserId)
    {
        this.roomId = roomId;
        this.roomUserId = roomUserId;
    }

    @Override
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        getMultipoint(connector).disconnectRoomUser(roomId, roomUserId);
        return null;
    }

    public String toString()
    {
        return String.format("DisconnectRoomUser agent action (roomId: %s, roomUserId: %s)", roomId, roomUserId);
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
