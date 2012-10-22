package cz.cesnet.shongo.controller.scheduler.report;

import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.util.TemporalHelper;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.Type;
import org.joda.time.Interval;

import javax.persistence.*;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getText()}
 */
@Entity
public class AllocatingPermanentReservationReport extends Report
{
    /**
     * Date/time Slot for the {@link Reservation}.
     */
    private Interval slot;

    /**
     * Constructor.
     */
    public AllocatingPermanentReservationReport()
    {
    }

    /**
     * Constructor.
     *
     * @param slot sets the {@link #slot}
     */
    public AllocatingPermanentReservationReport(Interval slot)
    {
        this.slot = slot;
    }

    /**
     * @return {@link #slot}
     */
    @Columns(columns = {@Column(name = "slotStart"), @Column(name = "slotEnd")})
    @Type(type = "Interval")
    @Access(AccessType.FIELD)
    public Interval getSlot()
    {
        return slot;
    }

    @Override
    @Transient
    public String getText()
    {
        return String.format("Allocating reservation for %s.", TemporalHelper.formatInterval(slot));
    }
}
