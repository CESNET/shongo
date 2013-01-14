package cz.cesnet.shongo.connector.api.ontology.actions.multipoint.rooms;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.ontology.ConnectorAgentAction;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class ModifyRoom extends ConnectorAgentAction
{
    private Room room;

    public ModifyRoom()
    {
    }

    public ModifyRoom(Room room)
    {
        this.room = room;
    }

    public Room getRoom()
    {
        return room;
    }

    public void setRoom(Room room)
    {
        this.room = room;
    }

    @Override
    public Object exec(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.info("Modifying room {}", room.getId());
        return getMultipoint(connector).modifyRoom(room);
    }

    public String toString()
    {
        return String.format("ModifyRoom agent action (room: %s)", room.getId());
    }
}