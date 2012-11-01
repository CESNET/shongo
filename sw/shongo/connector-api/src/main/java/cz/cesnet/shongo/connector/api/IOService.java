package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.api.CommandException;
import cz.cesnet.shongo.api.CommandUnsupportedException;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface IOService
{
    /**
     * Mutes a user in a room.
     *
     * @param roomId     ID of room
     * @param roomUserId ID of room user to mute
     */
    void muteParticipant(String roomId, String roomUserId) throws CommandException, CommandUnsupportedException;

    /**
     * Unmutes a user in a room.
     *
     * @param roomId     ID of room
     * @param roomUserId ID of room user to unmute
     */
    void unmuteParticipant(String roomId, String roomUserId) throws CommandException, CommandUnsupportedException;

    /**
     * Sets microphone audio level of a user in a room to a given value.
     *
     * @param roomId     ID of room
     * @param roomUserId ID of room user to adjust the settings for
     * @param level      microphone level to set, in range 0 to 100 (the implementing connector should adapt this value to
     *                   the range for its managed device)
     */
    void setParticipantMicrophoneLevel(String roomId, String roomUserId, int level)
            throws CommandException, CommandUnsupportedException;

    /**
     * Sets playback audio level of a user in a room to a given value.
     *
     * @param roomId     ID of room
     * @param roomUserId ID of room user to adjust the settings for
     * @param level      playback level to set, in range 0 to 100 (the implementing connector should adapt this value to
     *                   the range for its managed device)
     */
    void setParticipantPlaybackLevel(String roomId, String roomUserId, int level)
            throws CommandException, CommandUnsupportedException;

    /**
     * Enables video from a user in a room.
     *
     * @param roomId     ID of room
     * @param roomUserId ID of room user to enable video from
     */
    void enableParticipantVideo(String roomId, String roomUserId) throws CommandException, CommandUnsupportedException;

    /**
     * Disables video from a user in a room.
     *
     * @param roomId     ID of room
     * @param roomUserId ID of room user to disable video from
     */
    void disableParticipantVideo(String roomId, String roomUserId) throws CommandException, CommandUnsupportedException;

    /**
     * Enables a given room user as a content provider in the room. This is typically enabled by default.
     *
     * @param roomId     room identifier
     * @param roomUserId identifier of the user within the given room
     */
    void enableContentProvider(String roomId, String roomUserId) throws CommandException, CommandUnsupportedException;

    /**
     * Disables a given room user as a content provider in the room.
     * <p/>
     * Typically, all users are allowed to fight for being the content provider. Using this method, a user is not
     * allowed to do this.
     *
     * @param roomId     room identifier
     * @param roomUserId identifier of the user within the given room
     */
    void disableContentProvider(String roomId, String roomUserId) throws CommandException, CommandUnsupportedException;
}
