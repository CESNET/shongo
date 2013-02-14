package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.Required;
import cz.cesnet.shongo.api.util.Converter;
import cz.cesnet.shongo.api.util.IdentifiedObject;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.ReadablePartial;

/**
 * Represents a definition for periodic date/time slots.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PeriodicDateTimeSlot extends IdentifiedObject
{
    /**
     * Starting date/time for the first date/time slot.
     */
    private DateTime start;

    /**
     * Duration of each date/time slots.
     */
    private Period duration;

    /**
     * Period for multiple date/time slots.
     */
    private Period period;

    /**
     * Ending date and/or time after which the periodic events are not considered.
     */
    private ReadablePartial end;

    /**
     * Constructor.
     */
    public PeriodicDateTimeSlot()
    {
    }

    /**
     * Constructor.
     *
     * @param start    sets the {@link #start}
     * @param duration sets the {@link #duration}
     * @param period   sets the {@link #period}
     */
    public PeriodicDateTimeSlot(DateTime start, Period duration, Period period)
    {
        setStart(start);
        setPeriod(period);
        setDuration(duration);
    }

    /**
     * Constructor.
     *
     * @param start  sets the {@link #start}
     * @param period sets the {@link #period}
     * @param end    sets the {@link #end}
     */
    public PeriodicDateTimeSlot(DateTime start, Period duration, Period period, ReadablePartial end)
    {
        setStart(start);
        setDuration(duration);
        setPeriod(period);
        setEnd(end);
    }

    /**
     * Constructor.
     *
     * @param start    sets the {@link #start}
     * @param duration sets the {@link #duration}
     * @param period   sets the {@link #period}
     */
    public PeriodicDateTimeSlot(String start, String duration, String period)
    {
        setStart(DateTime.parse(start));
        setDuration(Period.parse(duration));
        setPeriod(Period.parse(period));
    }

    /**
     * Constructor.
     *
     * @param start    sets the {@link #start}
     * @param duration sets the {@link #duration}
     * @param period   sets the {@link #period}
     * @param end      sets the {@link #end}
     */
    public PeriodicDateTimeSlot(String start, String duration, String period, String end)
    {
        setStart(DateTime.parse(start));
        setDuration(Period.parse(duration));
        setPeriod(Period.parse(period));
        setEnd(Converter.Atomic.convertStringToReadablePartial(end));
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
     * @return {@link #duration}
     */
    public Period getDuration()
    {
        return duration;
    }

    /**
     * @param duration sets the {@link #duration}
     */
    public void setDuration(Period duration)
    {
        this.duration = duration;
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
