package cz.cesnet.shongo.connector.api.jade.multipoint.users;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class DialParticipant extends ConnectorCommand
{
    private String roomId;

    private Alias alias = null;

    public DialParticipant()
    {
    }

    public DialParticipant(String roomId, Alias alias)
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
        return getMultipoint(connector).dialParticipant(roomId, alias);
    }

    public String toString()
    {

        return String.format(DialParticipant.class.getSimpleName() + " (roomId: %s, alias: %s)",
                roomId, alias.toString());
    }
}
