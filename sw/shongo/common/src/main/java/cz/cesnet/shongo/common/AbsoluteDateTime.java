package cz.cesnet.shongo.common;

import javax.persistence.*;

/**
 * Represents an absolute Date/Time.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AbsoluteDateTime extends DateTime implements Comparable<AbsoluteDateTime>, Cloneable
{
    /**
     * Date of absolute date/time.
     */
    protected Date date;

    /**
     * Time of absolute date/time.
     */
    protected Time time;

    /**
     * Constructor
     */
    protected AbsoluteDateTime()
    {
    }

    /**
     * Construct date/time from an ISO8601 string, e.g. "2007-04-05T14:30:00".
     *
     * @param dateTime ISO8601 Date/Time;
     */
    public AbsoluteDateTime(String dateTime)
    {
        int index = dateTime.indexOf("T");
        if (index == -1) {
            setDate(dateTime);
        }
        else {
            setDate(dateTime.substring(0, index));
            setTime(dateTime.substring(index + 1, dateTime.length()));
        }
    }

    /**
     * Construct date/time from an date object. Date object became part
     * of date/time so when date is modified date become modified too.
     *
     * @param date Date object
     */
    public AbsoluteDateTime(Date date)
    {
        setDate(date);
    }

    /**
     * Construct date/time from an date and time objects. These objects became part
     * of date/time so when date/time is modified date and time objects become
     * modified too.
     *
     * @param date Date object
     * @param time Time object
     */
    public AbsoluteDateTime(Date date, Time time)
    {
        setDate(date);
        setTime(time);
    }

    /**
     * Get date/time date.
     *
     * @return date
     */
    @Column
    @Access(AccessType.FIELD)
    public Date getDate()
    {
        return date;
    }

    /**
     * @param date sets the {@link #date}
     */
    protected void setDate(Date date)
    {
        this.date = date;
    }

    /**
     * @param date sets the {@link #date} from string
     */
    protected void setDate(String date)
    {
        this.date = new Date(date);
    }

    /**
     * @return true if {@link #date} is not null, otherwise false
     */
    public boolean hasDate()
    {
        return date != null;
    }

    /**
     * Get date/time time.
     *
     * @return time.
     */
    @Column
    @Access(AccessType.FIELD)
    public Time getTime()
    {
        return time;
    }

    /**
     * @param time sets the {@link #time}
     */
    protected void setTime(Time time)
    {
        this.time = time;
    }

    /**
     * @param time sets the {@link #time} from string
     */
    protected void setTime(String time)
    {
        this.time = new Time(time);
    }

    /**
     * @return true if {@link #time} is not null, otherwise false
     */
    public boolean hasTime()
    {
        return time != null;
    }

    /**
     * Get date year.
     *
     * @return year
     */
    @Transient
    public int getYear()
    {
        return getDate().getYear();
    }

    /**
     * Get date month.
     *
     * @return month
     */
    @Transient
    public int getMonth()
    {
        return getDate().getMonth();
    }

    /**
     * Get date day.
     *
     * @return day
     */
    @Transient
    public int getDay()
    {
        return getDate().getDay();
    }

    /**
     * Get time hour.
     *
     * @return hour
     */
    @Transient
    public int getHour()
    {
        if (hasTime() == false) {
            throw new IllegalStateException("Time is not set!");
        }
        return getTime().getHour();
    }

    /**
     * Get time minute.
     *
     * @return minute
     */
    @Transient
    public int getMinute()
    {
        if (hasTime() == false) {
            throw new IllegalStateException("Time is not set!");
        }
        return getTime().getMinute();
    }

    /**
     * Get time second.
     *
     * @return second
     */
    @Transient
    public int getSecond()
    {
        if (hasTime() == false) {
            throw new IllegalStateException("Time is not set!");
        }
        return getTime().getSecond();
    }

    /**
     * Get Date/Time as ISO8601 string.
     *
     * @return string of ISO8601 Date/Time
     */
    public String toString()
    {
        StringBuilder dateTime = new StringBuilder();
        if (hasDate()) {
            dateTime.append(getDate().toString());
        }
        if (hasTime()) {
            dateTime.append("T");
            dateTime.append(getTime().toString());
        }
        return dateTime.toString();
    }

    @Override
    public AbsoluteDateTime getEarliest(AbsoluteDateTime referenceDateTime)
    {
        if (after(referenceDateTime)) {
            return this;
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
        AbsoluteDateTime dateTime = (AbsoluteDateTime) object;

        if (getDate().equals(dateTime.getDate()) == false) {
            return false;
        }
        if (hasTime() == false && dateTime.hasTime() == false) {
            return true;
        }
        if ((hasTime() == false && dateTime.hasTime()) || (hasTime() && dateTime.hasTime() == false)) {
            return false;
        }
        return getTime().equals(dateTime.getTime());
    }

    @Override
    public int hashCode()
    {
        int result = 19;
        result = 37 * result + getDate().hashCode();
        result = 37 * result + getTime().hashCode();
        return result;
    }

    @Override
    public int compareTo(AbsoluteDateTime absoluteDateTime)
    {
        if (this == absoluteDateTime) {
            return 0;
        }

        int dateResult = getDate().compareTo(absoluteDateTime.getDate());
        if (dateResult != 0) {
            return dateResult;
        }

        if (hasTime() == false || absoluteDateTime.hasTime() == false) {
            return 0;
        }

        int timeResult = getTime().compareTo(absoluteDateTime.getTime());
        if (timeResult != 0) {
            return timeResult;
        }

        return 0;
    }

    @Override
    public AbsoluteDateTime clone()
    {
        return new AbsoluteDateTime(date, time);
    }

    /**
     * Is this Date/Time before a given one?
     *
     * @param dateTime
     * @return boolean
     */
    public boolean before(AbsoluteDateTime dateTime)
    {
        return compareTo(dateTime) < 0;
    }

    /**
     * Is this Date/Time before a given one or equal to it?
     *
     * @param dateTime
     * @return boolean
     */
    public boolean beforeOrEqual(AbsoluteDateTime dateTime)
    {
        return compareTo(dateTime) <= 0;
    }

    /**
     * Is this Date/Time after a given one?
     *
     * @param dateTime
     * @return boolean
     */
    public boolean after(AbsoluteDateTime dateTime)
    {
        return compareTo(dateTime) > 0;
    }

    /**
     * Is this Date/Time after a given one or equal to it?
     *
     * @param dateTime
     * @return boolean
     */
    public boolean afterOrEqual(AbsoluteDateTime dateTime)
    {
        return compareTo(dateTime) >= 0;
    }

    /**
     * Adds a time period to this datetime and returns the result.
     * <p/>
     * This datetime object is not modified.
     *
     * @param period time period to add
     * @return resulting datetime
     */
    public AbsoluteDateTime add(Period period)
    {
        period.normalize();

        Time time = getTime();
        if (time != null) {
            time = time.add(period.getHour(), period.getMinute(), period.getSecond());
        }

        Date date = getDate();
        date = date.add(period.getYear(), period.getMonth(), period.getDay() + time.getOverflow());

        return new AbsoluteDateTime(date, time);
    }

    /**
     * Subtracts a time period from this datetime and returns the result.
     * <p/>
     * This datetime object is not modified.
     *
     * @param period time period to subtract
     * @return resulting datetime
     */
    public AbsoluteDateTime subtract(Period period)
    {
        period.normalize();

        Time time = getTime();
        if (time != null) {
            time = time.subtract(period.getHour(), period.getMinute(), period.getSecond());
        }

        Date date = getDate();
        date = date.subtract(period.getYear(), period.getMonth(), period.getDay() + time.getUnderflow());

        return new AbsoluteDateTime(date, time);
    }

    /**
     * Merge this date/time with given date/time and return
     * result. This and given date/time stay unchanged.
     * <p/>
     * The returned date/time contains values from this date/time
     * replaced by non-empty values from given date/time.
     *
     * @param dateTime Date/time to merge
     */
    public AbsoluteDateTime merge(AbsoluteDateTime dateTime)
    {
        return new AbsoluteDateTime(dateTime.hasDate() ? dateTime.getDate() : getDate(),
                dateTime.hasTime() ? dateTime.getTime() : getTime());
    }
}
