package cz.cesnet.shongo.controller.booking.executable;

import cz.cesnet.shongo.controller.booking.specification.Specification;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;

/**
 * Interface which can be implemented by {@link Specification}s or by {@link Reservation}s and the object
 * which implements it provides an {@link Endpoint} (which then can be used e.g., in {@link cz.cesnet.shongo.controller.booking.compartment.Compartment}).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface EndpointProvider
{
    /**
     * @return {@link Endpoint} which is provided
     */
    public Endpoint getEndpoint();
}
