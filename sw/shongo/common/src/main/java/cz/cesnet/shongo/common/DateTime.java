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
}
