package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.reservation.Reservation;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class ProvidedReservationNotAvailableReport extends AbstractReservationReport
{
    /**
     * Constructor.
     */
    public ProvidedReservationNotAvailableReport()
    {
    }

    /**
     * Constructor.
     *
     * @param reservation
     */
    public ProvidedReservationNotAvailableReport(Reservation reservation)
    {
        setReservation(reservation);
    }

    @Override
    @Transient
    public State getState()
    {
        return State.ERROR;
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("The %s is not available.", getReservationDescription());
    }
}
