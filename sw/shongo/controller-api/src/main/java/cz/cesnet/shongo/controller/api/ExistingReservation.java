package cz.cesnet.shongo.controller.api;

/**
 * {@link Reservation} for existing {@link Reservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ExistingReservation extends Reservation
{
    /**
     * Identifier of the {@link Reservation}.
     */
    private Reservation reservation;

    /**
     * @return {@link #reservation}
     */
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
}
