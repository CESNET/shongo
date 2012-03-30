package cz.cesnet.shongo.common;

/**
 * Represents a Date/Time that takes place periodically.
 *
 * @author Martin Srom
 */
public class PeriodicDateTime extends DateTime
{
    /**
     * Get the earliest Date/Time.
     *
     * @return absolute Date/Time
     */
    @Override
    public AbsoluteDateTime getDateTime()
    {
        throw new RuntimeException("TODO: Implement PeriodicDateTime.getDateTime");
    }
}
