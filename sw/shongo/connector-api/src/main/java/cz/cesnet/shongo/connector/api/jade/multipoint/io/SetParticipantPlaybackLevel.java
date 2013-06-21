package cz.cesnet.shongo.connector.api.jade.multipoint.io;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class SetParticipantPlaybackLevel extends ConnectorCommand
{
    private String roomId;
    private String roomUserId;
    private int level;

    public SetParticipantPlaybackLevel()
    {
    }

    public SetParticipantPlaybackLevel(String roomId, String roomUserId, int level)
    {
        this.roomId = roomId;
        this.roomUserId = roomUserId;
        this.level = level;
    }

    public String getRoomId()
    {
        return roomId;
    }

    public void setRoomId(String roomId)
    {
        this.roomId = roomId;
    }

    public String getRoomUserId()
    {
        return roomUserId;
    }

    public void setRoomUserId(String roomUserId)
    {
        this.roomUserId = roomUserId;
    }

    public int getLevel()
    {
        return level;
    }

    public void setLevel(int level)
    {
        this.level = level;
    }

    @Override
    public Object execute(CommonService connector) throws CommandException, CommandUnsupportedException
    {
        logger.debug("Setting playback level to {} for participant {} in room {}",
                new Object[]{level, roomUserId, roomId});
        getMultipoint(connector).setParticipantPlaybackLevel(roomId, roomUserId, level);
        return null;
    }

    @Override
    public String toString()
    {
        return String.format(SetParticipantPlaybackLevel.class.getSimpleName()
                + " (roomId: %s, roomUserId: %s, level: %d)", roomId, roomUserId, level);
    }
}
