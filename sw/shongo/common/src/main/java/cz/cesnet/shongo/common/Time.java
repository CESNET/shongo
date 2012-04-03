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
    public final int NullValue = Integer.MAX_VALUE;

    private int hour;
    private int minute;
    private int second;

    /**
     * Construct empty time
     */
    public Time()
    {
        clear();
    }

    /**
     * Construct time from ISO8601 string, e.g. "12:00:00"
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
        if ( hour == NullValue ) {
            this.hour = NullValue;
            return;
        }
        assert (hour >= 0 && hour <= 23) : "Hour should be in range 0 to 23.";
        this.hour = hour;
    }

    public int getMinute()
    {
        return minute;
    }

    public void setMinute(int minute)
    {
        if ( minute == NullValue ) {
            this.minute = NullValue;
            return;
        }
        assert (minute >= 0 && minute <= 59) : "Minute should be in range 0 to 59.";
        this.minute = minute;
    }

    public int getSecond()
    {
        return second;
    }

    public void setSecond(int second)
    {
        if ( second == NullValue ) {
            this.second = NullValue;
            return;
        }
        assert (second >= 0 && second <= 59) : "Second should be in range 0 to 59.";
        this.second = second;
    }

    /**
     * Check whether all fields have NullValue
     *
     * @return boolean
     */
    public boolean isEmpty()
    {
        return getHour() == NullValue && getMinute() == NullValue && getSecond() == NullValue;
    }

    /**
     * Clear all fields
     */
    public void clear()
    {
        hour = NullValue;
        minute = NullValue;
        second = NullValue;
    }

    /**
     * Set time from an ISO8601 string, e.g. "12:00:00".

     * @param time time specification as defined by ISO8601
     */
    public void fromString(String time)
    {
        clear();
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
     * Get time as ISO8601 string
     *
     * @return string of ISO8601 time
     */
    public String toString()
    {
        StringBuilder time = new StringBuilder();
        if (getHour() != NullValue) {
            time.append(String.format("%02d", getHour()));
            if (getMinute() != NullValue) {
                time.append(String.format(":%02d", getMinute()));
                if (getSecond() != NullValue) {
                    time.append(String.format(":%02d", getSecond()));
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
        if (getHour() != NullValue && time.getHour() != NullValue && getHour() != time.getHour()) {
            return false;
        }
        if (getMinute() != NullValue && time.getMinute() != NullValue && getMinute() != time.getMinute()) {
            return false;
        }
        if (getSecond() != NullValue && time.getSecond() != NullValue && getSecond() != time.getSecond()) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Time time)
    {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (this == time) {
            return EQUAL;
        }

        int hour1 = getHour();
        int hour2 = time.getHour();
        if (hour1 < hour2) {
            return BEFORE;
        }
        else if (hour1 > hour2) {
            return AFTER;
        }

        int minute1 = getMinute();
        int minute2 = time.getMinute();
        if (minute1 != NullValue && minute2 != NullValue) {
            if (minute1 < minute2) {
                return BEFORE;
            }
            else if (minute1 > minute2) {
                return AFTER;
            }
        }

        int second1 = getSecond();
        int second2 = time.getSecond();
        if (second1 != NullValue && second2 != NullValue) {
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
     * Clone time
     *
     * @return cloned instance of time
     */
    public Time clone()
    {
        Time time = new Time();
        time.setHour(getHour());
        time.setMinute(getMinute());
        time.setSecond(getSecond());
        return time;
    }

    /**
     * Add to hour and check whether the time overflowed to the next day(s).
     *
     * @param hour
     * @return number of overflowed days
     */
    public int addHourInplace(int hour)
    {
        int overflowedDays = 0;

        // Update hours
        if ( this.hour == NullValue )
            throw new RuntimeException("Can't add to hours because it is empty.");
        this.hour += hour;

        // Overflow to days
        if ( this.hour < 0 || this.hour >= 24 ) {
            overflowedDays += this.hour / 24;
            this.hour %= 24;
            if ( this.hour < 0 ) {
                this.hour += 24;
                overflowedDays--;
            }
        }

        return overflowedDays;
    }

    /**
     * Add to minute and check whether the time overflowed to the next day(s).
     *
     * @param minute
     * @return number of overflowed days
     */
    public int addMinuteInplace(int minute)
    {
        int overflowedDays = 0;

        // Update minutes
        if ( this.minute == NullValue )
            throw new RuntimeException("Can't add to minutes because it is empty.");
        this.minute += minute;

        // Overflow to hours
        if ( this.minute < 0 || this.minute >= 60) {
            overflowedDays += addHourInplace(this.minute / 60);
            this.minute %= 60;
            if ( this.minute < 0 ) {
                this.minute += 60;
                overflowedDays += addHourInplace(-1);
            }
        }

        return overflowedDays;
    }

    /**
     * Add to second and check whether the time overflowed to the next day(s).
     *
     * @param second
     * @return number of overflowed days
     */
    public int addSecondInplace(int second)
    {
        int overflowedDays = 0;

        // Update seconds
        if ( this.second == NullValue )
            throw new RuntimeException("Can't add to seconds because it is empty.");
        this.second += second;

        // Overflow to minutes
        if ( this.second < 0 || this.second >= 60) {
            overflowedDays += addMinuteInplace(this.second / 60);
            this.second %= 60;
            if ( this.second < 0 ) {
                this.second += 60;
                overflowedDays += addMinuteInplace(-1);
            }
        }

        return overflowedDays;
    }

    /**
     * Add to hour and return new time object. This object will not be modified.
     *
     * @param hour
     * @return a new time
     */
    public Time addHour(int hour)
    {
        Time time = clone();
        time.addHourInplace(hour);
        return time;
    }

    /**
     * Add to minute and return new time object. This object will not be modified.
     *
     * @param minute
     * @return a new time
     */
    public Time addMinute(int minute)
    {
        Time time = clone();
        time.addMinuteInplace(minute);
        return time;
    }

    /**
     * Add to second and return new time object. This object will not be modified.
     *
     * @param second
     * @return a new time
     */
    public Time addSecond(int second)
    {
        Time time = clone();
        time.addSecondInplace(second);
        return time;
    }
}
