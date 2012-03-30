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
    public abstract AbsoluteDateTime getEarliest();

    /**
     * Is Date/Time take place in future
     *
     * @return boolean
     */
    public boolean willOccur()
    {
        AbsoluteDateTime dateTime = getEarliest();
        return dateTime.after(now());
    }

    /**
     * Get current Date/Time
     *
     * @return current Date/Time
     */
    public static AbsoluteDateTime now()
    {
        throw new RuntimeException("TODO: Implement AbsoluteDateTime.now ISO8601");
    }
}
