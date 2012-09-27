package cz.cesnet.shongo.connector.jade;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;
import cz.cesnet.shongo.api.util.Address;
import cz.cesnet.shongo.connector.api.*;
import org.apache.commons.lang.NotImplementedException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Map;

/**
 * A connector for Cisco TelePresence MCU.
 * <p/>
 * Works using API 2.9.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CiscoMCUConnector implements MultipointService
{

    /**
     * An example of interaction with the device.
     * <p/>
     * Just for debugging purposes.
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException, CommandException, CommandUnsupportedException
    {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

        final String address;
        final String username;
        final String password;

        if (args.length > 0) {
            address = args[0];
        }
        else {
            System.out.print("address: ");
            address = in.readLine();
        }

        if (args.length > 1) {
            username = args[1];
        }
        else {
            System.out.print("username: ");
            username = in.readLine();
        }

        if (args.length > 2) {
            password = args[2];
        }
        else {
            System.out.print("password: ");
            password = in.readLine();
        }

        CiscoMCUConnector conn = new CiscoMCUConnector();
        conn.connect(new Address(address), username, password);

        Collection<RoomInfo> roomList = conn.getRoomList();
        System.out.println("Existing rooms:");
        for (RoomInfo room : roomList) {
            System.out.printf("  - %s (%s, created at %s, owned by %s)\n", room.getName(), room.getType(),
                    room.getCreation(), room.getOwner());
        }

        System.out.println("All done, disconnecting");
        conn.disconnect();
    }


    /**
     * The default port number to connect to.
     */
    public static final int DEFAULT_PORT = 443;


    // COMMON SERVICE

    @Override
    public void connect(Address address, String username, String password) throws CommandException
    {
        if (address.getPort() == Address.DEFAULT_PORT) {
            address.setPort(DEFAULT_PORT);
        }

        throw new NotImplementedException("connect() is not implemented yet");
    }

    @Override
    public void disconnect() throws CommandException
    {
    }

    @Override
    public ConnectorInfo getConnectorInfo()
    {
        return null;
    }


    // ROOM SERVICE

    @Override
    public RoomInfo getRoomInfo(String roomId) throws CommandException, CommandUnsupportedException
    {
        return null;
    }

    @Override
    public String createRoom(Room room) throws CommandException, CommandUnsupportedException
    {
        return null;
    }

    @Override
    public void modifyRoom(String roomId, Map attributes) throws CommandException, CommandUnsupportedException
    {
    }

    @Override
    public void deleteRoom(String roomId) throws CommandException, CommandUnsupportedException
    {
    }

    @Override
    public String exportRoomSettings(String roomId) throws CommandException, CommandUnsupportedException
    {
        return null;
    }

    @Override
    public void importRoomSettings(String roomId, String settings) throws CommandException, CommandUnsupportedException
    {
    }


    // ROOM CONTENT SERVICE

    @Override
    public void removeRoomContentFile(String roomId, String name) throws CommandException, CommandUnsupportedException
    {
    }

    @Override
    public MediaData getRoomContent(String roomId) throws CommandException, CommandUnsupportedException
    {
        return null;
    }

    @Override
    public void clearRoomContent(String roomId) throws CommandException, CommandUnsupportedException
    {
    }

    @Override
    public void addRoomContent(String roomId, String name, MediaData data)
            throws CommandException, CommandUnsupportedException
    {
    }


    // USER SERVICE

    @Override
    public void disableContentProvider(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
    }

    @Override
    public void disconnectRoomUser(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
    }

    @Override
    public void enableContentProvider(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
    }

    @Override
    public RoomUser getRoomUser(String roomId, String roomUserId) throws CommandException, CommandUnsupportedException
    {
        return null;
    }

    @Override
    public Collection<RoomUser> listRoomUsers(String roomId) throws CommandException, CommandUnsupportedException
    {
        return null;
    }

    @Override
    public void modifyRoomUser(String roomId, String roomUserId, Map attributes)
            throws CommandException, CommandUnsupportedException
    {
    }


    // I/O Service

    @Override
    public void disableUserVideo(String roomUserId) throws CommandException, CommandUnsupportedException
    {
    }

    @Override
    public void enableUserVideo(String roomUserId) throws CommandException, CommandUnsupportedException
    {
    }

    @Override
    public void muteUser(String roomUserId) throws CommandException, CommandUnsupportedException
    {
    }

    @Override
    public void setUserMicrophoneLevel(String roomUserId, int level)
            throws CommandException, CommandUnsupportedException
    {
    }

    @Override
    public void setUserPlaybackLevel(String roomUserId, int level) throws CommandException, CommandUnsupportedException
    {
    }

    @Override
    public void unmuteUser(String roomUserId) throws CommandException, CommandUnsupportedException
    {
    }


    // RECORDING SERVICE

    @Override
    public void deleteRecording(int recordingId) throws CommandException, CommandUnsupportedException
    {
    }

    @Override
    public void downloadRecording(String downloadURL, String targetPath)
            throws CommandException, CommandUnsupportedException
    {
    }

    @Override
    public String getRecordingDownloadURL(int recordingId) throws CommandException, CommandUnsupportedException
    {
        return null;
    }

    @Override
    public Collection<String> notifyParticipants(int recordingId) throws CommandException, CommandUnsupportedException
    {
        return null;
    }

    @Override
    public int startRecording(String roomId, ContentType format, RoomLayout layout)
            throws CommandException, CommandUnsupportedException
    {
        return 0;
    }

    @Override
    public void stopRecording(int recordingId) throws CommandException, CommandUnsupportedException
    {
    }


    // MONITORING SERVICE

    @Override
    public DeviceLoadInfo getDeviceLoadInfo() throws CommandException, CommandUnsupportedException
    {
        return null;
    }

    @Override
    public UsageStats getUsageStats() throws CommandException, CommandUnsupportedException
    {
        return null;
    }

    @Override
    public Collection<RoomInfo> getRoomList() throws CommandException, CommandUnsupportedException
    {
        return null;
    }

    @Override
    public MediaData getReceivedVideoSnapshot(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
        return null;
    }

    @Override
    public MediaData getSentVideoSnapshot(String roomId, String roomUserId)
            throws CommandException, CommandUnsupportedException
    {
        return null;
    }
}
