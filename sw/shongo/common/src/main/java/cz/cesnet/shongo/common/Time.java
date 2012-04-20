package cz.cesnet.shongo.common;

import cz.cesnet.shongo.common.util.Parser;

/**
 * Represents absolute time
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public final class Time implements Comparable<Time>, Cloneable
{
    /**
     * Null field value.
     */
    public static final int NullValue = Integer.MAX_VALUE;

    private final int hour;
    private final int minute;
    private final int second;
    private final int overflow;

    /**
     * Construct time by field values.
     *
     * @param hour
     * @param minute
     * @param second
     */
    public Time(int hour, int minute, int second, int overflow)
    {
        assert (hour == NullValue || (hour >= 0 && hour <= 23)) : "Hour should be in range 0 to 23.";
        assert (minute == NullValue || (minute >= 0 && minute <= 59)) : "Minute should be in range 0 to 59.";
        assert (second == NullValue || (second >= 0 && second <= 59)) : "Second should be in range 0 to 59.";

        this.hour = hour;
        this.minute = minute;
        this.second = second;
        this.overflow = overflow;
    }

    public Time()
    {
        this(NullValue, NullValue, NullValue, 0);
    }

    public Time(int hour)
    {
        this(hour, NullValue, NullValue, 0);
    }

    public Time(int hour, int minute)
    {
        this(hour, minute, NullValue, 0);
    }

    public Time(int hour, int minute, int second)
    {
        this(hour, minute, second, 0);
    }

    /**
     * Construct time from ISO8601 string, e.g. "12:00:00".
     *
     * @param time
     */
    public Time(String time)
    {
        try {
            TimeParser parser = new TimeParser(Parser.getTokenStream(time, TimeLexer.class));
            parser.parse();

            hour = parser.hour;
            minute = parser.minute;
            second = parser.second;
            overflow = 0;
        }
        catch (Exception exception) {
            throw new RuntimeException(
                    String.format("Failed to parse time '%s': %s", time, exception.getMessage()));
        }
    }

    /**
     * Get time hour.
     *
     * @return hour
     */
    public int getHour()
    {
        return hour;
    }

    /**
     * Get time minute.
     *
     * @return minute
     */
    public int getMinute()
    {
        return minute;
    }

    /**
     * Get time second.
     *
     * @return second
     */
    public int getSecond()
    {
        return second;
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

    @Override
    public Object clone()
    {
        return new Time(hour, minute, second);
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
        int resultHour = this.hour;
        int resultMinute = this.minute;
        int resultSecond = this.second;
        int resultOverflow = 0;

        if (second > 0) {
            assert (resultSecond != NullValue) : "Can't add to seconds because it is empty.";
            resultSecond += second;
            if (resultSecond >= 60) {
                minute += resultSecond / 60;
                resultSecond %= 60;
            }
        }

        if (minute > 0) {
            assert (resultMinute != NullValue) : "Can't add to minutes because it is empty.";
            resultMinute += minute;
            if (resultMinute >= 60) {
                hour += resultMinute / 60;
                resultMinute %= 60;
            }
        }

        if (hour > 0) {
            assert (resultHour != NullValue) : "Can't add to hours because it is empty.";
            resultHour += hour;
            if (resultHour >= 24) {
                resultOverflow = resultHour / 24;
                resultHour %= 24;
            }
        }

        return new Time(resultHour, resultMinute, resultSecond, resultOverflow);
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
        int resultHour = this.hour;
        int resultMinute = this.minute;
        int resultSecond = this.second;
        int resultOverflow = 0;

        if (second > 0) {
            assert (resultSecond != NullValue) : "Can't subtract from seconds because it is empty.";
            resultSecond -= second;
            if (resultSecond < 0) {
                minute += -resultSecond / 60;
                resultSecond %= 60;
                if (resultSecond != 0) {
                    resultSecond += 60;
                    minute++;
                }
            }
        }

        if (minute > 0) {
            assert (resultMinute != NullValue) : "Can't subtract from minutes because it is empty.";
            resultMinute -= minute;
            if (resultMinute < 0) {
                hour += -resultMinute / 60;
                resultMinute %= 60;
                if (resultMinute != 0) {
                    resultMinute += 60;
                    hour++;
                }
            }
        }

        if (hour > 0) {
            assert (resultHour != NullValue) : "Can't subtract from hours because it is empty.";
            resultHour -= hour;
            if (resultHour < 0) {
                resultOverflow = resultHour / 24;
                resultHour %= 24;
                if (resultHour != 0) {
                    resultHour += 24;
                    resultOverflow--;
                }
            }
        }

        return new Time(resultHour, resultMinute, resultSecond, resultOverflow);
    }

    /**
     * Get overflowed days.
     *
     * @return overflowed days
     */
    public int getOverflow()
    {
        assert (overflow >= 0) : "Overflow should be positive.";
        return overflow;
    }

    /**
     * Get under-flowed days.
     *
     * @return under-flowed days
     */
    public int getUnderflow()
    {
        assert (overflow <= 0) : "Underflow should be negative.";
        return -overflow;
    }

    /**
     * Merge this time with given time and return
     * result. This and given time stay unchanged.
     * <p/>
     * The returned time contains values from this time
     * replaced by non-empty values from given time.
     *
     * @param time Time to merge
     */
    public Time merge(Time time)
    {
        int resultHour = (time.hour != NullValue ? time.hour : hour);
        int resultMinute = (time.minute != NullValue ? time.minute : minute);
        int resultSecond = (time.second != NullValue ? time.second : second);
        return new Time(resultHour, resultMinute, resultSecond);
    }
}
