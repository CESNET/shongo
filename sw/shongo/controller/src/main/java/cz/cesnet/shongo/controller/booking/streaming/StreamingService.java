package cz.cesnet.shongo.controller.booking.streaming;

import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
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

    @Override
    protected cz.cesnet.shongo.controller.api.ExecutableService createApi()
    {
        return new cz.cesnet.shongo.controller.api.StreamingService();
    }

    @Override
    public void toApi(cz.cesnet.shongo.controller.api.ExecutableService executableServiceApi)
    {
        super.toApi(executableServiceApi);

        cz.cesnet.shongo.controller.api.StreamingService streamingServiceApi =
                (cz.cesnet.shongo.controller.api.StreamingService) executableServiceApi;

        streamingServiceApi.setResourceId(ObjectIdentifier.formatId(streamingCapability.getResource()));
    }
}
