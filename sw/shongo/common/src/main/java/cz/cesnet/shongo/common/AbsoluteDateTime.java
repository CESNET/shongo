package cz.cesnet.shongo.common;

/**
 * Represents an absolute Date/Time.
 *
 * @author Martin Srom
 */
public class AbsoluteDateTime extends DateTime
{
    /**
     * Get the earliest Date/Time.
     *
     * @return absolute Date/Time
     */
    @Override
    public AbsoluteDateTime getDateTime()
    {
        return this;
    }
}
