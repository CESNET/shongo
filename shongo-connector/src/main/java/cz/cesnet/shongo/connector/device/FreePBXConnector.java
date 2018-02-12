package cz.cesnet.shongo.connector.device;

import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.util.DeviceAddress;
import cz.cesnet.shongo.connector.api.UsageStats;
import cz.cesnet.shongo.connector.common.AbstractMultipointConnector;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
public class FreePBXConnector extends AbstractMultipointConnector {

    @Override
    public RoomParticipant getRoomParticipant(String roomId, String roomParticipantId) throws CommandException, CommandUnsupportedException {
        return null; // TODO??
    }

    @Override
    public void addRoomContent(String roomId, String name, MediaData data) throws CommandException, CommandUnsupportedException {
        throw new CommandUnsupportedException();
    }

    @Override
    public void disconnect() throws CommandException {
        // TODO
    }

    @Override
    public MediaData getRoomContent(String roomId) throws CommandException, CommandUnsupportedException {
        return null; // TODO?
    }

    @Override
    public void deleteRoom(String roomId) throws CommandException {
        // TODO ?
    }

    @Override
    public String dialRoomParticipant(String roomId, Alias alias) throws CommandException, CommandUnsupportedException {
        return null; // TODO?
    }

    @Override
    public String createRoom(Room room) throws CommandException {
        return null; // TODO
    }

    @Override
    public DeviceLoadInfo getDeviceLoadInfo() throws CommandException, CommandUnsupportedException {
        return null; // TODO??
    }

    @Override
    public void disconnectRoomParticipant(String roomId, String roomParticipantId) throws CommandException, CommandUnsupportedException {
        // TODO??
    }

    @Override
    public Room getRoom(String roomId) throws CommandException {
        return null;
    }

    @Override
    public Collection<RoomSummary> listRooms() throws CommandException, CommandUnsupportedException {
        return null; // TODO?
    }

    @Override
    public Collection<RoomParticipant> listRoomParticipants(String roomId) throws CommandException, CommandUnsupportedException {
        return null; // TODO?
    }

    @Override
    public void importRoomSettings(String roomId, String settings) throws CommandException, CommandUnsupportedException {
        // TODO?
    }

    @Override
    public Map<String, MediaData> getRoomParticipantSnapshots(String roomId, Set<String> roomParticipantIds) throws CommandException, CommandUnsupportedException {
        return null; // TODO?
    }

    @Override
    public void clearRoomContent(String roomId) throws CommandException, CommandUnsupportedException {

    }

    @Override
    public String exportRoomSettings(String roomId) throws CommandException, CommandUnsupportedException {
        throw new CommandUnsupportedException();
    }

    @Override
    public UsageStats getUsageStats() throws CommandException, CommandUnsupportedException
    {
        throw new CommandUnsupportedException();
    }

    @Override
    public void modifyRoomParticipants(RoomParticipant roomParticipantConfiguration) throws CommandException, CommandUnsupportedException {
        // TODO
    }

    @Override
    public void modifyRoomParticipant(RoomParticipant roomParticipant) throws CommandException, CommandUnsupportedException {
        // TODO
    }

    @Override
    public void removeRoomContentFile(String roomId, String name) throws CommandException, CommandUnsupportedException {
        throw new CommandUnsupportedException();
    }

    @Override
    protected void onModifyRoom(Room room) throws CommandException {
        // TODO
    }

    @Override
    public ConnectionState getConnectionState() {
        return null;  // TODO
    }

    @Override
    public void connect(DeviceAddress deviceAddress, String username, String password) throws CommandException {
        // TODO
    }
    private static String computeSignature(String secretKey, String data) {
        try {
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(), "SHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(keySpec);
            byte[] hashBytes = mac.doFinal(data.getBytes());
            return Hex.encodeHexString(hashBytes);
        } catch (Exception e) {
            throw new Error("Error while computing message signature."); // TODO
        }
    }
}
