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
    public RoomUser[] listParticipants(Types.SecurityToken token, String roomId)
    {
        throw new RuntimeException("TODO: Implement RoomServiceImpl.listParticipants");
    }

    @Override
    public RoomUser getParticipant(Types.SecurityToken token, String roomId, String userId)
    {
        throw new RuntimeException("TODO: Implement RoomServiceImpl.getParticipant");
    }

    @Override
    public void modifyParticipant(Types.SecurityToken token, String roomId, String userId, Map attributes)
    {
        throw new RuntimeException("TODO: Implement RoomServiceImpl.modifyParticipant");
    }

    @Override
    public void disconnectParticipant(Types.SecurityToken token, String roomId, String userId)
    {
        throw new RuntimeException("TODO: Implement RoomServiceImpl.disconnectParticipant");
    }*/
}
