package cz.cesnet.shongo.jade.ontology.actions.multipoint.rooms;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.jade.ontology.ConnectorAgentAction;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class GetRoomSummary extends ConnectorAgentAction
{
    private String roomId;

    public GetRoomSummary()
    {
    }

    public GetRoomSummary(String roomId)
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
        logger.info("Getting room summary on {}", roomId);
        return getMultipoint(connector).getRoomSummary(roomId);
    }

    @Override
    public String toString()
    {
        return String.format("GetRoomSummary agent action (roomId: %s)", roomId);
    }
}
