package cz.cesnet.shongo.connector.api.ontology.actions.multipoint.users;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.ontology.ConnectorAgentAction;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ListParticipants extends ConnectorAgentAction
{
    private String roomId;

    public ListParticipants()
    {
    }

    public ListParticipants(String roomId)
    {
        this.roomId = roomId;
    }

    public String getRoomId()
    {
        return roomId;
    }

    public void setRoomId(String roomId)
    {
        this.roomId = roomId;
    }

    @Override
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        return getMultipoint(connector).listParticipants(roomId);
    }

    public String toString()
    {
        return String.format("ListParticipants agent action (roomId: %s)", roomId);
    }
}
