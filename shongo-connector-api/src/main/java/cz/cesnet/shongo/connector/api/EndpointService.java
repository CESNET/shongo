package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;

/**
 * Set of functionality offered by endpoint devices.
 * <p/>
 * Any of the methods may throw CommandException when a command execution fails, or CommandUnsupportedException when the
 * command is not supported (and thus may not be implemented) by the target device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface EndpointService extends CommonService
{
    /**
     * Dials a server by alias.
     *
     * @param alias alias of server to dial
     * @return call ID (suitable for further control of the call)
     */
    String dial(Alias alias) throws CommandException, CommandUnsupportedException;

    /**
     * Hangs up a call.
     * TODO: block until the call is really hung up?
     *
     * @param callId ID of the call to hang up; previously returned by dial()
     */
    void hangUp(String callId) throws CommandException, CommandUnsupportedException;

    /**
     * Hangs up all calls.
     * TODO: block until all calls are really hung up? (would simplify CodecC90Connector.standBy())
     */
    void hangUpAll() throws CommandException, CommandUnsupportedException;

    /**
     * Sets the device to standby mode.
     */
    void standBy() throws CommandException, CommandUnsupportedException;

    /**
     * Reboots the device.
     */
    void rebootDevice() throws CommandException, CommandUnsupportedException;

    /**
     * Mutes this endpoint.
     */
    void mute() throws CommandException, CommandUnsupportedException;

    /**
     * Unmutes this endpoint.
     */
    void unmute() throws CommandException, CommandUnsupportedException;

    /**
     * Sets microphone (all microphones) audio level of this endpoint to a given value.
     *
     * @param level microphone level to set, in range 0 to 100 (the implementing connector should adapt this value to
     *              the range for its managed device)
     */
    void setMicrophoneLevel(int level) throws CommandException, CommandUnsupportedException;

    /**
     * Sets playback audio level of this endpoint to a given value.
     *
     * @param level microphone level to set, in range 0 to 100 (the implementing connector should adapt this value to
     *              the range for its managed device)
     */
    void setPlaybackLevel(int level) throws CommandException, CommandUnsupportedException;

    /**
     * Enables video from this endpoint.
     */
    void enableVideo() throws CommandException, CommandUnsupportedException;

    /**
     * Disables video from this endpoint.
     */
    void disableVideo() throws CommandException, CommandUnsupportedException;

    /**
     * Starts the presentation mode (turns on the media stream).
     */
    void startPresentation() throws CommandException, CommandUnsupportedException;

    /**
     * Stops the presentation mode (turns off the media stream).
     */
    void stopPresentation() throws CommandException, CommandUnsupportedException;

    /**
     * Shows a message box to the user.
     *
     * @param duration for how long (at most) the message should be shown
     * @param text     message to show
     */
    void showMessage(int duration, String text) throws CommandException, CommandUnsupportedException;

}
