package cz.cesnet.shongo.common;

/**
 * Represents an arbitrary Date/Time.
 *
 * @author Martin Srom
 */
public abstract class DateTime
{
    /**
     * Get the earliest Date/Time.
     *
     * @return absolute Date/Time
     */
    public abstract AbsoluteDateTime getDateTime();


    public boolean willOccur()
    {
        AbsoluteDateTime dateTime = getDateTime();
        return dateTime.after(now());
    }

    /**
     * Get current Date/Time
     *
     * @return
     */
    public static AbsoluteDateTime now()
    {
        throw new RuntimeException("TODO: Implement AbsoluteDateTime.now ISO8601");
    }
}
