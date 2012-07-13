package cz.cesnet.shongo.connector.api;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface EndpointService extends CommonService
{
    /**
     * Dials a server.
     * @param server    server address to dial
     */
    void dial(String server);

    /**
     * Resets the device.
     */
    void resetDevice();

    /**
     * Mutes this endpoint.
     */
    void mute();

    /**
     * Unmutes this endpoint.
     */
    void unmute();

    /**
     * Sets microphone audio level of this endpoint to a given value.
     * @param level         microphone level to set
     */
    void setMicrophoneLevel(int level);

    /**
     * Sets playback audio level of this endpoint to a given value.
     * @param level         microphone level to set
     */
    void setPlaybackLevel(int level);

    /**
     * Enables video from this endpoint.
     */
    void enableVideo();

    /**
     * Disables video from this endpoint.
     */
    void disableVideo();

}
