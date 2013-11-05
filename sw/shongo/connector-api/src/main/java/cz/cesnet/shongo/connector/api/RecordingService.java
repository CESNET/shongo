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
     * Create a new folder where one or more recordings can be stored.
     *
     * @param description of the folder by which the folder and it's recordings can be found in case of manual lookup
     * @return identifier of newly created folder (unique among other folders on the device)
     * @throws CommandException
     */
    public String createRecordingFolder(String description) throws CommandException;

    /**
     * Delete existing folder for storing recordings.
     *
     * @param recordingFolderId identifier of the folder to be deleted
     * @throws CommandException
     */
    public void deleteRecordingFolder(String recordingFolderId) throws CommandException;

    /**
     * Returns collections of recordings for folder with given {@code recordingFolderId}.
     *
     * @param recordingFolderId identifier of folder, where requested recordings are stored
     * @return collection of {@link Recording}s
     * @throws CommandException
     * @throws CommandUnsupportedException
     */
    public Collection<Recording> listRecordings(String recordingFolderId)
            throws CommandException, CommandUnsupportedException;

    /**
     * Returns information about the recording.
     *
     * @param recordingId identifier of the recording, previously returned by the {@link #startRecording} method
     * @return {@link Recording} with given {@code recordingId}
     */
    public Recording getRecording(String recordingId) throws CommandException, CommandUnsupportedException;

    /**
     * Immediately starts recording a meeting specified by given {@code alias}.
     *
     * @param recordingFolderId identifier of folder, where the recording should be stored
     * @param alias             alias of an endpoint which should be recorded (it can be a virtual room)
     * @return identifier of the recording for further reference (unique among other recordings on the device)
     */
    public String startRecording(String recordingFolderId, Alias alias) throws CommandException, CommandUnsupportedException;

    /**
     * Stops recording which was started by the {@link #startRecording}.
     *
     * @param recordingId identifier of the recording to stop, previously returned by the {@link #startRecording} method
     */
    void stopRecording(String recordingId) throws CommandException, CommandUnsupportedException;

    /**
     * Deletes a recording with given {@code recordingId}.
     * <p/>
     * If the recording is being worked with somehow (still being recorded, being uploaded, etc.), the operation is
     * deferred to the moment when current operations are completed.
     *
     * @param recordingId identifier of the recording, previously returned by the {@link #startRecording} method
     */
    void deleteRecording(String recordingId) throws CommandException, CommandUnsupportedException;
}
