package cz.cesnet.shongo.common;

/**
 * Represents an arbitrary Date/Time.
 *
 * @author Martin Srom
 */
public abstract class DateTime
{
    /**
     * Get the earliest Date/Time since a given datetime (strict inequality).
     *
     * @param referenceDateTime    the datetime since which to find the earliest occurrence
     * @return absolute Date/Time, or <code>null</code> if the datetime won't take place since referenceDateTime
     */
    public abstract AbsoluteDateTime getEarliest(AbsoluteDateTime referenceDateTime);

    /**
     * Get the earliest Date/Time from now.
     *
     * @return absolute Date/Time, or <code>null</code> if the datetime won't take place
     */
    final public AbsoluteDateTime getEarliest()
    {
        return getEarliest(now());
    }

    /**
     * Will the datetime take place in future (strict inequality)?
     *
     * @return boolean
     */
    final public boolean willOccur()
    {
        AbsoluteDateTime dateTime = getEarliest();
        return dateTime.after(now());
    }

    /**
     * Checks whether this datetime will take place since a given absolute datetime (strict inequality).
     *
     * @param referenceDateTime    the datetime take as "now" for evaluating future
     * @return true if this datetime will take place at least once after or in referenceDateTime,
     *         false if not
     */
    public boolean willOccur(AbsoluteDateTime referenceDateTime)
    {
        throw new RuntimeException("TODO: implement willOccur");
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
