package cz.cesnet.shongo.common;

import cz.cesnet.shongo.common.util.Parser;

/**
 * Represents absolute time
 *
 * @author Martin Srom
 */
public class Time implements Comparable<Time>
{
    /**
     * Null field value.
     */
    public static final int NullValue = Integer.MAX_VALUE;

    private int hour;
    private int minute;
    private int second;
    private int overflow = 0;
    private int underflow = 0;

    /**
     * Construct time by field values.
     *
     * @param hour
     * @param minute
     * @param second
     */
    public Time(int hour, int minute, int second)
    {
        setHour(hour);
        setMinute(minute);
        setSecond(second);
    }

    public Time()
    {
        this(NullValue, NullValue, NullValue);
    }

    public Time(int hour)
    {
        this(hour, NullValue, NullValue);
    }

    public Time(int hour, int minute)
    {
        this(hour, NullValue, NullValue);
    }

    /**
     * Construct time from ISO8601 string, e.g. "12:00:00".
     *
     * @param time
     */
    public Time(String time)
    {
        fromString(time);
    }

    public int getHour()
    {
        return hour;
    }

    public void setHour(int hour)
    {
        assert (hour == NullValue || (hour >= 0 && hour <= 23)) : "Hour should be in range 0 to 23.";
        this.hour = hour;
    }

    public int getMinute()
    {
        return minute;
    }

    public void setMinute(int minute)
    {
        assert (minute == NullValue || (minute >= 0 && minute <= 59)) : "Minute should be in range 0 to 59.";
        this.minute = minute;
    }

    public int getSecond()
    {
        return second;
    }

    public void setSecond(int second)
    {
        assert (second == NullValue || (second >= 0 && second <= 59)) : "Second should be in range 0 to 59.";
        this.second = second;
    }

    /**
     * Check whether all fields have NullValue.
     *
     * @return boolean
     */
    public boolean isEmpty()
    {
        return hour == NullValue && minute == NullValue && second == NullValue;
    }

    /**
     * Clear all fields.
     */
    public void setEmpty()
    {
        hour = NullValue;
        minute = NullValue;
        second = NullValue;
    }

    /**
     * Set time from an ISO8601 string, e.g. "12:00:00".
     *
     * @param time time specification as defined by ISO8601
     */
    public void fromString(String time)
    {
        setEmpty();
        try {
            TimeParser parser = new TimeParser(Parser.getTokenStream(time, TimeLexer.class));
            parser.setTime(this);
            parser.parse();
        }
        catch (Exception exception) {
            throw new RuntimeException(
                    String.format("Failed to parse time '%s': %s", time, exception.getMessage()));
        }
    }

    /**
     * Get time as ISO8601 string.
     *
     * @return string of ISO8601 time
     */
    public String toString()
    {
        StringBuilder time = new StringBuilder();
        if (hour != NullValue) {
            time.append(String.format("%02d", hour));
            if (minute != NullValue) {
                time.append(String.format(":%02d", minute));
                if (second != NullValue) {
                    time.append(String.format(":%02d", second));
                }
            }
        }
        return time.toString();
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
        Time time = (Time) object;
        if (hour != time.hour) {
            return false;
        }
        if (minute != time.minute) {
            return false;
        }
        if (second != time.second) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int result = 13;
        result = 37 * result + (hour != NullValue ? hour : 0);
        result = 37 * result + (minute != NullValue ? minute : 0);
        result = 37 * result + (second != NullValue ? second : 0);
        return result;
    }

    @Override
    public int compareTo(Time time)
    {
        if (this == time) {
            return 0;
        }

        // Fields should be both empty or nonempty
        assert ((hour == NullValue && time.hour == NullValue) || (hour != NullValue && time.hour != NullValue)) :
                "Can't compare times with empty hour in only one of them.";
        assert ((minute == NullValue && time.minute == NullValue) || (minute != NullValue && time.minute != NullValue)) :
                "Can't compare times with empty minute in only one of them.";
        assert ((second == NullValue && time.second == NullValue) || (second != NullValue && time.second != NullValue)) :
                "Can't compare times with empty hour in only one of them.";

        // Compare hours
        if (hour != NullValue && time.hour != NullValue) {
            if (hour < time.hour) {
                return -1;
            }
            else if (hour > time.hour) {
                return 1;
            }
        }
        else {
            assert (minute == NullValue && time.minute == NullValue) : "Can't compare minutes with empty hours.";
            assert (second == NullValue && time.second == NullValue) : "Can't compare seconds with empty hours.";
        }

        // Compare minutes
        if (minute != NullValue && this.minute != NullValue) {
            if (minute < time.minute) {
                return -1;
            }
            else if (minute > time.minute) {
                return 1;
            }
        }
        else {
            assert (second == NullValue && time.second == NullValue) : "Can't compare seconds with empty hours.";
        }

        // Compare seconds
        if (second != NullValue && time.second != NullValue) {
            if (second < time.second) {
                return -1;
            }
            else if (second > this.second) {
                return 0;
            }
        }

        return 0;
    }

