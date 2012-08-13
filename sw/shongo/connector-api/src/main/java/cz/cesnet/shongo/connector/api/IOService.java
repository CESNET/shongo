package cz.cesnet.shongo.connector.api;

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
    void muteUser(String roomUserId);

    /**
     * Unmutes a user in a room.
     *
     * @param roomUserId ID of room user to unmute
     */
    void unmuteUser(String roomUserId);

    /**
     * Sets microphone audio level of a user in a room to a given value.
     *
     * @param roomUserId ID of room user to adjust the settings for
     * @param level      microphone level to set
     */
    void setUserMicrophoneLevel(String roomUserId, int level);

    /**
     * Sets playback audio level of a user in a room to a given value.
     *
     * @param roomUserId ID of room user to adjust the settings for
     * @param level      microphone level to set
     */
    void setUserPlaybackLevel(String roomUserId, int level);

    /**
     * Enables video from a user in a room.
     *
     * @param roomUserId ID of room user to enable video from
     */
    void enableUserVideo(String roomUserId);

    /**
     * Disables video from a user in a room.
     *
     * @param roomUserId ID of room user to disable video from
     */
    void disableUserVideo(String roomUserId);
}
