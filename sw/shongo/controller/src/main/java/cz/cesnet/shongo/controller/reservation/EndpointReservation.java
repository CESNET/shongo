package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.controller.executor.Endpoint;
import cz.cesnet.shongo.controller.executor.EndpointProvider;
import cz.cesnet.shongo.controller.executor.ResourceEndpoint;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.Resource;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Represents a {@link Reservation} for a {@link Endpoint}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class EndpointReservation extends ResourceReservation implements EndpointProvider
{
    /**
     * @return {@link #resource} as {@link DeviceResource}
     */
    @Transient
    public DeviceResource getDeviceResource()
    {
        return (DeviceResource) getResource();
    }

    @Override
    public void setResource(Resource resource)
    {
        if (!(resource instanceof DeviceResource)) {
            throw new IllegalArgumentException("Resource must be device to be endpoint.");
        }
        super.setResource(resource);
    }

    /**
     * @return allocated {@link Endpoint} by the {@link EndpointReservation}
     */
    @Transient
    public Endpoint getEndpoint()
    {
        return new ResourceEndpoint(getDeviceResource());
    }
}
