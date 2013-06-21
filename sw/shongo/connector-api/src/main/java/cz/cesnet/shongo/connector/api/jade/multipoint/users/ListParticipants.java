package cz.cesnet.shongo.connector.api.jade.multipoint.users;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ListParticipants extends ConnectorCommand
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
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Getting list of all participants in room {}", roomId);
        return getMultipoint(connector).listParticipants(roomId);
    }

    public String toString()
    {
        return String.format(ListParticipants.class.getSimpleName() + " (roomId: %s)", roomId);
    }
}
