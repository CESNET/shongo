package cz.cesnet.shongo.common.api;

/**
 * Represents a time slot
 *
 * @author Martin Srom
 */
public class TimeSlot
{
    /**
     * Starting Date/Time
     */
    private DateTime dateTime;

    /**
     * Duration
     */
    private Duration duration;

    public DateTime getDateTime()
    {
        return dateTime;
    }

    public void setDateTime(DateTime dateTime)
    {
        this.dateTime = dateTime;
    }

    public Duration getDuration()
    {
        return duration;
    }

    public void setDuration(Duration duration)
    {
        this.duration = duration;
    }
}
