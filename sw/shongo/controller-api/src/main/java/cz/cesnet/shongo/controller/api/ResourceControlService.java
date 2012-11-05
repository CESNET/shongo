package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.RoomSummary;
import cz.cesnet.shongo.api.RoomUser;
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
     * @param deviceResourceIdentifier
     * @return
     * @throws FaultException
     */
    @API
    public Collection<String> getSupportedMethods(SecurityToken token, String deviceResourceIdentifier)
            throws FaultException;

    @API
    public String dial(SecurityToken token, String deviceResourceIdentifier, String address) throws FaultException;

    @API
    public String dial(SecurityToken token, String deviceResourceIdentifier, Alias alias) throws FaultException;

    @API
    public void standBy(SecurityToken token, String deviceResourceIdentifier) throws FaultException;

    @API
    public void hangUp(SecurityToken token, String deviceResourceIdentifier, String callId) throws FaultException;

    @API
    public void hangUpAll(SecurityToken token, String deviceResourceIdentifier) throws FaultException;

    @API
    public void resetDevice(SecurityToken token, String deviceResourceIdentifier) throws FaultException;

    @API
    public void mute(SecurityToken token, String deviceResourceIdentifier) throws FaultException;

    @API
    public void unmute(SecurityToken token, String deviceResourceIdentifier) throws FaultException;

    @API
    public void setMicrophoneLevel(SecurityToken token, String deviceResourceIdentifier, int level)
            throws FaultException;

    @API
    public void setPlaybackLevel(SecurityToken token, String deviceResourceIdentifier, int level)
            throws FaultException;

    @API
    public void enableVideo(SecurityToken token, String deviceResourceIdentifier) throws FaultException;

    @API
    public void disableVideo(SecurityToken token, String deviceResourceIdentifier) throws FaultException;

    @API
    public void startPresentation(SecurityToken token, String deviceResourceIdentifier) throws FaultException;

    @API
    public void stopPresentation(SecurityToken token, String deviceResourceIdentifier) throws FaultException;

    @API
    public String dialParticipant(SecurityToken token, String deviceResourceIdentifier, String roomId, String address)
            throws FaultException;

    @API
    public String dialParticipant(SecurityToken token, String deviceResourceIdentifier, String roomId, Alias alias)
            throws FaultException;

    @API
    public void disconnectParticipant(SecurityToken token, String deviceResourceIdentifier, String roomId,
            String roomUserId) throws FaultException;

    @API
    public RoomSummary getRoomSummary(SecurityToken token, String deviceResourceIdentifier, String roomId)
            throws FaultException;

    @API
    public String createRoom(SecurityToken token, String deviceResourceIdentifier, Room room) throws FaultException;

    /**
     * Modifies a room.
     *
     * @param token                    security token
     * @param deviceResourceIdentifier identifier of the device to perform the action
     * @param roomId                   identifier of the room to modify
     * @param attributes               attributes to change; attribute having <code>null</code> is unset on the room
     * @param options                  room options to change; options having <code>null</code> are unset
     * @return new room identifier (it may have changed due to some attribute change)
     * @throws FaultException
     */
    @API
    public String modifyRoom(SecurityToken token, String deviceResourceIdentifier, String roomId,
            Map<String, Object> attributes, Map<Room.Option, Object> options) throws FaultException;

    @API
    public void deleteRoom(SecurityToken token, String deviceResourceIdentifier, String roomId) throws FaultException;

    @API
    public Collection<RoomSummary> listRooms(SecurityToken token, String deviceResourceIdentifier)
            throws FaultException;

    @API
    public Collection<RoomUser> listParticipants(SecurityToken token, String deviceResourceIdentifier, String roomId)
            throws FaultException;

    @API
    public RoomUser getParticipant(SecurityToken token, String deviceResourceIdentifier, String roomId,
            String roomUserId) throws FaultException;

    @API
    public void modifyParticipant(SecurityToken token, String deviceResourceIdentifier, String roomId,
            String roomUserId, Map<String, Object> attributes) throws FaultException;

    @API
    public void muteParticipant(SecurityToken token, String deviceResourceIdentifier, String roomId, String roomUserId)
            throws FaultException;

    @API
    public void unmuteParticipant(SecurityToken token, String deviceResourceIdentifier, String roomId,
            String roomUserId) throws FaultException;

    @API
    public void enableParticipantVideo(SecurityToken token, String deviceResourceIdentifier, String roomId,
            String roomUserId) throws FaultException;

    @API
    public void disableParticipantVideo(SecurityToken token, String deviceResourceIdentifier, String roomId,
            String roomUserId) throws FaultException;

    @API
    public void setParticipantMicrophoneLevel(SecurityToken token, String deviceResourceIdentifier, String roomId,
            String roomUserId, int level) throws FaultException;

    @API
    public void setParticipantPlaybackLevel(SecurityToken token, String deviceResourceIdentifier, String roomId,
            String roomUserId, int level) throws FaultException;
}
