package cz.cesnet.shongo.controller.api;

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

    /*@Override
    public RoomUser[] listRoomUsers(Types.SecurityToken token, String roomId)
    {
        throw new RuntimeException("TODO: Implement RoomServiceImpl.listRoomUsers");
    }

    @Override
    public RoomUser getRoomUser(Types.SecurityToken token, String roomId, String userId)
    {
        throw new RuntimeException("TODO: Implement RoomServiceImpl.getRoomUser");
    }

    @Override
    public void modifyRoomUser(Types.SecurityToken token, String roomId, String userId, Map attributes)
    {
        throw new RuntimeException("TODO: Implement RoomServiceImpl.modifyRoomUser");
    }

    @Override
    public void disconnectRoomUser(Types.SecurityToken token, String roomId, String userId)
    {
        throw new RuntimeException("TODO: Implement RoomServiceImpl.disconnectRoomUser");
    }*/
}
