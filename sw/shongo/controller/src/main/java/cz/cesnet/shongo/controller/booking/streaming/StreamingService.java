package cz.cesnet.shongo.controller.booking.streaming;

import cz.cesnet.shongo.controller.booking.executable.ExecutableService;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 * {@link cz.cesnet.shongo.controller.booking.executable.ExecutableService} for streaming.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class StreamingService extends ExecutableService
{
    /**
     * {@link StreamingCapability} of {@link DeviceResource} which is used for streaming.
     */
    private StreamingCapability streamingCapability;

    /**
     * @return {@link #streamingCapability}
     */
    @ManyToOne
    @Access(AccessType.FIELD)
    public StreamingCapability getStreamingCapability()
    {
        return streamingCapability;
    }

    /**
     * @param streamingCapability sets the {@link #streamingCapability}
     */
    public void setStreamingCapability(StreamingCapability streamingCapability)
    {
        this.streamingCapability = streamingCapability;
    }
}
