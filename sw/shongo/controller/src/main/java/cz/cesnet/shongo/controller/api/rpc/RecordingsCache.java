package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.ExpirationMap;
import cz.cesnet.shongo.controller.api.ResourceRecording;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import org.joda.time.Duration;

import java.util.Iterator;
import java.util.List;

/**
 * Cache of {@link ResourceRecording}s for {@link Executable}s.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RecordingsCache
{
    /**
     * Collection of {@link cz.cesnet.shongo.api.Recording}s by executableId.
     */
    private final ExpirationMap<Long, List<ResourceRecording>> executableRecordingsCache =
            new ExpirationMap<Long, List<ResourceRecording>>();

    /**
     * Constructor.
     */
    public RecordingsCache()
    {
        this.executableRecordingsCache.setExpiration(Duration.standardSeconds(30));
    }

    /**
     * @param executableId
     * @return list of {@link ResourceRecording}s for {@link Executable} with given {@code executableId}
     */
    public synchronized List<ResourceRecording> getExecutableRecordings(Long executableId)
    {
        return executableRecordingsCache.get(executableId);
    }

    /**
     * @param executableId
     * @param recordings to be stored for {@link Executable} with given {@code executableId}
     */
    public synchronized void putExecutableRecordings(Long executableId, List<ResourceRecording> recordings)
    {
        executableRecordingsCache.put(executableId, recordings);
    }

    /**
     * @param executableId for {@link Executable} for which the cache should be cleared
     */
    public synchronized void removeExecutableRecordings(Long executableId)
    {
        executableRecordingsCache.remove(executableId);
    }


    /**
     * Remove recording from cache.
     *
     * @param deviceResourceId
     * @param recordingFolderId
     * @param recordingId
     */
    public synchronized void removeRecording(String deviceResourceId, String recordingFolderId, String recordingId)
    {
        Iterator<List<ResourceRecording>> executableIterator = executableRecordingsCache.iterator();
        while (executableIterator.hasNext()) {
            List<ResourceRecording> recordings = executableIterator.next();
            Iterator<ResourceRecording> recordingIterator = recordings.iterator();
            while (recordingIterator.hasNext()) {
                ResourceRecording recording = recordingIterator.next();
                if (!deviceResourceId.equals(recording.getResourceId())) {
                    continue;
                }
                if (!recordingFolderId.equals(recording.getRecordingFolderId())) {
                    continue;
                }
                if (!recordingId.equals(recording.getId())) {
                    continue;
                }
                recordingIterator.remove();
            }
        }
    }
}
