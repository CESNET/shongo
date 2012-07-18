package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.api.Alias;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface EndpointService extends CommonService
{
    /**
     * Dials a server.
     * @param server    server address to dial
     */
    void dial(Alias server) throws CommandException;

    /**
     * Resets the device.
     */
    void resetDevice() throws CommandException;

    /**
     * Mutes this endpoint.
     */
    void mute() throws CommandException;

    /**
     * Unmutes this endpoint.
     */
    void unmute() throws CommandException;

    /**
     * Sets microphone audio level of this endpoint to a given value.
     * @param level         microphone level to set
     */
    void setMicrophoneLevel(int level) throws CommandException;

    /**
     * Sets playback audio level of this endpoint to a given value.
     * @param level         microphone level to set
     */
    void setPlaybackLevel(int level) throws CommandException;

    /**
     * Enables video from this endpoint.
     */
    void enableVideo() throws CommandException;

    /**
     * Disables video from this endpoint.
     */
    void disableVideo() throws CommandException;

}
