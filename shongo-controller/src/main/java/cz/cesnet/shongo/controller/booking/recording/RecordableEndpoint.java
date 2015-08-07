package cz.cesnet.shongo.controller.booking.recording;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.RecordingFolder;
import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.controller.booking.executable.Endpoint;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.util.IdentifierSynchronization;

import java.util.Map;
import java.util.Set;

/**
 * Represents and {@link Endpoint} which can be recorded.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface RecordableEndpoint
{
    /**
     * @return identifier of the {@link RecordableEndpoint}
     */
    public Long getId();

    /**
     * @return {@link DeviceResource} of this endpoint
     */
    public DeviceResource getResource();

    /**
     * @return set of {@link Technology}s of this {@link RecordableEndpoint}
     */
    public Set<Technology> getTechnologies();

    /**
     * @return callable {@link Alias}
     */
    public Alias getRecordingAlias();

    /**
     * @return {@code AliasType.ROOM_NAME} as string, can be null
     */
    public String getRecordingPrefixName();

    /**
     * @return description of the folder to which recordings will be stored
     */
    public RecordingFolder getRecordingFolderApi();

    /**
     * @return identifier of recording folders by {@link RecordingCapability}
     */
    public Map<RecordingCapability, String> getRecordingFolderIds();

    /**
     * @param recordingCapability for which the identifier of the folder should be returned
     * @return identifier of recording folder where all recordings should be stored
     */
    public String getRecordingFolderId(RecordingCapability recordingCapability);

    /**
     * @param recordingCapability for which the identifier of the folder should be set
     * @param recordingFolderId sets the value returned by {@link #getRecordingFolderId}
     */
    public void putRecordingFolderId(RecordingCapability recordingCapability, String recordingFolderId);

    /**
     * @param recordingCapability for which the identifier of the folder should be removed
     */
    public void removeRecordingFolderId(RecordingCapability recordingCapability);
}
