package cz.cesnet.shongo.common;

import java.util.Calendar;

/**
 * Represents an absolute Date/Time.
 *
 * @author Martin Srom
 */
public class AbsoluteDateTime extends DateTime implements Comparable
{
    /** Internal Date/Time storage */
    private Calendar calendar;

    /**
     * Constructor
     *
     * @param dateTime ISO8601 Date/Time
     */
    public AbsoluteDateTime(String dateTime)
    {
        fromString(dateTime);
    }

    /**
     * Set Date/Time from ISO8601 string
     *
     * @param dateTime
     */
    public void fromString(String dateTime)
    {
        throw new RuntimeException("TODO: Implement AbsoluteDateTime.fromString ISO8601");
    }

    /**
     * Get Date/Time as ISO8601 string
     *
     * @return string of ISO8601 Date/Time
     */
    public String toString()
    {
        throw new RuntimeException("TODO: Implement AbsoluteDateTime.toString ISO8601");
    }
    
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

    @Override
    public int compareTo(Object o)
    {
        throw new RuntimeException("TODO: Implement AbsoluteDateTime.compareTo");
    }

    @Override
    public boolean equals(Object o)
    {
        throw new RuntimeException("TODO: Implement AbsoluteDateTime.equals");
    }

    /**
     * Is this Date/Time before given
     *
     * @param dateTime
     * @return boolean
     */
    public boolean before(AbsoluteDateTime dateTime)
    {
        return compareTo(dateTime) < 0;
    }

    /**
     * Is this Date/Time after given
     *
     * @param dateTime
     * @return boolean
     */
    public boolean after(AbsoluteDateTime dateTime)
    {
        return compareTo(dateTime) > 0;
    }

}
