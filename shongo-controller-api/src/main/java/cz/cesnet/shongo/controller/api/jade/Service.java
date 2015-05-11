package cz.cesnet.shongo.controller.api.jade;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.Room;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.api.jade.CommandException;

import java.util.List;
import java.util.Map;

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
     * @return {@link UserInformation} for given {@code userId} or null when the user doesn't exist
     */
    public UserInformation getUserInformation(String userId);

    /**
     * @param userPrincipalName of the user from identity provider
     * @return {@link UserInformation} for user with given {@code userPrincipalName} or null when the user doesn't exist
     */
    public UserInformation getUserInformationByPrincipalName(String userPrincipalName);

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
     * @param titles     titles of the message by languages ({@code null} can be used as default language)
     * @param messages   which should be used for notification ({@code null} can be used as default language)
     */
    public void notifyTarget(String agentName, NotifyTargetType targetType, String targetId,
            Map<String, String> titles, Map<String, String> messages) throws CommandException;

    /**
     * @param agentName
     * @param roomId
     * @return recording folder id for room with given {@code roomId}
     */
    public String getRecordingFolderId(String agentName, String roomId) throws CommandException;

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
        ROOM_OWNERS,

        /**
         * Resource administrators. The {@code targetId} must be {@code null}.
         */
        RESOURCE_ADMINS,

        /**
         * All owners of recording folder should be notified. The {@code targetId} must contain {@link GetRecordingFolderId}.
         */
        REC_FOLDER_OWNERS,

        /**
         * All users with access to recording folder should be notified. The {@code targetId} must contain {@link GetRecordingFolderId}.
         */
        REC_FOLDER_READERS
    }
}
