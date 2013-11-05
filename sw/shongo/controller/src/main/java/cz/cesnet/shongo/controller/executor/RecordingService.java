package cz.cesnet.shongo.controller.executor;


import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.RecordingCapability;

import javax.persistence.*;

/**
 * {@link EndpointService} for recording.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class RecordingService extends EndpointService
{
    /**
     * {@link RecordingCapability} of {@link DeviceResource} which is used for recording.
     */
    private RecordingCapability recordingCapability;

    /**
     * Current identifier of {@link cz.cesnet.shongo.api.Recording}.
     */
    private String recordingId;

    /**
     * @return {@link #recordingCapability}
     */
    @ManyToOne
    @Access(AccessType.FIELD)
    public RecordingCapability getRecordingCapability()
    {
        return recordingCapability;
    }

    /**
     * @param recordingCapability sets the {@link #recordingCapability}
     */
    public void setRecordingCapability(RecordingCapability recordingCapability)
    {
        this.recordingCapability = recordingCapability;
    }

    /**
     * @return {@link #recordingId}
     */
    public String getRecordingId()
    {
        return recordingId;
    }

    /**
     * @param recordingId sets the {@link #recordingId}
     */
    public void setRecordingId(String recordingId)
    {
        this.recordingId = recordingId;
    }

    @PreUpdate
    protected void onUpdate()
    {
        boolean enabled = isEnabled();
        if (enabled && recordingId == null) {
            throw new IllegalStateException("Enabled recording service should have recording identifier.");
        }
        else if (!enabled && recordingId != null) {
            throw new IllegalStateException("Disabled recording service shouldn't have recording identifier.");
        }
    }
}
