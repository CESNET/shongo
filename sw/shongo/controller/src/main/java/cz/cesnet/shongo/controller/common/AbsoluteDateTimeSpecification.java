package cz.cesnet.shongo.controller.common;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * Represents an absolute Date/Time.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AbsoluteDateTimeSpecification extends DateTimeSpecification
        implements Comparable<AbsoluteDateTimeSpecification>, Cloneable
{
    /**
     * Date/time that holds absolute date/time.
     */
    protected DateTime dateTime;

    /**
     * Constructor.
     */
    private AbsoluteDateTimeSpecification()
    {
    }

    /**
     * Construct date/time from an ISO8601 string, e.g. "2007-04-05T14:30:00".
     *
     * @param dateTime ISO8601 Date/Time;
     */
    public AbsoluteDateTimeSpecification(String dateTime)
    {
        this.dateTime = DateTime.parse(dateTime);
    }

    /**
     * Construct date/time from an date object. Date object became part
     * of date/time so when date is modified date become modified too.
     *
     * @param dateTime Date/time object
     */
    public AbsoluteDateTimeSpecification(DateTime dateTime)
    {
        if (dateTime == null) {
            throw new IllegalArgumentException("Given date/time must not be null!");
        }
        this.dateTime = dateTime;
    }

    /**
     * @return {@link #dateTime}
     */
    @Column
    @Type(type = "DateTime")
    public DateTime getDateTime()
    {
        return dateTime;
    }

    /**
     * @param dateTime sets the {@link #dateTime}
     */
    protected void setDateTime(DateTime dateTime)
    {
        this.dateTime = dateTime;
    }

    /**
     * Get Date/Time as ISO8601 string.
     *
     * @return string of ISO8601 Date/Time
     */
    public String toString()
    {
        return dateTime.toString();
    }

    @Override
    public DateTime getEarliest(DateTime referenceDateTime)
    {
        if (dateTime.isAfter(referenceDateTime)) {
            return dateTime;
        }
        return null;
    }

    @Override
    public boolean equals(Object object)
    {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        AbsoluteDateTimeSpecification dateTime = (AbsoluteDateTimeSpecification) object;
        return getDateTime().equals(dateTime.getDateTime());
    }

    @Override
    public int hashCode()
    {
        return getDateTime().hashCode();
    }

    @Override
    public int compareTo(AbsoluteDateTimeSpecification absoluteDateTime)
    {
        if (this == absoluteDateTime) {
            return 0;
        }
        return getDateTime().compareTo(absoluteDateTime.getDateTime());
    }

    @Override
    public AbsoluteDateTimeSpecification clone()
    {
        return new AbsoluteDateTimeSpecification(getDateTime());
    }

    /**
     * Is this Date/Time before a given one?
     *
     * @param dateTime
     * @return boolean
     */
    public boolean before(AbsoluteDateTimeSpecification dateTime)
    {
        return compareTo(dateTime) < 0;
    }

    /**
     * Is this Date/Time before a given one or equal to it?
     *
     * @param dateTime
     * @return boolean
     */
    public boolean beforeOrEqual(AbsoluteDateTimeSpecification dateTime)
    {
        return compareTo(dateTime) <= 0;
    }

    /**
     * Is this Date/Time after a given one?
     *
     * @param dateTime
     * @return boolean
     */
    public boolean after(AbsoluteDateTimeSpecification dateTime)
    {
        return compareTo(dateTime) > 0;
    }

    /**
     * Is this Date/Time after a given one or equal to it?
     *
     * @param dateTime
     * @return boolean
     */
    public boolean afterOrEqual(AbsoluteDateTimeSpecification dateTime)
    {
        return compareTo(dateTime) >= 0;
    }
}
