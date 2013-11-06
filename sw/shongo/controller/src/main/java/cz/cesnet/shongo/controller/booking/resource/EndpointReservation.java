package cz.cesnet.shongo.controller.booking.resource;

import cz.cesnet.shongo.controller.booking.executable.Endpoint;
import cz.cesnet.shongo.controller.booking.executable.EndpointProvider;
import cz.cesnet.shongo.controller.booking.executable.ResourceEndpoint;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Represents a {@link cz.cesnet.shongo.controller.booking.reservation.Reservation} for a {@link Endpoint}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class EndpointReservation extends ResourceReservation implements EndpointProvider
{
    /**
     * Constructor.
     */
    public EndpointReservation()
    {
    }

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
