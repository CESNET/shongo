package cz.cesnet.shongo.controller.booking.streaming;

import cz.cesnet.shongo.controller.booking.resource.DeviceCapability;

import javax.persistence.Entity;
import javax.persistence.EntityManager;

/**
 * Capability tells that the {@link cz.cesnet.shongo.controller.booking.resource.DeviceResource} can stream a call.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class StreamingCapability extends DeviceCapability
{
    /**
     * Constructor.
     */
    public StreamingCapability()
    {
    }

    @Override
    public cz.cesnet.shongo.controller.api.Capability createApi()
    {
        return new cz.cesnet.shongo.controller.api.StreamingCapability();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.Capability api)
    {
        cz.cesnet.shongo.controller.api.StreamingCapability streamingCapabilityApi =
                (cz.cesnet.shongo.controller.api.StreamingCapability) api;
        super.toApi(api);
    }

    @Override
    public void fromApi(cz.cesnet.shongo.controller.api.Capability api, EntityManager entityManager)
    {
        super.fromApi(api, entityManager);

        cz.cesnet.shongo.controller.api.StreamingCapability streamingCapabilityApi =
                (cz.cesnet.shongo.controller.api.StreamingCapability) api;
    }
}
