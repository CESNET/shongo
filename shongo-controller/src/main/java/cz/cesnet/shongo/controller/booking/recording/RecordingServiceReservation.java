package cz.cesnet.shongo.controller.booking.recording;

import cz.cesnet.shongo.controller.booking.reservation.ExecutableServiceReservation;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.Resource;

import javax.persistence.*;

/**
 * Represents a {@link ExecutableServiceReservation} for recording.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class RecordingServiceReservation extends ExecutableServiceReservation
{
    /**
     * {@link cz.cesnet.shongo.controller.booking.room.RoomProviderCapability} in which the room is allocated.
     */
    private RecordingCapability recordingCapability;

    /**
     * @return {@link #recordingCapability}
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
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

    @Override
    @Transient
    public Long getTargetId()
    {
        return recordingCapability.getId();
    }

    @Override
    @Transient
    public DeviceResource getAllocatedResource()
    {
        return recordingCapability.getDeviceResource();
    }
}
