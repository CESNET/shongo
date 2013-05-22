package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.controller.reservation.Reservation;

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
        else if (reservations.size() == 0) {
            return reservations.get(0);
        }
        else {
            throw new TodoImplementException();
        }
    }

    /**
     * @param reservations sets the {@link #reservations}
     */
    public void setReservations(List<Reservation> reservations)
    {
        this.reservations = reservations;
    }

    /**
     * @param reservation to be added to the {@link #reservations}
     */
    public void addReservation(Reservation reservation)
    {
        // Manage bidirectional association
        if (reservations.contains(reservation) == false) {
            if (reservations.size() > 0) {
                throw new TodoImplementException();
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
