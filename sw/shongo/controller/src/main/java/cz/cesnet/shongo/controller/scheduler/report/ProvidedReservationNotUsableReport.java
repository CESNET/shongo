package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.reservation.Reservation;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class ProvidedReservationNotUsableReport extends AbstractReservationReport
{
    /**
     * Constructor.
     */
    public ProvidedReservationNotUsableReport()
    {
    }

    /**
     * Constructor.
     *
     * @param reservation
     */
    public ProvidedReservationNotUsableReport(Reservation reservation)
    {
        setReservation(reservation);
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("%s is not usable because provided date/time slot doesn't contain the requested.",
                getReservationDescription());
    }
}
