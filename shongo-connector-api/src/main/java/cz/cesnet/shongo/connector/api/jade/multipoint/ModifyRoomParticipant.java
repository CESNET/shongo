package cz.cesnet.shongo.connector.api.jade.multipoint;

import cz.cesnet.shongo.api.RoomParticipant;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 * @see {@link cz.cesnet.shongo.connector.api.RoomService#modifyRoomParticipant}
 */
public class ModifyRoomParticipant extends ConnectorCommand
{
    private RoomParticipant roomParticipant;

    public ModifyRoomParticipant()
    {
    }

    public ModifyRoomParticipant(RoomParticipant roomParticipant)
    {
        this.roomParticipant = roomParticipant;
    }

    public RoomParticipant getRoomParticipant()
    {
        return roomParticipant;
    }

    public void setRoomParticipant(RoomParticipant roomParticipant)
    {
        this.roomParticipant = roomParticipant;
    }

    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Modifying participant {} in room {}", roomParticipant.getId(), roomParticipant.getRoomId());
        getMultipoint(connector).modifyRoomParticipant(roomParticipant);
        return null;
    }

    @Override
    public String toString()
    {
        return String.format(ModifyRoomParticipant.class.getSimpleName() + " %s", roomParticipant);
    }
}
