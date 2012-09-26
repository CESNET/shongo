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
     * @param roomUserId ID of room user to mute
     */
    void muteUser(String roomUserId) throws CommandException, CommandUnsupportedException;

    /**
     * Unmutes a user in a room.
     *
     * @param roomUserId ID of room user to unmute
     */
    void unmuteUser(String roomUserId) throws CommandException, CommandUnsupportedException;

    /**
     * Sets microphone audio level of a user in a room to a given value.
     *
     * @param roomUserId ID of room user to adjust the settings for
     * @param level      microphone level to set
     */
    void setUserMicrophoneLevel(String roomUserId, int level) throws CommandException, CommandUnsupportedException;

    /**
     * Sets playback audio level of a user in a room to a given value.
     *
     * @param roomUserId ID of room user to adjust the settings for
     * @param level      microphone level to set
     */
    void setUserPlaybackLevel(String roomUserId, int level) throws CommandException, CommandUnsupportedException;

    /**
     * Enables video from a user in a room.
     *
     * @param roomUserId ID of room user to enable video from
     */
    void enableUserVideo(String roomUserId) throws CommandException, CommandUnsupportedException;

    /**
     * Disables video from a user in a room.
     *
     * @param roomUserId ID of room user to disable video from
     */
    void disableUserVideo(String roomUserId) throws CommandException, CommandUnsupportedException;
}
