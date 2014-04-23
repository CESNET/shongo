package cz.cesnet.shongo.connector.api.jade.multipoint;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 * @see {@link cz.cesnet.shongo.connector.api.RoomService#dialRoomParticipant}
 */
public class DialRoomParticipant extends ConnectorCommand
{
    private String roomId;

    private Alias alias = null;

    public DialRoomParticipant()
    {
    }

    public DialRoomParticipant(String roomId, Alias alias)
    {
        this.roomId = roomId;
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

    public Alias getAlias()
    {
        return alias;
    }

    public void setAlias(Alias alias)
    {
        this.alias = alias;
    }

    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        if (alias == null) {
            throw new IllegalStateException("Alias should be set.");
        }
        logger.debug("Dialing user at alias {} into room {}", alias, roomId);
        return getMultipoint(connector).dialRoomParticipant(roomId, alias);
    }

    public String toString()
    {

        return String.format(DialRoomParticipant.class.getSimpleName() + " (roomId: %s, alias: %s)",
                roomId, alias.toString());
    }
}
