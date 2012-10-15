package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.controller.request.ReservationRequest;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;

/**
 * Represents a reuse of an existing {@link Reservation} to a new {@link Reservation} for
 * a different {@link ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ExistingReservation extends Reservation
{
    /**
     * Existing {@link Reservation}.
     */
    private Reservation reservation;

    /**
     * @return {@link #reservation}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public Reservation getReservation()
    {
        return reservation;
    }

    /**
     * @param reservation sets the {@link #reservation}
     */
    public void setReservation(Reservation reservation)
    {
        this.reservation = reservation;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Reservation createApi()
    {
        throw new RuntimeException("TODO: Implement ExistingReservation.createApi");
    }
}
