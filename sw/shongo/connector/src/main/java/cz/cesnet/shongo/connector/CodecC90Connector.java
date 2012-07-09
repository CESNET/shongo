package cz.cesnet.shongo.connector;

import cz.cesnet.shongo.connector.api.*;

import java.util.Map;

/**
 * A connector for Cisco TelePresence System Codec C90.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CodecC90Connector implements MultipointService
{
    @Override
    public DeviceLoadInfo getDeviceLoadInfo()
    {
        return null;
    }

    @Override
    public UsageStats getUsageStats()
    {
        return null;
    }

    @Override
    public RoomInfo[] getRoomList()
    {
        return new RoomInfo[0];
    }

    @Override
    public MediaData getReceivedVideoSnapshot(String roomId, String roomUserId)
    {
        return null;
    }

    @Override
    public MediaData getSentVideoSnapshot(String roomId, String roomUserId)
    {
        return null;
    }

    @Override
    public int startRecording(String roomId, ContentType format, RoomLayout layout)
    {
        return 0;
    }

    @Override
    public void stopRecording(int recordingId)
    {
    }

    @Override
    public String getRecordingDownloadURL(int recordingId)
    {
        return null;
    }

    @Override
    public String[] notifyParticipants(int recordingId)
    {
        return new String[0];
    }

    @Override
    public void downloadRecording(String downloadURL, String targetPath)
    {
    }

    @Override
    public void deleteRecording(int recordingId)
    {
    }

    @Override
    public MediaData getRoomContent(String roomId)
    {
        return null;
    }

    @Override
    public void addRoomContent(String roomId, String name, MediaData data)
    {
    }

    @Override
    public void removeRoomContentFile(String roomId, String name)
    {
    }

    @Override
    public void clearRoomContent(String roomId)
    {
    }

    @Override
    public RoomInfo getRoomInfo(String roomId)
    {
        return null;
    }

    @Override
    public String createRoom(Room room)
    {
        return null;
    }

    @Override
    public void modifyRoom(String roomId, Map attributes)
    {
    }

    @Override
    public void deleteRoom(String roomId)
    {
    }

    @Override
    public String exportRoomSettings(String roomId)
    {
        return null;
    }

    @Override
    public void importRoomSettings(String roomId, String settings)
    {
    }

    @Override
    public RoomUser[] listRoomUsers(String roomId)
    {
        return new RoomUser[0];
    }

    @Override
    public RoomUser getRoomUser(String roomId, String roomUserId)
    {
        return null;
    }

    @Override
    public void modifyRoomUser(String roomId, String roomUserId, Map attributes)
    {
    }

    @Override
    public void disconnectRoomUser(String roomId, String roomUserId)
    {
    }

    @Override
    public void enableContentProvider(String roomId, String roomUserId)
    {
    }

    @Override
    public void disableContentProvider(String roomId, String roomUserId)
    {
    }
}

