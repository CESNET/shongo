package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.xmlrpc.Service;

/**
 * Interface to the service handling operations on rooms.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface RoomService extends Service
{
    /**
     * Lists all users currently participating in a given room.
     *
     * @param token  token of the user requesting the operation
     * @param roomId Shongo identifier of the room resource
     * @return
     */
    //public RoomUser[] listRoomUsers(SecurityToken token, String roomId);

    /**
     * Gets a concrete room user.
     *
     * @param token  token of the user requesting the operation
     * @param roomId Shongo identifier of the room resource
     * @param userId user identifier within a concrete technology
     * @return
     */
    //public RoomUser getRoomUser(SecurityToken token, String roomId, String userId);

    /**
     * Modifies a given room user.
     *
     * @param token      token of the user requesting the operation
     * @param roomId     Shongo identifier of the room resource
     * @param userId     user identifier within a concrete technology
     * @param attributes map of room user attributes; should only contain attributes specified in the RoomUser class
     */
    //public void modifyRoomUser(SecurityToken token, String roomId, String userId, Map attributes);

    /**
     * Disconnects a user from a given room.
     *
     * @param token  token of the user requesting the operation
     * @param roomId Shongo identifier of the room resource
     * @param userId user identifier within a concrete technology
     */
    //public void disconnectRoomUser(SecurityToken token, String roomId, String userId);
}
