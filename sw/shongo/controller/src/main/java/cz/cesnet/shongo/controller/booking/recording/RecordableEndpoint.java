package cz.cesnet.shongo.controller.booking.recording;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.controller.booking.executable.Endpoint;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.util.IdentifierSynchronization;

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
    public DeviceResource getDeviceResource();

    /**
     * @return set of {@link Technology}s of this {@link RecordableEndpoint}
     */
    public Set<Technology> getTechnologies();

    /**
     * @return callable {@link Alias}
     */
    public Alias getRecordingAlias();

    /**
     * @return description of the folder to which recordings will be stored
     */
    public String getRecordingFolderDescription();

    /**
     * @param recordingCapability for which the identifier of the folder should be returned
     * @return identifier of recording folder where all recordings should be stored
     */
    public String getRecordingFolderId(RecordingCapability recordingCapability);

    /**
     * @param recordingFolderId sets the value returned by {@link #getRecordingFolderId}
     */
    public void putRecordingFolderId(RecordingCapability recordingCapability, String recordingFolderId);

    /**
     * {@link IdentifierSynchronization} for {@link RecordableEndpoint#getRecordingFolderId} and
     * {@link RecordableEndpoint#putRecordingFolderId}.
     */
    public static IdentifierSynchronization SYNCHRONIZATION = new IdentifierSynchronization();
}
