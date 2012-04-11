package cz.cesnet.shongo.common;

/**
 * Represents an absolute Date/Time.
 *
 * @author Martin Srom
 */
public class AbsoluteDateTime extends DateTime implements Comparable<AbsoluteDateTime>, Cloneable
{
    /**
     * Null field value.
     */
    public final int NullValue = Integer.MAX_VALUE;

    private final Date date;
    private final Time time;

    /**
     * Construct zero date/time.
     */
    public AbsoluteDateTime()
    {
        this(new Date(), new Time());
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
            date = new Date(dateTime);
            time = new Time();
        }
        else {
            date = new Date(dateTime.substring(0, index));
            time = new Time(dateTime.substring(index + 1, dateTime.length()));
        }
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
        this.date = date;
        this.time = time;
    }

    /**
     * Get date/time date.
     *
     * @return date
     */
    public Date getDate()
    {
        return date;
    }

    /**
     * Get date/time time.
     *
     * @return time.
     */
    public Time getTime()
    {
        return time;
    }

    /**
     * Get date year.
     *
     * @return year
     */
    public int getYear()
    {
        return getDate().getYear();
    }

    /**
     * Get date month.
     *
     * @return month
     */
    public int getMonth()
    {
        return getDate().getMonth();
    }

    /**
     * Get date day.
     *
     * @return day
     */
    public int getDay()
    {
        return getDate().getDay();
    }

    /**
     * Get time hour.
     *
     * @return hour
     */
    public int getHour()
    {
        return getTime().getHour();
    }

    /**
     * Get time minute.
     *
     * @return minute
     */
    public int getMinute()
    {
        return getTime().getMinute();
    }

    /**
     * Get time second.
     *
     * @return second
     */
    public int getSecond()
    {
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
        dateTime.append(getDate().toString());
        if (getTime().isEmpty() == false) {
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
        return getDate().equals(dateTime.getDate()) && getTime().equals(dateTime.getTime());
    }

    @Override
    public int hashCode()
    {
        int result = 19;
        result = 37 * result + date.hashCode();
        result = 37 * result + time.hashCode();
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

        int timeResult = getTime().compareTo(absoluteDateTime.getTime());
        if (timeResult != 0) {
            return timeResult;
        }

        return 0;
    }

    @Override
    public Object clone()
    {
        return new AbsoluteDateTime(date, time);
    }

    /**
     * Checks whether this date/time equals the given date/time by skipping
     * all empty fields (in this or given date/time).
     *
     * @param dateTime
     * @return true if this date/time matches the given date/time,
     *         false otherwise
     */
    public boolean match(AbsoluteDateTime dateTime)
    {
        if (this == dateTime) {
            return true;
        }
        if (dateTime == null) {
            return false;
        }
        return getDate().match(dateTime.getDate()) && getTime().match(dateTime.getTime());
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
        time = time.add(period.getHour(), period.getMinute(), period.getSecond());

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
        time = time.subtract(period.getHour(), period.getMinute(), period.getSecond());

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
        return new AbsoluteDateTime(date.merge(dateTime.getDate()), time.merge(dateTime.getTime()));
    }
}
