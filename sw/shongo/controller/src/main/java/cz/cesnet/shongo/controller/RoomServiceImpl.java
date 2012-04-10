package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.common.api.SecurityToken;
import cz.cesnet.shongo.controller.api.RoomService;
import cz.cesnet.shongo.controller.api.RoomUser;

import java.util.Map;

/**
 * Room service implementation.
 *
 * @author Martin Srom
 */
public class RoomServiceImpl implements RoomService
{
    @Override
    public RoomUser[] listRoomUsers(SecurityToken token, String roomId)
    {
        throw new RuntimeException("TODO: Implement RoomServiceImpl.listRoomUsers");
    }

    @Override
    public RoomUser getRoomUser(SecurityToken token, String roomId, String userId)
    {
        throw new RuntimeException("TODO: Implement RoomServiceImpl.getRoomUser");
    }

    @Override
    public void modifyRoomUser(SecurityToken token, String roomId, String userId, Map attributes)
    {
        throw new RuntimeException("TODO: Implement RoomServiceImpl.modifyRoomUser");
    }

    @Override
    public void disconnectRoomUser(SecurityToken token, String roomId, String userId)
    {
        throw new RuntimeException("TODO: Implement RoomServiceImpl.disconnectRoomUser");
    }
}
