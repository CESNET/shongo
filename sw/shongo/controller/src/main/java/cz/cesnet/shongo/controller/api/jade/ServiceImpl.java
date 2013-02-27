package cz.cesnet.shongo.controller.api.jade;

import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.Authorization;

/**
 * Implementation of {@link Service}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ServiceImpl implements Service
{
    /**
     * Constructor.
     */
    public ServiceImpl()
    {
    }

    @Override
    public UserInformation getUserInformation(String userId)
    {
        return Authorization.getInstance().getUserInformation(userId);
    }

    @Override
    public Room getRoom(String roomId)
    {
        throw new RuntimeException("TODO: Implement ServiceImpl.getRoom");
    }

    @Override
    public void notifyRoomOwners(String roomId, String message)
    {
        throw new RuntimeException("TODO: Implement ServiceImpl.notifyRoomOwners");
    }
}
