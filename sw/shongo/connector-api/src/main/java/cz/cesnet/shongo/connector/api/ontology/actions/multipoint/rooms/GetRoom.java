package cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.ontology.ConnectorAgentAction;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class GetRoom extends ConnectorAgentAction
{
    private String roomId;

    public GetRoom()
    {
    }

    public GetRoom(String roomId)
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
        logger.debug("Getting room {}", roomId);
        return getMultipoint(connector).getRoom(roomId);
    }

    @Override
    public String toString()
    {
        return String.format(GetRoom.class.getSimpleName() + " (roomId: %s)", roomId);
    }
}
