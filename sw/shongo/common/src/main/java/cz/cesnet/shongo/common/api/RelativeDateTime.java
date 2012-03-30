package cz.cesnet.shongo.common.api;

/**
 * Represents a relative Date/Time
 *
 * @author Martin Srom
 */
public class RelativeDateTime extends DateTime
{
    /**
     * Duration from current Date/Time
     */
    private Duration duration;

    public Duration getDuration()
    {
        return duration;
    }

    public void setDuration(Duration duration)
    {
        this.duration = duration;
    }
}
