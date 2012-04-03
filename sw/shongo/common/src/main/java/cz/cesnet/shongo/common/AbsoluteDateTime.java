package cz.cesnet.shongo.common;

import cz.cesnet.shongo.common.util.Parser;

import java.util.GregorianCalendar;

/**
 * Represents an absolute Date/Time.
 *
 * @author Martin Srom
 */
public class AbsoluteDateTime extends DateTime implements Comparable
{
    /**
     * Internal Date/Time storage
     */
    private Calendar calendar = new Calendar();

    /**
     * Calendar type
     *
     * @author Martin Srom
     */
    private static class Calendar extends GregorianCalendar
    {
        public int getWithoutNormalization(int field)
        {
            return internalGet(field);
        }
    }

    /**
     * Construct zero date/time
     */
    public AbsoluteDateTime()
    {
        fromString("00000000T000000");
    }

    /**
     * Construct date/time from an ISO8601 string, e.g. "2007-04-05T14:30:00"
     *
     * @param dateTime ISO8601 Date/Time; see {@link #fromString} for more info about supported input formats
     */
    public AbsoluteDateTime(String dateTime)
    {
        fromString(dateTime);
    }
    
    public int getYear()
    {
        if (calendar.isSet(Calendar.YEAR)) {
            return calendar.getWithoutNormalization(Calendar.YEAR);
        }
        return 0;
    }

    public void setYear(int year)
    {
        assert (year >= 0 && year <= 9999) : "Year should be in range 0 to 9999.";
        if (year == 0) {
            calendar.clear(Calendar.YEAR);
        }
        else {
            calendar.set(Calendar.YEAR, year);
        }
    }

    public int getMonth()
    {
        if (calendar.isSet(Calendar.MONTH)) {
            return calendar.getWithoutNormalization(Calendar.MONTH);
        }
        return 0;
    }

    public void setMonth(int month)
    {
        assert (month >= 0 && month <= 12) : "Month should be in range 0 to 12.";
        if (month == 0) {
            calendar.clear(Calendar.MONTH);
        }
        else {
            calendar.set(Calendar.MONTH, month);
        }
    }

    public int getDay()
    {
        if (calendar.isSet(Calendar.DAY_OF_MONTH))  {
            return calendar.getWithoutNormalization(Calendar.DAY_OF_MONTH);
        }
        return 0;
    }

    public void setDay(int day)
    {
        assert (day >= 0 && day <= 31) : "Day should be in range 0 to 31.";
        if (day == 0) {
            calendar.clear(Calendar.DAY_OF_MONTH);
        }
        else {
            calendar.set(Calendar.DAY_OF_MONTH, day);
        }
    }

    public int getHour()
    {
        return calendar.getWithoutNormalization(Calendar.HOUR_OF_DAY);
    }

    public void setHour(int hour)
    {
        assert (hour >= 0 && hour <= 23) : "Hour should be in range 0 to 23.";
        calendar.set(Calendar.HOUR_OF_DAY, hour);
    }

    public int getMinute()
    {
        return calendar.getWithoutNormalization(Calendar.MINUTE);
    }

    public void setMinute(int minute)
    {
        assert (minute >= 0 && minute <= 59) : "Minute should be in range 0 to 59.";
        calendar.set(Calendar.MINUTE, minute);
    }

    public int getSecond()
    {
        return calendar.getWithoutNormalization(Calendar.SECOND);
    }

    public void setSecond(int second)
    {
        assert (second >= 0 && second <= 59) : "Second should be in range 0 to 59.";
        calendar.set(Calendar.SECOND, second);
    }

    /**
     * Set Date/Time from an ISO8601 string, e.g. "2007-04-05T14:30:00".
     *
     * According to ISO 8601, both short (e.g. "20070405T143000") and extended (e.g. "2007-04-05T14:30:00")
     * formats are supported.
     *
     * @param dateTime    datetime specification as defined by ISO8601, e.g. "2007-04-05T14:30"
     */
    public void fromString(String dateTime)
    {
        calendar.clear();
        try {
            AbsoluteDateTimeParser parser = new AbsoluteDateTimeParser(Parser.getTokenStream(dateTime,
                    AbsoluteDateTimeLexer.class));
            parser.setAbsoluteDateTime(this);
            parser.parse();
        }
        catch ( Exception exception ) {
            throw new RuntimeException(
                    String.format("Failed to parse date/time '%s': %s", dateTime, exception.getMessage()));
        }
    }

    /**
     * Get Date/Time as ISO8601 string
     *
     * @return string of ISO8601 Date/Time
     */
    public String toString()
    {
        String date = String.format("%04d-%02d-%02d", getYear(), getMonth(), getDay());
        String time = String.format("%02d:%02d:%02d", getHour(), getMinute(), getSecond());
        String dateTime = date + "T" + time;
        return dateTime;
    }

    @Override
    public AbsoluteDateTime getEarliest(AbsoluteDateTime referenceDateTime)
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

    /**
     * Adds a time period to this datetime and returns the result.
     *
     * This datetime object is not modified.
     *
     * @param period    time period to add
     * @return resulting datetime
     */
    public AbsoluteDateTime add(Period period)
    {
        return this; // FIXME
    }

    /**
     * Subtracts a time period from this datetime and returns the result.
     *
     * This datetime object is not modified.
     *
     * @param period    time period to subtract
     * @return resulting datetime
     */
    public AbsoluteDateTime subtract(Period period)
    {
        return this; // FIXME
    }

}
