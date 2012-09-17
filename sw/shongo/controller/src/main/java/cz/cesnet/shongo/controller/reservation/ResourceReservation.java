package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.controller.compartment.Endpoint;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.fault.TodoImplementException;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

/**
 * Represents a {@link cz.cesnet.shongo.controller.reservation.Reservation} for a {@link cz.cesnet.shongo.controller.compartment.Endpoint}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ResourceReservation extends Reservation
{
    private Resource resource;

    /**
     * @return {@link #resource}
     */
    @OneToOne
    public Resource getResource()
    {
        return resource;
    }

    /**
     * @param resource sets the {@link #resource}
     */
    public void setResource(Resource resource)
    {
        this.resource = resource;
    }
}
