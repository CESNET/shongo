package cz.cesnet.shongo.connector.api;

import java.util.Map;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface UserService
{
    /**
     * Lists all users present in a virtual room.
     *
     * @param roomId room identifier
     * @return array of room users
     */
    RoomUser[] listRoomUsers(String roomId);

    /**
     * Gets user information and settings in a room.
     *
     * @param roomId     room identifier
     * @param roomUserId identifier of the user within the given room
     * @return
     */
    RoomUser getRoomUser(String roomId, String roomUserId);

    /**
     * Modifies user settings in the room.
     * <p/>
     * Suitable for setting microphone/playback level, muting/unmuting, user layout, ...
     *
     * @param roomId     room identifier
     * @param roomUserId identifier of the user within the given room
     * @param attributes map of attributes to change
     */
    void modifyRoomUser(String roomId, String roomUserId, Map attributes);

    /**
     * Disconnects a user from a room.
     *
     * @param roomId     room identifier
     * @param roomUserId identifier of the user within the given room
     */
    void disconnectRoomUser(String roomId, String roomUserId);

    /**
     * Enables a given room user as a content provider in the room. This is typically enabled by default.
     *
     * @param roomId     room identifier
     * @param roomUserId identifier of the user within the given room
     */
    void enableContentProvider(String roomId, String roomUserId);

    /**
     * Disables a given room user as a content provider in the room.
     * <p/>
     * Typically, all users are allowed to fight for being the content provider. Using this method, a user is not
     * allowed to do this.
     *
     * @param roomId     room identifier
     * @param roomUserId identifier of the user within the given room
     */
    void disableContentProvider(String roomId, String roomUserId);
}
