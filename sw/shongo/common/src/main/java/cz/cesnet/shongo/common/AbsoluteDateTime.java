package cz.cesnet.shongo.common;

import cz.cesnet.shongo.common.util.Parser;

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
    public final int NULL = Integer.MAX_VALUE;

    private int year;
    private int month;
    private int day;
    private int hour;
    private int minute;
    private int second;

    /**
     * Construct zero date/time
     */
    public AbsoluteDateTime()
    {
        clear();
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
        return year;
    }

    public void setYear(int year)
    {
        assert (year >= 0 && year <= 9999) : "Year should be in range 0 to 9999.";
        this.year = (year == 0 ? NULL : year);
    }

    public int getMonth()
    {
        return month;
    }

    public void setMonth(int month)
    {
        assert (month >= 0 && month <= 12) : "Month should be in range 0 to 12.";
        this.month = (month == 0 ? NULL : month);
    }

    public int getDay()
    {
        return day;
    }

    public void setDay(int day)
    {
        assert (day >= 0 && day <= 31) : "Day should be in range 0 to 31.";
        this.day = (day == 0 ? NULL : day);
    }

    public int getHour()
    {
        return hour;
    }

    public void setHour(int hour)
    {
        assert (hour >= 0 && hour <= 23) : "Hour should be in range 0 to 23.";
        this.hour = hour;
    }

    public int getMinute()
    {
        return minute;
    }

    public void setMinute(int minute)
    {
        assert (minute >= 0 && minute <= 59) : "Minute should be in range 0 to 59.";
        this.minute = minute;
    }

    public int getSecond()
    {
        return second;
    }

    public void setSecond(int second)
    {
        assert (second >= 0 && second <= 59) : "Second should be in range 0 to 59.";
        this.second = second;
    }

    /**
     * Clear all date/time fields
     */
    public void clear()
    {
        year = NULL;
        month = NULL;
        day = NULL;
        hour = NULL;
        minute = NULL;
        second = NULL;
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
        clear();
        try {
            AbsoluteDateTimeParser parser = new AbsoluteDateTimeParser(Parser.getTokenStream(dateTime,
                    AbsoluteDateTimeLexer.class));
            parser.setAbsoluteDateTime(this);
            parser.parse();
        }
        catch (Exception exception) {
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
        int year = getYear();
        if (year == NULL) {
            year = 0;
        }
        int month = getMonth();
        if (month == NULL) {
            month = 0;
        }
        int day = getDay();
        if (day == NULL) {
            day = 0;
        }

        StringBuilder dateTime = new StringBuilder();
        dateTime.append(String.format("%04d-%02d-%02d", year, month, day));
        StringBuilder time = new StringBuilder();
        if (getHour() != NULL) {
            time.append(String.format("%02d", getHour()));
            if (getMinute() != NULL) {
                time.append(String.format(":%02d", getMinute()));
                if (getSecond() != NULL) {
                    time.append(String.format(":%02d", getSecond()));
                }
            }
        }
        if (time.length() > 0) {
            dateTime.append("T");
            dateTime.append(time);
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
        if (getYear() != NULL && dateTime.getYear() != NULL && getYear() != dateTime.getYear()) {
            return false;
        }
        if (getMonth() != NULL && dateTime.getMonth() != NULL && getMonth() != dateTime.getMonth()) {
            return false;
        }
        if (getDay() != NULL && dateTime.getDay() != NULL && getDay() != dateTime.getDay()) {
            return false;
        }
        if (getHour() != NULL && dateTime.getHour() != NULL && getHour() != dateTime.getHour()) {
            return false;
        }
        if (getMinute() != NULL && dateTime.getMinute() != NULL && getMinute() != dateTime.getMinute()) {
            return false;
        }
        if (getSecond() != NULL && dateTime.getSecond() != NULL && getSecond() != dateTime.getSecond()) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(AbsoluteDateTime absoluteDateTime)
    {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (this == absoluteDateTime) {
            return EQUAL;
        }

        int year1 = getYear();
        int year2 = absoluteDateTime.getYear();
        if (year1 != NULL && year2 != NULL) {
            if (year1 < year2) {
                return BEFORE;
            }
            else if (year1 > year2) {
                return AFTER;
            }
        }

        int month1 = getMonth();
        int month2 = absoluteDateTime.getMonth();
        if (month1 != NULL && month2 != NULL) {
            if (month1 < month2) {
                return BEFORE;
            }
            else if (month1 > month2) {
                return AFTER;
            }
        }

        int day1 = getDay();
        int day2 = absoluteDateTime.getDay();
        if (day1 != NULL && day2 != NULL) {
            if (day1 < day2) {
                return BEFORE;
            }
            else if (day1 > day2) {
                return AFTER;
            }
        }

        int hour1 = getHour();
        int hour2 = absoluteDateTime.getHour();
        if (hour1 < hour2) {
            return BEFORE;
        }
        else if (hour1 > hour2) {
            return AFTER;
        }

        int minut1 = getMinute();
        int minute2 = absoluteDateTime.getMinute();
        if (minut1 != NULL && minute2 != NULL) {
            if (minut1 < minute2) {
                return BEFORE;
            }
            else if (minut1 > minute2) {
                return AFTER;
            }
        }

        int second1 = getSecond();
        int second2 = absoluteDateTime.getSecond();
        if (second1 != NULL && second2 != NULL) {
            if (second1 < second2) {
                return BEFORE;
            }
            else if (second1 > second2) {
                return AFTER;
            }
        }

        return EQUAL;
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
     * <p/>
     * This datetime object is not modified.
     *
     * @param period time period to add
     * @return resulting datetime
     */
    public AbsoluteDateTime add(Period period)
    {
        throw new RuntimeException("TODO: Implement AbsoluteDateTime.add");
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
        throw new RuntimeException("TODO: Implement AbsoluteDateTime.subtract");
    }

}
