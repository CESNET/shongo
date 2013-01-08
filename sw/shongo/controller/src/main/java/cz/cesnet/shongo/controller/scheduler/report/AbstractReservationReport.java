package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.reservation.AliasReservation;
import cz.cesnet.shongo.controller.reservation.Reservation;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public abstract class AbstractReservationReport extends Report
{
    /**
     * @see Reservation
     */
    private Reservation reservation;

    /**
     * Constructor.
     */
    public AbstractReservationReport()
    {
    }

    /**
     * @return {@link #reservation}
     */
    @OneToOne
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

    /**
     * @return string description of reservation
     */
    @Transient
    public String getReservationDescription()
    {
        String reservationDescription;
        if ( reservation instanceof AliasReservation) {
            AliasReservation aliasReservation = (AliasReservation) reservation;
            reservationDescription = String.format("Alias reservation '%d'", aliasReservation.getId());
        }
        else {
            reservationDescription = reservation.getClass().getSimpleName();
        }
        return reservationDescription;
    }
}
