package cz.cesnet.shongo.controller.api.jade;

import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.UserInformation;

/**
 * Defines a set of API methods which controller provides through JADE middle-ware.
 * For each method is defined one {@link ControllerCommand} which can be used
 * by JADE agents to invoke the method (it executes it in the {@link ControllerCommand#execute(Service)}).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface Service
{
    /**
     * @param userId for which the {@link UserInformation} should be returned
     * @return {@link UserInformation} for given {@code userId}
     */
    public UserInformation getUserInformation(String userId);

    /**
     * Get room information.
     *
     * @param roomId of the room which should be retrieved
     * @return {@link Room} by given {@code roomId}
     */
    public Room getRoom(String roomId);

    /**
     * Notify all owners for the room with given {@code roomId} by given {@code message}.
     *
     * @param roomId of the room whose owners should be notified
     * @param message which should be used for notification
     */
    public void notifyRoomOwners(String roomId, String message);
}
