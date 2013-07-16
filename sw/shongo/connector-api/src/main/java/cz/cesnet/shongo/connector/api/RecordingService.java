package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.Recording;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;

import java.util.Collection;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 * @author Ondre Pavelka <pavelka@cesnet.cz>
 */
public interface RecordingService
{
    /**
     * Immediately starts recording in a room.
     * <p/>
     * If the room is already being recorded, it does not have any effect and returns 0.
     *
     * @param folderId identifier of room to be stored
     * @param alias room alias
     * @return recording identifier for further reference, unique among other recordings on the device;
     *         or 0 if the room is already being recorded
     */
    String startRecording(String folderId, Alias alias) throws CommandException, CommandUnsupportedException;

    /**
     * Stops recording.
     *
     * @param recordingId identifier of the recording to stop, previously returned by the <code>startRecording</code>
     *                    method
     */
    void stopRecording(String recordingId) throws CommandException, CommandUnsupportedException;

    /**
     * Returns info about the recording.
     *
     * @param recordingId identifier of the recording, previously returned by the <code>startRecording</code> method
     * @return information about a recording with recordingId
     */
    Recording getRecording(String recordingId) throws CommandException, CommandUnsupportedException;

    /**
     * Returns URL for every recording.
     *
     * @param folderId identifier of room or folder, where recordings are stored
     * @return collection of URLs
     * @throws CommandException
     * @throws CommandUnsupportedException
     */
    Collection<Recording> listRecordings(String folderId) throws CommandException, CommandUnsupportedException;

    /**
     * Deletes a given recording.
     *
     * If the recording is being worked with somehow (still being recorded, being uploaded, etc.), the operation is
     * deferred to the moment when current operations are completed.
     *
     * @param recordingId identifier of the recording, previously returned by the <code>startRecording</code> method
     */
    void deleteRecording(String recordingId) throws CommandException, CommandUnsupportedException;

    /**
     * Copy recording to specified folder on device.
     *
     * @param recordingId identifier of the recording to be copied
     * @param folderId identifier of the folder to be stored in
     * @throws CommandException
     * @throws CommandUnsupportedException
     */
    void moveRecording(String recordingId, String folderId) throws CommandException, CommandUnsupportedException;
}
