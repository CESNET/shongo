package cz.cesnet.shongo.connector.api.jade.multipoint;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

import java.util.HashSet;
import java.util.Set;

/**
 * Gets snapshots for participants in a room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link cz.cesnet.shongo.connector.api.RoomService#getRoomParticipantSnapshots}
 */
public class GetRoomParticipantSnapshots extends ConnectorCommand
{
    private String roomId;
    private Set<String> roomParticipantIds = new HashSet<String>();

    public GetRoomParticipantSnapshots()
    {
    }

    public GetRoomParticipantSnapshots(String roomId, Set<String> roomParticipantIds)
    {
        this.roomId = roomId;
        this.roomParticipantIds.addAll(roomParticipantIds);
    }

    public String getRoomId()
    {
        return roomId;
    }

    public void setRoomId(String roomId)
    {
        this.roomId = roomId;
    }

    public Set<String> getRoomParticipantIds()
    {
        return roomParticipantIds;
    }

    public void setRoomParticipantIds(Set<String> roomParticipantIds)
    {
        this.roomParticipantIds = roomParticipantIds;
    }

    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Getting snapshots for participants {} in room {}", roomParticipantIds, roomId);
        return getMultipoint(connector).getRoomParticipantSnapshots(roomId, roomParticipantIds);
    }

    @Override
    public String toString()
    {
        return String
                .format(GetRoomParticipantSnapshots.class.getSimpleName() + " (roomId: %s, roomParticipantIds: %s)",
                        roomId, roomParticipantIds);
    }
}
