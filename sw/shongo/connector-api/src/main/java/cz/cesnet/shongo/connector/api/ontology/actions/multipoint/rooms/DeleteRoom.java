package cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.ontology.ConnectorAgentAction;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class DeleteRoom extends ConnectorAgentAction
{
    private String roomId;

    public DeleteRoom()
    {
    }

    public DeleteRoom(String roomId)
    {
        this.roomId = roomId;
    }

    @Override
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.info("Deleting room {}", roomId);
        getMultipoint(connector).deleteRoom(roomId);
        return null;
    }

    public String toString()
    {
        return String.format("DeleteRoom agent action (roomId: %s)", roomId);
    }

    public String getRoomId()
    {
        return roomId;
    }

    public void setRoomId(String roomId)
    {
        this.roomId = roomId;
    }
}
