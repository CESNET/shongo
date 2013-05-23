package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.reservation.Reservation;
import org.joda.time.Interval;

import javax.persistence.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents an allocation of reservations for {@link #reservationRequest}.
 * {@link Reservation}s must not intersect in time slots.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class Allocation extends PersistentObject
{
    /**
     * Current {@link AbstractReservationRequest} which is/should be allocated.
     */
    private AbstractReservationRequest reservationRequest;

    /**
     * Collection of allocated {@link Reservation}s.
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
     * @return {@link #reservations}
     */
    @OneToMany
    @Access(AccessType.FIELD)
    public List<Reservation>    getReservations()
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
}
