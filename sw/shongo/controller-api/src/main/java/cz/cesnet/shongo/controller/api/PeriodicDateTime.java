package cz.cesnet.shongo.controller.api;

import org.joda.time.DateTime;
import org.joda.time.Period;

/**
 * Represents a definition for multiple date/times by period.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PeriodicDateTime extends ComplexType
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
}
