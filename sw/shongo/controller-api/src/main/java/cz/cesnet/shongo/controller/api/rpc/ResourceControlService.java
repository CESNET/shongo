package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.api.rpc.Service;
import cz.cesnet.shongo.controller.api.SecurityToken;

import java.util.Collection;
import java.util.Map;

/**
 * Interface to the service handling control operations on resources.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface ResourceControlService extends Service
{
    /**
     * Gets collection of method names supported by the identified device.
     * <p/>
     * Watch out, that the names refer to methods on the connector interfaces, not from this API!
     * For example, the listRooms() method from this interface was in fact implemented by calling the getRoomList()
     * method on connector, so the returned list of supported methods must have contained "getRoomList" rather than
     * "listRooms".
     * <p/>
     * FIXME: fix the aforementioned shortcoming - return directly the names from this interface
     *
     * @param token
     * @param deviceResourceId
     * @return collection of supported method names
     */
    @API
    public Collection<String> getSupportedMethods(SecurityToken token, String deviceResourceId);

    @API
    public DeviceLoadInfo getDeviceLoadInfo(SecurityToken token, String deviceResourceId);

    @API
    public String dial(SecurityToken token, String deviceResourceId, Alias alias);

    @API
    public void standBy(SecurityToken token, String deviceResourceId);

    @API
    public void hangUp(SecurityToken token, String deviceResourceId, String callId);

    @API
    public void hangUpAll(SecurityToken token, String deviceResourceId);

    @API
    public void rebootDevice(SecurityToken token, String deviceResourceId);

    @API
    public void mute(SecurityToken token, String deviceResourceId);

    @API
    public void unmute(SecurityToken token, String deviceResourceId);

    @API
    public void setMicrophoneLevel(SecurityToken token, String deviceResourceId, int level);

    @API
    public void setPlaybackLevel(SecurityToken token, String deviceResourceId, int level);

    @API
    public void enableVideo(SecurityToken token, String deviceResourceId);

    @API
    public void disableVideo(SecurityToken token, String deviceResourceId);

    @API
    public void startPresentation(SecurityToken token, String deviceResourceId);

    @API
    public void stopPresentation(SecurityToken token, String deviceResourceId);

    @API
    public String dialRoomParticipant(SecurityToken token, String deviceResourceId, String roomId, Alias alias);

    @API
    public void disconnectRoomParticipant(SecurityToken token, String deviceResourceId, String roomId,
            String roomParticipantId);

    @API
    public Room getRoom(SecurityToken token, String deviceResourceId, String roomId);

    @API
    public String createRoom(SecurityToken token, String deviceResourceId, Room room);

    /**
     * Modifies a room.
     *
     * @param token            security token
     * @param deviceResourceId shongo-id of the device to perform the action
     * @param room             see {@link Room}
     * @return new room id (it may have changed due to some attribute change)
     */
    @API
    public String modifyRoom(SecurityToken token, String deviceResourceId, Room room);

    @API
    public void deleteRoom(SecurityToken token, String deviceResourceId, String roomId);

    @API
    public Collection<RoomSummary> listRooms(SecurityToken token, String deviceResourceId);

    @API
    public Collection<RoomParticipant> listRoomParticipants(SecurityToken token, String deviceResourceId, String roomId);

    @API
    public RoomParticipant getRoomParticipant(SecurityToken token, String deviceResourceId, String roomId,
            String roomParticipantId);

    @API
    public void modifyRoomParticipant(SecurityToken token, String deviceResourceId, RoomParticipant roomParticipant);

    @API
    public void muteRoomParticipant(SecurityToken token, String deviceResourceId, String roomId,
            String roomParticipantId);

    @API
    public void unmuteRoomParticipant(SecurityToken token, String deviceResourceId, String roomId,
            String roomParticipantId);

    @API
    public void enableRoomParticipantVideo(SecurityToken token, String deviceResourceId, String roomId,
            String roomParticipantId);

    @API
    public void disableRoomParticipantVideo(SecurityToken token, String deviceResourceId, String roomId,
            String roomParticipantId);

    @API
    public void setRoomParticipantMicrophoneLevel(SecurityToken token, String deviceResourceId, String roomId,
            String roomParticipantId, int level);

    @API
    public void setRoomParticipantPlaybackLevel(SecurityToken token, String deviceResourceId, String roomId,
            String roomParticipantId, int level);

    @API
    public void showMessage(SecurityToken token, String deviceResourceId, int duration, String text);


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Recording service.
    //

    /**
     * @param token
     * @param deviceResourceId
     * @param roomId identifier of room
     * @return list of recording URLs for room with given {@code roomId}
     */
    @API
    public Collection<Recording> listRecordings(SecurityToken token, String deviceResourceId, String roomId);
}
