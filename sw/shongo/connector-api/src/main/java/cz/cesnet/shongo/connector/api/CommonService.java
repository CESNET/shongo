package cz.cesnet.shongo.connector.api;

/**
 * Common connector API.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface CommonService
{
    /**
     * Get information about connector.
     * @return information about the connector
     */
    ConnectorInfo getConnectorInfo();

    /**
     * Mutes a user in a room.
     * @param roomUserId    ID of room user to mute
     */
    void muteRoomUser(String roomUserId);

    /**
     * Unmutes a user in a room.
     * @param roomUserId    ID of room user to unmute
     */
    void unmuteRoomUser(String roomUserId);

    /**
     * Sets microphone audio level of a user in a room to a given value.
     * @param roomUserId    ID of room user to adjust the settings for
     * @param level         microphone level to set
     */
    void setMicrophoneLevel(String roomUserId, int level);

    /**
     * Sets playback audio level of a user in a room to a given value.
     * @param roomUserId    ID of room user to adjust the settings for
     * @param level         microphone level to set
     */
    void setPlaybackLevel(String roomUserId, int level);

    /**
     * Enables video from a user in a room.
     * @param roomUserId    ID of room user to enable video from
     */
    void enableUserVideo(String roomUserId);

    /**
     * Disables video from a user in a room.
     * @param roomUserId    ID of room user to disable video from
     */
    void disableUserVideo(String roomUserId);
}
