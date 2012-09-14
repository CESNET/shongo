package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.controller.compartment.Endpoint;
import cz.cesnet.shongo.fault.TodoImplementException;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Represents a {@link Reservation} for a {@link Endpoint}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class EndpointReservation extends Reservation
{
    /**
     * @return allocated {@link Endpoint} by the {@link EndpointReservation}
     */
    @Transient
    public Endpoint getEndpoint()
    {
        throw new TodoImplementException();
    }
}
