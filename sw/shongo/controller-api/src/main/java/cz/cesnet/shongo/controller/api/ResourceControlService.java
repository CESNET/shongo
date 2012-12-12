package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.*;
import cz.cesnet.shongo.api.xmlrpc.Service;
import cz.cesnet.shongo.fault.FaultException;

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
     * @return
     * @throws FaultException
     */
    @API
    public Collection<String> getSupportedMethods(SecurityToken token, String deviceResourceId)
            throws FaultException;

    @API
    public DeviceLoadInfo getDeviceLoadInfo(SecurityToken token, String deviceResourceId) throws FaultException;

    @API
    public String dial(SecurityToken token, String deviceResourceId, String address) throws FaultException;

    @API
    public String dial(SecurityToken token, String deviceResourceId, Alias alias) throws FaultException;

    @API
    public void standBy(SecurityToken token, String deviceResourceId) throws FaultException;

    @API
    public void hangUp(SecurityToken token, String deviceResourceId, String callId) throws FaultException;

    @API
    public void hangUpAll(SecurityToken token, String deviceResourceId) throws FaultException;

    @API
    public void resetDevice(SecurityToken token, String deviceResourceId) throws FaultException;

    @API
    public void mute(SecurityToken token, String deviceResourceId) throws FaultException;

    @API
    public void unmute(SecurityToken token, String deviceResourceId) throws FaultException;

    @API
    public void setMicrophoneLevel(SecurityToken token, String deviceResourceId, int level)
            throws FaultException;

    @API
    public void setPlaybackLevel(SecurityToken token, String deviceResourceId, int level)
            throws FaultException;

    @API
    public void enableVideo(SecurityToken token, String deviceResourceId) throws FaultException;

    @API
    public void disableVideo(SecurityToken token, String deviceResourceId) throws FaultException;

    @API
    public void startPresentation(SecurityToken token, String deviceResourceId) throws FaultException;

    @API
    public void stopPresentation(SecurityToken token, String deviceResourceId) throws FaultException;

    @API
    public String dialParticipant(SecurityToken token, String deviceResourceId, String roomId, String address)
            throws FaultException;

    @API
    public String dialParticipant(SecurityToken token, String deviceResourceId, String roomId, Alias alias)
            throws FaultException;

    @API
    public void disconnectParticipant(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId) throws FaultException;

    @API
    public Room getRoom(SecurityToken token, String deviceResourceId, String roomId)
            throws FaultException;

    @API
    public String createRoom(SecurityToken token, String deviceResourceId, Room room) throws FaultException;

    /**
     * Modifies a room.
     *
     * @param token                    security token
     * @param deviceResourceId shongo-id of the device to perform the action
     * @param room                     see {@link Room}
     * @return new room id (it may have changed due to some attribute change)
     * @throws FaultException
     */
    @API
    public String modifyRoom(SecurityToken token, String deviceResourceId, Room room) throws FaultException;

    @API
    public void deleteRoom(SecurityToken token, String deviceResourceId, String roomId) throws FaultException;

    @API
    public Collection<RoomSummary> listRooms(SecurityToken token, String deviceResourceId)
            throws FaultException;

    @API
    public Collection<RoomUser> listParticipants(SecurityToken token, String deviceResourceId, String roomId)
            throws FaultException;

    @API
    public RoomUser getParticipant(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId) throws FaultException;

    @API
    public void modifyParticipant(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId, Map<String, Object> attributes) throws FaultException;

    @API
    public void muteParticipant(SecurityToken token, String deviceResourceId, String roomId, String roomUserId)
            throws FaultException;

    @API
    public void unmuteParticipant(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId) throws FaultException;

    @API
    public void enableParticipantVideo(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId) throws FaultException;

    @API
    public void disableParticipantVideo(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId) throws FaultException;

    @API
    public void setParticipantMicrophoneLevel(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId, int level) throws FaultException;

    @API
    public void setParticipantPlaybackLevel(SecurityToken token, String deviceResourceId, String roomId,
            String roomUserId, int level) throws FaultException;

    @API
    public void showMessage(SecurityToken token, String deviceResourceIdentifier, int duration, String text)
            throws FaultException;
}
