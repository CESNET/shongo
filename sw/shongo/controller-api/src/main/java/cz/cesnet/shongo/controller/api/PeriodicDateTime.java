package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.Required;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.ReadablePartial;

/**
 * Represents a definition for multiple date/times by period.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PeriodicDateTime
{
    /**
     * First date/time.
     */
    private DateTime start;

    /**
     * Period for multiple date/times.
     */
    private Period period;

    /**
     * Ending date and/or time after which the periodic events are not considered.
     */
    private ReadablePartial end;

    /**
     * Constructor.
     */
    public PeriodicDateTime()
    {
    }

    /**
     * Constructor.
     *
     * @param start  sets the {@link #start}
     * @param period sets the {@link #period}
     */
    public PeriodicDateTime(DateTime start, Period period)
    {
        setStart(start);
        setPeriod(period);
    }

    /**
     * @return {@link #start}
     */
    @Required
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
     * @return {@link #period}
     */
    @Required
    public Period getPeriod()
    {
        return period;
    }

    /**
     * @param period sets the {@link #period}
     */
    public void setPeriod(Period period)
    {
        this.period = period;
    }

    /**
     * @return {@link #end}
     */
    public ReadablePartial getEnd()
    {
        return end;
    }

    /**
     * @param end sets the {@link #end}
     */
    public void setEnd(ReadablePartial end)
    {
        this.end = end;
    }
}
