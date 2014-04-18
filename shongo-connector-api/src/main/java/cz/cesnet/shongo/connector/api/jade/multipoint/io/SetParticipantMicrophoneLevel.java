package cz.cesnet.shongo.connector.api.jade.multipoint.io;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.connector.api.CommonService;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class SetParticipantMicrophoneLevel extends ConnectorCommand
{
    private String roomId;
    private String roomParticipantId;
    private int level;

    public SetParticipantMicrophoneLevel()
    {
    }

    public SetParticipantMicrophoneLevel(String roomId, String roomParticipantId, int level)
    {
        this.roomId = roomId;
        this.roomParticipantId = roomParticipantId;
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

    public String getRoomParticipantId()
    {
        return roomParticipantId;
    }

    public void setRoomParticipantId(String roomParticipantId)
    {
        this.roomParticipantId = roomParticipantId;
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
        logger.debug("Setting microphone level to {} for participant {} in room {}",
                new Object[]{level, roomParticipantId, roomId});
        getMultipoint(connector).setParticipantMicrophoneLevel(roomId, roomParticipantId, level);
        return null;
    }

    @Override
    public String toString()
    {
        return String.format(SetParticipantMicrophoneLevel.class.getSimpleName()
                + " (roomId: %s, roomParticipantId: %s, level: %d)", roomId, roomParticipantId, level);
    }
}
