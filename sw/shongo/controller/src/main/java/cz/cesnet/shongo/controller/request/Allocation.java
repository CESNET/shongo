package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.ControllerReportSetHelper;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.reservation.Reservation;
import org.joda.time.Interval;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents child {@link ReservationRequest}s and allocated reservations for the {@link #reservationRequest}.
 *
 * The {@link Allocation} instance is shared by original {@link AbstractReservationRequest} and all it's modified
 * {@link AbstractReservationRequest}s.
 *
 * {@link Reservation}s must not intersect in theirs time slots.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class Allocation extends PersistentObject
{
    /**
     * Latest {@link AbstractReservationRequest} for which the {@link Allocation} exists.
     */
    private AbstractReservationRequest reservationRequest;

    /**
     * List of {@link ReservationRequest}s which are created for this {@link Allocation}.
     */
    private List<ReservationRequest> childReservationRequests = new ArrayList<ReservationRequest>();

    /**
     * Collection of {@link Reservation}s which are allocated for this {@link Allocation}.
     */
    private List<Reservation> reservations = new LinkedList<Reservation>();

    /**
     * @return {@link #reservationRequest}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public AbstractReservationRequest getReservationRequest()
    {
        return reservationRequest;
    }

    /**
     * @param reservationRequest sets the {@link #reservationRequest}
     */
    public void setReservationRequest(AbstractReservationRequest reservationRequest)
    {
        this.reservationRequest = reservationRequest;
    }

    /**
     * @return {@link #childReservationRequests}
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "parentAllocation")
    @Access(AccessType.FIELD)
    @OrderBy("slotStart")
    public List<ReservationRequest> getChildReservationRequests()
    {
        return Collections.unmodifiableList(childReservationRequests);
    }

    /**
     * @param id of the {@link ReservationRequest}
     * @return {@link ReservationRequest} with given {@code id}
     * @throws cz.cesnet.shongo.CommonReportSet.EntityNotFoundException when the {@link ReservationRequest} doesn't exist
     */
    @Transient
    private ReservationRequest getChildReservationRequestById(Long id) throws CommonReportSet.EntityNotFoundException
    {
        for (ReservationRequest reservationRequest : childReservationRequests) {
            if (reservationRequest.getId().equals(id)) {
                return reservationRequest;
            }
        }
        return ControllerReportSetHelper.throwEntityNotFoundFault(ReservationRequest.class, id);
    }

    /**
     * @param childReservationRequest to be added to the {@link #childReservationRequests}
     */
    public void addChildReservationRequest(ReservationRequest childReservationRequest)
    {
        // Manage bidirectional association
        if (childReservationRequests.contains(childReservationRequest) == false) {
            childReservationRequests.add(childReservationRequest);
            childReservationRequest.setParentAllocation(this);
        }
    }

    /**
     * @param reservationRequest to be removed from the {@link #childReservationRequests}
     */
    public void removeChildReservationRequest(ReservationRequest reservationRequest)
    {
        // Manage bidirectional association
        if (childReservationRequests.contains(reservationRequest)) {
            childReservationRequests.remove(reservationRequest);
            reservationRequest.setParentAllocation(null);
        }
    }

    /**
     * @return {@link #reservations}
     */
    @OneToMany
    @Access(AccessType.FIELD)
    public List<Reservation> getReservations()
    {
        return Collections.unmodifiableList(reservations);
    }

    /**
     * @return current {@link Reservation} from {@link #reservations}
     */
    @Transient
    public Reservation getCurrentReservation()
    {
        if (reservations.isEmpty()) {
            return null;
        }
        else if (reservations.size() == 1) {
            return reservations.get(0);
        }
        else {
            throw new TodoImplementException();
        }
    }

    /**
     * @param reservation to be added to the {@link #reservations}
     */
    public void addReservation(Reservation reservation)
    {
        // Manage bidirectional association
        if (reservations.contains(reservation) == false) {
            // Check if reservation doesn't collide with any old one
            Interval reservationSlot = reservation.getSlot();
            for (Reservation oldReservation : reservations) {
                if (reservationSlot.overlaps(oldReservation.getSlot())) {
                    throw new IllegalStateException(
                            String.format("New reservation cannot be added to allocation"
                                    + " because it's time slot '%s' collides with '%s' from old reservation '%s'.",
                                    reservationSlot, oldReservation.getSlot(),
                                    EntityIdentifier.formatId(oldReservation)));
                }
            }
            reservations.add(reservation);
            reservation.setAllocation(this);
        }
    }

    /**
     * @param reservation to be removed from the {@link #reservations}
     */
    public void removeReservation(Reservation reservation)
    {
        // Manage bidirectional association
        if (reservations.contains(reservation)) {
            reservations.remove(reservation);
            reservation.setAllocation(null);
        }
    }

    @PrePersist
    @PreUpdate
    public void validate()
    {
        if (reservationRequest == null) {
            throw new RuntimeException("A reservation request is not set for the allocation.");
        }
    }
}
