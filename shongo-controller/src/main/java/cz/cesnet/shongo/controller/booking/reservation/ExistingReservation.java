package cz.cesnet.shongo.controller.booking.reservation;

import cz.cesnet.shongo.controller.booking.request.ReservationRequest;

import jakarta.persistence.*;

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
    private Reservation reusedReservation;

    /**
     * Constructor.
     */
    public ExistingReservation()
    {
    }

    /**
     * @return {@link #reusedReservation}
     */
    @ManyToOne(optional = false)
    @Access(AccessType.FIELD)
    public Reservation getReusedReservation()
    {
        return reusedReservation;
    }

    /**
     * @param reservation sets the {@link #reusedReservation}
     */
    public void setReusedReservation(Reservation reservation)
    {
        this.reusedReservation = reservation;
    }

    @Override
    @Transient
    public Reservation getAllocationReservation()
    {
        return reusedReservation;
    }

    @Override
    protected cz.cesnet.shongo.controller.api.Reservation createApi()
    {
        return new cz.cesnet.shongo.controller.api.ExistingReservation();
    }

    @Override
    protected void toApi(cz.cesnet.shongo.controller.api.Reservation api, EntityManager entityManager, boolean admin)
    {
        cz.cesnet.shongo.controller.api.ExistingReservation existingReservationApi =
                (cz.cesnet.shongo.controller.api.ExistingReservation) api;
        existingReservationApi.setReservation(getReusedReservation().toApi(entityManager, admin));
        super.toApi(api, entityManager, admin);
    }
}
