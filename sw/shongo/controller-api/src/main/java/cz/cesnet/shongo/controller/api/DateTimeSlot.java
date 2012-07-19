package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.AllowedTypes;
import org.joda.time.DateTime;
import org.joda.time.Period;

/**
 * Represents a time slot defined by a starting moment and a duration.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DateTimeSlot extends ComplexType
{
    /**
     * Starting date/time.
     */
    private Object start;

    /**
     * Duration of the time slot.
     */
    private Period duration;

    /**
     * Constructor.
     */
    public DateTimeSlot()
    {
    }

    /**
     * Constructor.
     *
     * @param start    sets the {@link #start}
     * @param duration sets the {@link #duration}
     */
    public DateTimeSlot(Object start, Period duration)
    {
        setStart(start);
        setDuration(duration);
    }

    /**
     * @return {@link #start}
     */
    @AllowedTypes({DateTime.class, PeriodicDateTime.class})
    public Object getStart()
    {
        return start;
    }

    /**
     * @param start sets the {@link #start}
     */
    public void setStart(Object start)
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
}
