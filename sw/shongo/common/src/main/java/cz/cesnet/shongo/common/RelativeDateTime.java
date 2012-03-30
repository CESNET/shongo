package cz.cesnet.shongo.common;

/**
 * Represents an relative Date/Time from now.
 *
 * @author Martin Srom
 */
public class RelativeDateTime extends DateTime
{
    /**
     * Get the earliest Date/Time.
     *
     * @return absolute Date/Time
     */
    @Override
    public AbsoluteDateTime getDateTime()
    {
        throw new RuntimeException("TODO: Implement RelativeDateTime.getDateTime");
    }
}
