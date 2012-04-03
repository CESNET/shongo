package cz.cesnet.shongo.common;

/**
 * Represents an absolute Date/Time.
 *
 * @author Martin Srom
 */
public class AbsoluteDateTime extends DateTime implements Comparable<AbsoluteDateTime>
{
    /**
     * Null field value.
     */
    public final int NullValue = Integer.MAX_VALUE;

    private Date date;
    private Time time;

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
     * @param dateTime ISO8601 Date/Time; see {@link #fromString} for more info about supported input formats
     */
    public AbsoluteDateTime(String dateTime)
    {
        this(new Date(), new Time());

        fromString(dateTime);
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

    public Date getDate()
    {
        return date;
    }

    public void setDate(Date date)
    {
        this.date = date;
    }

    public Time getTime()
    {
        return time;
    }

    public void setTime(Time time)
    {
        this.time = time;
    }

    public int getYear()
    {
        return getDate().getYear();
    }

    public void setYear(int year)
    {
        getDate().setYear(year);
    }

    public int getMonth()
    {
        return getDate().getMonth();
    }

    public void setMonth(int month)
    {
        getDate().setMonth(month);
    }

    public int getDay()
    {
        return getDate().getDay();
    }

    public void setDay(int day)
    {
        getDate().setDay(day);
    }

    public int getHour()
    {
        return getTime().getHour();
    }

    public void setHour(int hour)
    {
        getTime().setHour(hour);
    }

    public int getMinute()
    {
        return getTime().getMinute();
    }

    public void setMinute(int minute)
    {
        getTime().setMinute(minute);
    }

    public int getSecond()
    {
        return getTime().getSecond();
    }

    public void setSecond(int second)
    {
        getTime().setSecond(second);
    }

    /**
     * Clear all date/time fields
     */
    public void clear()
    {
        getDate().clear();
        getTime().clear();
    }

    /**
     * Set Date/Time from an ISO8601 string, e.g. "2007-04-05T14:30:00".
     * <p/>
     * According to ISO 8601, both short (e.g. "20070405T143000") and extended (e.g. "2007-04-05T14:30:00")
     * formats are supported.
     *
     * @param dateTime datetime specification as defined by ISO8601, e.g. "2007-04-05T14:30"
     */
    public void fromString(String dateTime)
    {
        int index = dateTime.indexOf("T");
        if (index == -1) {
            getDate().fromString(dateTime);
        }
        else {
            getDate().fromString(dateTime.substring(0, index));
            getTime().fromString(dateTime.substring(index + 1, dateTime.length()));
        }
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
    public int compareTo(AbsoluteDateTime absoluteDateTime)
    {
        final int EQUAL = 0;

        if (this == absoluteDateTime) {
            return EQUAL;
        }

        int dateResult = getDate().compareTo(absoluteDateTime.getDate());
        if (dateResult != EQUAL) {
            return dateResult;
        }

        int timeResult = getTime().compareTo(absoluteDateTime.getTime());
        if (timeResult != EQUAL) {
            return timeResult;
        }

        return EQUAL;
    }

    /**
     * Is this Date/Time before given.
     *
     * @param dateTime
     * @return boolean
     */
    public boolean before(AbsoluteDateTime dateTime)
    {
        return compareTo(dateTime) < 0;
    }

    /**
     * Is this Date/Time after given.
     *
     * @param dateTime
     * @return boolean
     */
    public boolean after(AbsoluteDateTime dateTime)
    {
        return compareTo(dateTime) > 0;
    }

    /**
     * Clone absolute date/time.
     *
     * @return cloned instance of absolute date/time
     */
    public AbsoluteDateTime clone()
    {
        AbsoluteDateTime absoluteDateTime = new AbsoluteDateTime();
        absoluteDateTime.setYear(getYear());
        absoluteDateTime.setMonth(getMonth());
        absoluteDateTime.setDay(getDay());
        absoluteDateTime.setHour(getHour());
        absoluteDateTime.setMinute(getMinute());
        absoluteDateTime.setSecond(getSecond());
        return absoluteDateTime;
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

        AbsoluteDateTime absoluteDateTime = clone();
        Date date = absoluteDateTime.getDate();
        Time time = absoluteDateTime.getTime();

        date.addYearInplace(period.getYear());
        date.addMonthInplace(period.getMonth());
        date.addDayInplace(period.getDay());

        int overflowedDays = 0;
        overflowedDays += time.addHourInplace(period.getHour());
        overflowedDays += time.addMinuteInplace(period.getMinute());
        overflowedDays += time.addSecondInplace(period.getSecond());
        if (overflowedDays != 0) {
            date.addDayInplace(overflowedDays);
        }

        return absoluteDateTime;
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

        AbsoluteDateTime absoluteDateTime = clone();
        Date date = absoluteDateTime.getDate();
        Time time = absoluteDateTime.getTime();

        date.addYearInplace(-period.getYear());
        date.addMonthInplace(-period.getMonth());
        date.addDayInplace(-period.getDay());

        int overflowedDays = 0;
        overflowedDays += time.addHourInplace(-period.getHour());
        overflowedDays += time.addMinuteInplace(-period.getMinute());
        overflowedDays += time.addSecondInplace(-period.getSecond());
        if (overflowedDays != 0) {
            date.addDayInplace(overflowedDays);
        }

        return absoluteDateTime;
    }
}
