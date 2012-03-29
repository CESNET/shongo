package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.common.api.Duration;
import cz.cesnet.shongo.common.api.SecurityToken;
import cz.cesnet.shongo.common.api.TimeSlot;

import java.util.Map;

/**
 * Interface to the service handling operations on resources.
 *
 * @author Ondrej Bouda
 */
public interface RoomService {

    /**
     * Lists all users currently participating in a given room.
     *
     * @param token         token of the user requesting the operation
     * @param roomId        Shongo identifier of the room resource
     * @return
     */
    public RoomUser[] listRoomUsers(SecurityToken token, String roomId);

    /**
     * Disconnects a user from the room he/she is currently participating in.
     *
     * @param token         token of the user requesting the operation
     * @param user
     */
    public void disconnectRoomUser(SecurityToken token, RoomUser user);

    /**
     * Mutes a given user in the room he/she is currently participating.
     *
     * @param token         token of the user requesting the operation
     * @param user
     */
    public void muteRoomUser(SecurityToken token, RoomUser user);

    /**
     * Unmutes a given user in the room he/she is currently participating.
     *
     * @param token         token of the user requesting the operation
     * @param user
     */
    public void unmuteRoomUser(SecurityToken token, RoomUser user);

    /**
     * Sets the microphone level of a user in the room he/she is currently participating.
     *
     * @param token         token of the user requesting the operation
     * @param user
     * @param level
     */
    public void setRoomUserMicLevel(SecurityToken token, RoomUser user, int level);

    /**
     * Sets the playback level of a user in the room he/she is currently participating - alters his speakers volume.
     *
     * @param token         token of the user requesting the operation
     * @param user
     * @param level
     */
    public void setRoomUserPlaybackLevel(SecurityToken token, RoomUser user, int level);

}
