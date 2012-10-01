package cz.cesnet.shongo.jade.ontology;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class DialParticipant extends ConnectorAgentAction
{
    private String roomId;
    private String roomUserId;
    private Alias alias;

    public DialParticipant()
    {
    }

    public DialParticipant(String roomId, String roomUserId, Alias alias)
    {
        this.roomId = roomId;
        this.roomUserId = roomUserId;
        this.alias = alias;
    }

    @Override
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.info(String.format("Dialing in room %s user %s at alias %s", roomId, roomUserId, alias));
        getMultipoint(connector).dialParticipant(roomId, roomUserId, alias);
        return null;
    }

    public String toString()
    {
        return String
                .format("DialParticipant agent action (room: %s, roomUser: %s, alias: %s)", roomId, roomUserId, alias);
    }

    public Alias getAlias()
    {
        return alias;
    }

    public void setAlias(Alias alias)
    {
        this.alias = alias;
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
