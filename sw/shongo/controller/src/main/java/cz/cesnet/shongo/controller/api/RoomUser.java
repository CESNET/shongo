package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.common.api.UserIdentity;

/**
 * Represents an active user in a virtual Room
 *
 * @author Martin Srom
 */
public class RoomUser
{
    /** User id in room */
    private String id;

    /** Room resource id */
    private String roomId;

    /** User identity */
    private UserIdentity user;

    public String getId() {
        return id;
    }

    public String getRoomId() {
        return roomId;
    }

    public UserIdentity getUser() {
        return user;
    }
}
