package cz.cesnet.shongo.controller.api.jade;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.UserInformation;

/**
 * Defines a set of API methods which controller provides through JADE middle-ware.
 * For each method is defined one {@link ControllerCommand} which can be used
 * by JADE agents to invoke the method (it executes it in the {@link ControllerCommand#execute}).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface Service
{
    /**
     * @param userId for which the {@link UserInformation} should be returned
     * @return {@link UserInformation} for given {@code userId}
     */
    public UserInformation getUserInformation(String userId) throws CommandException;

    /**
     * @param userOriginalId original id of the user from identity provider
     * @return {@link UserInformation} for user with given {@code userOriginalId}
     * @throws CommandException
     */
    public UserInformation getUserInformationByOriginalId(String userOriginalId) throws CommandException;

    /**
     * Get room information.
     *
     * @param roomId of the room which should be retrieved
     * @return {@link Room} by given {@code roomId}
     */
    public Room getRoom(String agentName, String roomId) throws CommandException;

    /**
     * Notify given {@code targetType} with given {@code targetId} by given {@code message}.
     *
     * @param targetType {@link NotifyTargetType} defining who should be notified
     * @param targetId   identifier defining the identity of {@link NotifyTargetType} who should be notified
     * @param title      title of the message
     * @param message    which should be used for notification
     */
    public void notifyTarget(String agentName, NotifyTargetType targetType, String targetId,
            String title, String message) throws CommandException;

    /**
     * Enumeration of all possible notification targets.
     */
    public static enum NotifyTargetType
    {
        /**
         * A single user should be notified. The {@code targetId} must contain user-id.
         */
        USER,

        /**
         * All owners of a single {@link Room} should be notified. The {@code targetId} must contain {@link Room#id}.
         */
        ROOM_OWNERS
    }
}
