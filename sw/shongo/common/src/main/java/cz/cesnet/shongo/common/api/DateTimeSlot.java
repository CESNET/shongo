package cz.cesnet.shongo.common.api;

import cz.cesnet.shongo.common.xmlrpc.StructType;

/**
 * Represents a date/time slot
 *
 * @author Martin Srom
 */
public class DateTimeSlot implements StructType
{
    /**
     * Starting Date/Time
     */
    private DateTime start;

    /**
     * Period
     */
    private Period duration;

    public DateTime getStart()
    {
        return start;
    }

    public void setStart(DateTime start)
    {
        this.start = start;
    }

    public Period getDuration()
    {
        return duration;
    }

    public void setDuration(Period duration)
    {
        this.duration = duration;
    }
}