    /**
     * Checks whether this time equals the given time by skipping
     * all empty fields (in this or given time).
     *
     * @param time
     * @return true if this time matches the given time,
     *         false otherwise
     */
    public boolean match(Time time)
    {
        if (this == time) {
            return true;
        }
        if (time == null) {
            return false;
        }
        if (hour != NullValue && time.hour != NullValue && hour != time.hour) {
            return false;
        }
        if (minute != NullValue && time.minute != NullValue && minute != time.minute) {
            return false;
        }
        if (second != NullValue && time.second != NullValue && second != time.second) {
            return false;
        }
        return true;
    }

    /**
     * Clone time.
     *
     * @return cloned instance of time
     */
    public Time clone()
    {
        Time time = new Time();
        time.hour = hour;
        time.minute = minute;
        time.second = second;
        return time;
    }

    /**
     * Add given time to this time. This object is not modified.
     *
     * @param time
     * @return result of addition
     */
    public Time add(Time time)
    {
        int hour = (time.hour != NullValue ? time.hour : 0);
        int minute = (time.minute != NullValue ? time.minute : 0);
        int second = (time.second != NullValue ? time.second : 0);
        return add(hour, minute, second);
    }

    /**
     * Add given time to this time. This object is not modified.
     *
     * @param hour
     * @param minute
     * @param second
     * @return result of addition
     */
    public Time add(int hour, int minute, int second)
    {
        Time result = clone();

        if (second > 0) {
            assert (result.second != NullValue) : "Can't add to seconds because it is empty.";
            result.second += second;
            if (result.second >= 60) {
                minute += result.second / 60;
                result.second %= 60;
            }
        }

        if (minute > 0) {
            assert (result.minute != NullValue) : "Can't add to minutes because it is empty.";
            result.minute += minute;
            if (result.minute >= 60) {
                hour += result.minute / 60;
                result.minute %= 60;
            }
        }

        if (hour > 0) {
            assert (result.hour != NullValue) : "Can't add to hours because it is empty.";
            result.hour += hour;
            if (result.hour >= 24) {
                result.overflow += result.hour / 24;
                result.hour %= 24;
            }
        }

        return result;
    }

    /**
     * Subtract given time from this time. This object is not modified.
     *
     * @param time
     * @return result of subtraction
     */
    public Time subtract(Time time)
    {
        int hour = (time.hour != NullValue ? time.hour : 0);
        int minute = (time.minute != NullValue ? time.minute : 0);
        int second = (time.second != NullValue ? time.second : 0);
        return subtract(hour, minute, second);
    }

    /**
     * Subtract given time from this time. This object is not modified.
     *
     * @param hour
     * @param minute
     * @param second
     * @return result of subtraction
     */
    public Time subtract(int hour, int minute, int second)
    {
        Time result = clone();

        if (second > 0) {
            assert (result.second != NullValue) : "Can't subtract from seconds because it is empty.";
            result.second -= second;
            if (result.second < 0) {
                minute += -result.second / 60;
                result.second %= 60;
                if (result.second != 0) {
                    result.second += 60;
                    minute++;
                }
            }
        }

        if (minute > 0) {
            assert (result.minute != NullValue) : "Can't subtract from minutes because it is empty.";
            result.minute -= minute;
            if (result.minute < 0) {
                hour += -result.minute / 60;
                result.minute %= 60;
                if (result.minute != 0) {
                    result.minute += 60;
                    hour++;
                }
            }
        }

        if (hour > 0) {
            assert (result.hour != NullValue) : "Can't subtract from hours because it is empty.";
            result.hour -= hour;
            if (result.hour < 0) {
                result.underflow += -result.hour / 24;
                result.hour %= 24;
                if (result.hour != 0) {
                    result.hour += 24;
                    result.underflow++;
                }
            }
        }

        return result;
    }

    /**
     * Checks if overflowed days are nonzero
     *
     * @return boolean
     */
    public boolean isOverflow()
    {
        return this.overflow > 0;
    }

    /**
     * Get overflowed days and set the overflowed days to zero.
     *
     * @return overflowed days
     */
    public int popOverflow()
    {
        int overflow = this.overflow;
        this.overflow = 0;
        return overflow;
    }

    /**
     * Checks if under-flowed days are nonzero
     *
     * @return boolean
     */
    public boolean isUnderflow()
    {
        return this.underflow > 0;
    }

    /**
     * Get under-flowed days and set the under-flowed days to zero.
     *
     * @return under-flowed days
     */
    public int popUnderflow()
    {
        int underflow = this.underflow;
        this.underflow = 0;
        return underflow;
    }
}
