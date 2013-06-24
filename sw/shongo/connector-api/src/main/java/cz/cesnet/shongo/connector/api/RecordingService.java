package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.CommandUnsupportedException;
import cz.cesnet.shongo.api.RoomLayout;

import java.util.Collection;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public interface RecordingService
{
    /**
     * Immediately starts recording in a room.
     * <p/>
     * If the room is already being recorded, it does not have any effect and returns 0.
     *
     * @param roomId identifier of a room to record
     * @param format video format to record in
     * @param layout room layout to use; if NULL, the room default layout is used
     * @return recording identifier for further reference, unique among other recordings on the device;
     *         or 0 if the room is already being recorded
     */
    int startRecording(String roomId, ContentType format, RoomLayout layout) throws CommandException,
                                                                                    CommandUnsupportedException;

    /**
     * Stops recording.
     *
     * @param recordingId identifier of the recording to stop, previously returned by the <code>startRecording</code>
     *                    method
     */
    void stopRecording(int recordingId) throws CommandException, CommandUnsupportedException;

    /**
     * Returns a URL from where it is possible to download a recording.
     *
     * @param recordingId identifier of the recording, previously returned by the <code>startRecording</code> method
     * @return URL to download a recording
     */
    String getRecordingDownloadURL(int recordingId) throws CommandException, CommandUnsupportedException;

    /**
     * Returns URL for every recording.
     *
     * @param roomId identifier of room
     * @return collection of URLs
     * @throws CommandException
     * @throws CommandUnsupportedException
     */
    Collection<String> listRecordings(String roomId) throws CommandException, CommandUnsupportedException;

    /**
     * Lists all participants present during a given recording.
     * <p/>
     * Suitable for sending notifications about availability of the recording.
     *
     * @param recordingId identifier of the recording, previously returned by the <code>startRecording</code> method
     * @return array of identifiers of users present in any moment of the recording
     */
    Collection<String> notifyParticipants(int recordingId) throws CommandException, CommandUnsupportedException;

    /**
     * Starts downloading a recording to a local storage.
     *
     * @param downloadURL URL to download from
     * @param targetPath  path under which to store the recording on the server
     */
    void downloadRecording(String downloadURL, String targetPath) throws CommandException, CommandUnsupportedException;

    /**
     * Deletes a given recording.
     * <p/>
     * If the recording is being worked with somehow (still being recorded, being uploaded, etc.), the operation is
     * deferred to the moment when current operations are completed.
     *
     * @param recordingId identifier of the recording, previously returned by the <code>startRecording</code> method
     */
    void deleteRecording(int recordingId) throws CommandException, CommandUnsupportedException;
}
