package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.PersistentObject;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

/**
 * Represents a {@link ReservationRequestSet.State#PREPROCESSED} state of a reservation request for a specific interval.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ReservationRequestSetPreprocessedState extends PersistentObject
{
    /**
     * Reservation request to which the state belongs.
     */
    private ReservationRequestSet reservationRequestSet;

    /**
     * Interval start date/time.
     */
    private DateTime start;

    /**
     * Interval end date/time.
     */
    private DateTime end;

    /**
     * @return {@link #reservationRequestSet}
     */
    @OneToOne
    public ReservationRequestSet getReservationRequestSet()
    {
        return reservationRequestSet;
    }

    /**
     * @param reservationRequestSet sets the {@link #reservationRequestSet}
     */
    public void setReservationRequestSet(ReservationRequestSet reservationRequestSet)
    {
        this.reservationRequestSet = reservationRequestSet;
    }

    /**
     * @return interval ({@link #start}, {@link #end})
     */
    @Transient
    public Interval getInterval()
    {
        return new Interval(start, end);
    }

    /**
     * @param interval sets the interval ({@link #start}, {@link #end})
     */
    public void setInterval(Interval interval)
    {
        setStart(interval.getStart());
        setEnd(interval.getEnd());
    }

    /**
     * @return {@link #start}
     */
    @Column(name = "interval_start")
    @Type(type = "DateTime")
    public DateTime getStart()
    {
        return start;
    }

    /**
     * @param start sets the {@link #start}
     */
    public void setStart(DateTime start)
    {
        this.start = start;
    }

    /**
     * @return {@link #end}
     */
    @Column(name = "interval_end")
    @Type(type = "DateTime")
    public DateTime getEnd()
    {
        return end;
    }

    /**
     * @param end sets the {@link #end}
     */
    public void setEnd(DateTime end)
    {
        this.end = end;
    }
}
