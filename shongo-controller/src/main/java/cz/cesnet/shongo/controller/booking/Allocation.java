package cz.cesnet.shongo.controller.booking;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestSet;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import org.joda.time.Interval;

import javax.persistence.*;
import java.util.*;

/**
 * Represents child {@link ReservationRequest}s and allocated reservations for the {@link #reservationRequest}.
 * <p/>
 * The {@link Allocation} instance is shared by original {@link AbstractReservationRequest} and all it's modified
 * versions.
 * <p/>
 * {@link #reservations}s must not intersect in theirs time slots.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class Allocation extends SimplePersistentObject
{
    /**
     * @see State
     */
    private State state;

    /**
     * Has notifications been sent
     */
    private boolean notified;

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
     * @return {@link #state}
     */
    @Column(length = AbstractComplexType.ENUM_COLUMN_LENGTH)
    @Enumerated(EnumType.STRING)
    public State getState()
    {
        return state;
    }

    /**
     * @param state sets the {@link #state}
     */
    public void setState(State state)
    {
        this.state = state;
    }

    @Column(nullable = false, columnDefinition = "boolean default false")
    public boolean isNotified() {
        return notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }

    /**
     * @return {@link #reservationRequest}
     */
    @OneToOne(fetch = FetchType.LAZY)
    public AbstractReservationRequest getReservationRequest()
    {
        return getLazyImplementation(reservationRequest);
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

    public void setChildReservationRequests(List<ReservationRequest> childReservationRequests)
    {
        this.childReservationRequests = childReservationRequests;
    }

    /**
     * @param childReservationRequest to be added to the {@link #childReservationRequests}
     */
    public void addChildReservationRequest(ReservationRequest childReservationRequest)
    {
        // Manage bidirectional association
        if (!childReservationRequests.contains(childReservationRequest)) {
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
    @OrderBy("slotStart")
    @Access(AccessType.FIELD)
    public List<Reservation> getReservations()
    {
        return Collections.unmodifiableList(reservations);
    }

    public void setReservations(List<Reservation> reservations)
    {
        this.reservations = reservations;
    }

    /**
     * @return {@link #reservations} identifiers
     */
    @Transient
    public Set<Long> getReservationIds()
    {
        Set<Long> reservationIds = new TreeSet<Long>();
        for (Reservation reservation : reservations)
        {
            reservationIds.add(reservation.getId());
        }
        return reservationIds;
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
            return reservations.get(reservations.size() - 1);
        }
    }

    /**
     * @param reservation to be added to the {@link #reservations}
     */
    public void addReservation(Reservation reservation)
    {
        // Manage bidirectional association
        if (!reservations.contains(reservation)) {
            // Check reservations overlaps only for new reservations (not for deletion - other states)
            if (State.ACTIVE_WITHOUT_CHILD_RESERVATION_REQUESTS.equals(state)) {
                // Check if reservation doesn't collide with any old one
                Interval reservationSlot = reservation.getSlot();
                for (Reservation oldReservation : reservations) {
                    if (reservationSlot.overlaps(oldReservation.getSlot())) {
                        throw new IllegalStateException(
                                String.format("New reservation cannot be added to allocation"
                                                + " because it's time slot '%s' collides with '%s' from old reservation '%s'.",
                                        reservationSlot, oldReservation.getSlot(),
                                        ObjectIdentifier.formatId(oldReservation)));
                    }
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
    public void onUpdate()
    {
        if (state != State.DELETED) {
            if (reservationRequest == null) {
                throw new RuntimeException("A reservation request is not set for the allocation.");
            }
            if (reservationRequest instanceof ReservationRequest) {
                state = State.ACTIVE_WITHOUT_CHILD_RESERVATION_REQUESTS;
            }
            else if (reservationRequest instanceof ReservationRequestSet) {
                state = State.ACTIVE_WITHOUT_RESERVATIONS;
            }
            else {
                state = null;
            }
        }
    }

    /**
     * State of allocation.
     */
    public static enum State
    {
        /**
         * {@link Allocation} should not contain any {@link #childReservationRequests} (they should be deleted).
         */
        ACTIVE_WITHOUT_CHILD_RESERVATION_REQUESTS,

        /**
         * {@link Allocation} should not contain any {@link #reservations} (they should be deleted).
         */
        ACTIVE_WITHOUT_RESERVATIONS,

        /**
         * {@link Allocation}, all {@link #reservations} and all {@link #childReservationRequests} should be deleted.
         */
        DELETED
    }
}
