package cz.cesnet.shongo.controller.request;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.*;

/**
 * Represents a interval (time slot).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Embeddable
public class Slot
{
    /**
     * Interval start date/time.
     */
    private DateTime start;

    /**
     * Interval end date/time.
     */
    private DateTime end;

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
    @Access(AccessType.PROPERTY)
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
    @Access(AccessType.PROPERTY)
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

    @Override
    public String toString()
    {
        return getInterval().toString();
    }
}
