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

    AbsoluteDateTime[] enumerateDateTimes()
    {
        return enumerateDateTimes(null, null);
    }

    AbsoluteDateTime[] enumerateDateTimes(AbsoluteDateTime from, AbsoluteDateTime to)
    {
        throw new RuntimeException("TODO: Implement PeriodicDateTime.enumerateDateTimes");
    }

    @Override
    public AbsoluteDateTime getDateTime()
    {
        throw new RuntimeException("TODO: Implement PeriodicDateTime.getDateTime");
    }
}
