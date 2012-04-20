package cz.cesnet.shongo.common.api;

/**
 * Represents a relative Date/Time
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RelativeDateTime extends DateTime
{
    /**
     * Period from current Date/Time
     */
    private Period duration;

    public Period getDuration()
    {
        return duration;
    }

    public void setDuration(Period duration)
    {
        this.duration = duration;
    }
}
