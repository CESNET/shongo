package cz.cesnet.shongo.controller.impl;

import cz.cesnet.shongo.controller.api.API;
import cz.cesnet.shongo.controller.api.RoomService;
import cz.cesnet.shongo.controller.api.RoomUser;

import java.util.Map;

/**
 * Room service implementation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomServiceImpl implements RoomService
{
    @Override
    public String getServiceName()
    {
        return "Room";
    }

    @Override
    public RoomUser[] listRoomUsers(API.SecurityToken token, String roomId)
    {
        throw new RuntimeException("TODO: Implement RoomServiceImpl.listRoomUsers");
    }

    @Override
    public RoomUser getRoomUser(API.SecurityToken token, String roomId, String userId)
    {
        throw new RuntimeException("TODO: Implement RoomServiceImpl.getRoomUser");
    }

    @Override
    public void modifyRoomUser(API.SecurityToken token, String roomId, String userId, Map attributes)
    {
        throw new RuntimeException("TODO: Implement RoomServiceImpl.modifyRoomUser");
    }

    @Override
    public void disconnectRoomUser(API.SecurityToken token, String roomId, String userId)
    {
        throw new RuntimeException("TODO: Implement RoomServiceImpl.disconnectRoomUser");
    }
}
