package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.Recording;
import cz.cesnet.shongo.api.RecordingFolder;
import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;

import java.io.FileNotFoundException;
import java.util.Collection;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 * @author Ondre Pavelka <pavelka@cesnet.cz>
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface RecordingService
{
    /**
     * Create a new folder where one or more recordings can be stored.
     *
     * @param recordingFolder
     * @return identifier of newly created folder (unique among other folders on the device)
     * @throws CommandException
     */
    public String createRecordingFolder(RecordingFolder recordingFolder) throws CommandException;

    /**
     * Update existing recording folder.
     *
     * @param recordingFolder
     * @throws CommandException
     */
    public void modifyRecordingFolder(RecordingFolder recordingFolder) throws CommandException;

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
     * Returns active recording for room with alias, if any
     *
     * @param alias alias for room
     * @return recording info or null, if room is not recorded
     * @throws CommandException
     * @throws CommandUnsupportedException
     */
    public Recording getActiveRecording(Alias alias) throws CommandException, CommandUnsupportedException;

    /**
     * Check whether recording with given {@code recordingId} is currently active (is being recorded).
     *
     * @param recordingId of the recording to be checked
     * @return true when the recording is currently being recorded, false otherwise
     * @throws CommandException
     * @throws CommandUnsupportedException
     */
    public boolean isRecordingActive(String recordingId) throws CommandException, CommandUnsupportedException;

    /**
     * Immediately starts recording a meeting specified by given {@code alias}.
     *
     * @param recordingFolderId identifier of folder, where the recording should be stored
     * @param alias             alias of an endpoint which should be recorded (it can be a virtual room)
     * @param recordingPrefixName          name used for the name of the recording (can be null)
     * @param recordingSettings recording settings
     * @return identifier of the recording for further reference (unique among other recordings on the device)
     */
    public String startRecording(String recordingFolderId, Alias alias, String recordingPrefixName, RecordingSettings recordingSettings)
            throws CommandException, CommandUnsupportedException;

    /**
     * Stops recording which was started by the {@link #startRecording}.
     *
     * @param recordingId identifier of the recording to stop, previously returned by the {@link #startRecording} method
     */
    public void stopRecording(String recordingId) throws CommandException, CommandUnsupportedException;

    /**
     * Deletes a recording with given {@code recordingId}.
     * <p/>
     * If the recording is being worked with somehow (still being recorded, being uploaded, etc.), the operation is
     * deferred to the moment when current operations are completed.
     *
     * @param recordingId identifier of the recording, previously returned by the {@link #startRecording} method
     */
    public void deleteRecording(String recordingId) throws CommandException, CommandUnsupportedException;

    /**
     * Check recording in device and move it to appropriate recording folder.
     *
     * @param recordingId
     * @throws CommandException
     */
    public void checkRecording(String recordingId) throws CommandException;

    /**
     * Check recordings in device and move them to appropriate recording folder.
     *
     * @throws CommandException
     */
    public void checkRecordings() throws CommandException;

    /**
     * Make Recording public. Accessed by everyone.
     *
     * @throws CommandException
     */
    public void makeRecordingPublic(String recordingId) throws CommandException, CommandUnsupportedException;

    /**
     * Make Recording private. Authorized like recorded room.
     *
     * @throws CommandException
     */
    public void makeRecordingPrivate(String recordingId) throws CommandException, CommandUnsupportedException;

    /**
     * Make Recording public. Accessed by everyone.
     *
     * @throws CommandException
     */
    public void makeRecordingFolderPublic(String recordingFolderId) throws CommandException, CommandUnsupportedException;

    /**
     * Make Recording private. Authorized like recorded room.
     *
     * @throws CommandException
     */
    public void makeRecordingFolderPrivate(String recordingFolderId) throws CommandException, CommandUnsupportedException;

    /**
     * Returns if existing {@link RecordingFolder} is public.
     *
     * @param recordingFolderId
     */
    public boolean isRecordingFolderPublic(String recordingFolderId) throws CommandException;
}
