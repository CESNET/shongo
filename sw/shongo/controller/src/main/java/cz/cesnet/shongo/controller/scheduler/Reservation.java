package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.common.PersistentObject;
import cz.cesnet.shongo.controller.reservation.ReservationRequest;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an allocation for a {@link ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class Reservation extends PersistentObject
{
    /**
     * {@link ReservationRequest} for which the reservation is allocated.
     */
    private ReservationRequest reservationRequest;

    /**
     * List of {@link AllocatedCompartment}s that are allocated for the reservation request.
     */
    private List<AllocatedCompartment> allocatedCompartments = new ArrayList<AllocatedCompartment>();

    /**
     * @return {@link #reservationRequest}
     */
    @OneToOne
    public ReservationRequest getReservationRequest()
    {
        return reservationRequest;
    }

    /**
     * @param reservationRequest sets the {@link #reservationRequest}
     */
    private void setReservationRequest(ReservationRequest reservationRequest)
    {
        this.reservationRequest = reservationRequest;
    }

    /**
     * @return {@link #allocatedCompartments}
     */
    @OneToMany(cascade = CascadeType.ALL,mappedBy = "reservation")
    public List<AllocatedCompartment> getAllocatedCompartments()
    {
        return allocatedCompartments;
    }

    /**
     * @param allocatedCompartments sets the {@link #allocatedCompartments}
     */
    private void setAllocatedCompartments(List<AllocatedCompartment> allocatedCompartments)
    {
        this.allocatedCompartments = allocatedCompartments;
    }

    /**
     * @param allocatedCompartment allocated compartment to be added to the {@link #allocatedCompartments}
     */
    public void addAllocatedCompartment(AllocatedCompartment allocatedCompartment)
    {
        this.allocatedCompartments.add(allocatedCompartment);
    }
}
