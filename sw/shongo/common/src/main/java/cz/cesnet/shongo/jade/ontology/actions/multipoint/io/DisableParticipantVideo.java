package cz.cesnet.shongo.jade.ontology.actions.multipoint.io;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.jade.ontology.ConnectorAgentAction;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class DisableParticipantVideo extends ConnectorAgentAction
{
    private String roomId;
    private String roomUserId;

    public DisableParticipantVideo()
    {
    }

    public DisableParticipantVideo(String roomId, String roomUserId)
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
        getMultipoint(connector).disableParticipantVideo(roomId, roomUserId);
        return null;
    }

    @Override
    public String toString()
    {
        return String.format("DisableParticipantVideo agent action (roomId: %s, roomUserId: %s)", roomId, roomUserId);
    }
}
