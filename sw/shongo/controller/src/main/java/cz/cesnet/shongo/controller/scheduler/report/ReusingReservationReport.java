package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.reservation.Reservation;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class ReusingReservationReport extends AbstractReservationReport
{
    /**
     * Constructor.
     */
    public ReusingReservationReport()
    {
    }

    /**
     * Constructor.
     *
     * @param reservation
     */
    public ReusingReservationReport(Reservation reservation)
    {
        setReservation(reservation);
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Reusing %s.", getReservationDescription());
    }
}
