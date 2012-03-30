package cz.cesnet.shongo.common;

/**
 * Represents a Date/Time that takes place periodically.
 *
 * @author Martin Srom
 */
public class PeriodicDateTime extends DateTime
{
    // TODO: add attributes

    // TODO: getters and setters for attributes

    /**
     * Enumerate all periodic Date/Time events to array of absolute Date/Times.
     *
     * @return array of absolute Date/Times
     */
    AbsoluteDateTime[] enumerate()
    {
        return enumerate(null, null);
    }

    /**
     * Enumerate all periodic Date/Time events to array of absolute Date/Times.
     * Return only events that take place inside interval defined by from - to.
     *
     * @param from
     * @param to
     * @return array of absolute Date/Times
     */
    AbsoluteDateTime[] enumerate(AbsoluteDateTime from, AbsoluteDateTime to)
    {
        throw new RuntimeException("TODO: Implement PeriodicDateTime.enumerate");
    }

    @Override
    public AbsoluteDateTime getEarliest()
    {
        throw new RuntimeException("TODO: Implement PeriodicDateTime.getDateTime");
    }
}
