package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.StreamingCapability;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 * {@link EndpointService} for streaming.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class StreamingService extends EndpointService
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
