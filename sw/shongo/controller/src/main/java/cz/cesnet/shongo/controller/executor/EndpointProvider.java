package cz.cesnet.shongo.controller.executor;

import cz.cesnet.shongo.controller.request.Specification;
import cz.cesnet.shongo.controller.reservation.Reservation;

/**
 * Interface which can be implemented by {@link Specification}s or by {@link Reservation}s and the object
 * which implements it provides an {@link Endpoint} (which then can be used e.g., in {@link Compartment}).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface EndpointProvider
{
    public Endpoint getEndpoint();
}
