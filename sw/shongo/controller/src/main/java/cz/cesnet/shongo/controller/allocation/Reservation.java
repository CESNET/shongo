package cz.cesnet.shongo.controller.allocation;

import cz.cesnet.shongo.common.PersistentObject;
import cz.cesnet.shongo.controller.request.ReservationRequest;

import javax.persistence.*;
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
    @Access(AccessType.FIELD)
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
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "reservation")
    @Access(AccessType.FIELD)
    public List<AllocatedCompartment> getAllocatedCompartments()
    {
        return allocatedCompartments;
    }

    /**
     * @param allocatedCompartment allocated compartment to be added to the {@link #allocatedCompartments}
     */
    public void addAllocatedCompartment(AllocatedCompartment allocatedCompartment)
    {
        // Manage bidirectional association
        if (allocatedCompartments.contains(allocatedCompartment) == false) {
            allocatedCompartments.add(allocatedCompartment);
            allocatedCompartment.setReservation(this);
        }
    }

    /**
     * @param allocatedCompartment allocated compartment to be removed from the {@link #allocatedCompartments}
     */
    public void removeAllocatedCompartment(AllocatedCompartment allocatedCompartment)
    {
        // Manage bidirectional association
        if (allocatedCompartments.contains(allocatedCompartment)) {
            allocatedCompartments.remove(allocatedCompartment);
            allocatedCompartment.setReservation(null);
        }
    }
}
