package cz.cesnet.shongo.common;

import cz.cesnet.shongo.common.util.Parser;

/**
 * Represents a duration or period.
 *
 * @author Martin Srom
 */
public class Period
{
    private int year = 0;
    private int month = 0;
    private int day = 0;
    private int hour = 0;
    private int minute = 0;
    private int second = 0;
    private int week = 0;

    public int getYear()
    {
        return year;
    }

    public void setYear(int year)
    {
        this.year = year;
    }

    public int getMonth()
    {
        return month;
    }

    public void setMonth(int month)
    {
        this.month = month;
    }

    public int getDay()
    {
        return day;
    }

    public void setDay(int day)
    {
        this.day = day;
    }

    public int getHour()
    {
        return hour;
    }

    public void setHour(int hour)
    {
        this.hour = hour;
    }

    public int getMinute()
    {
        return minute;
    }

    public void setMinute(int minute)
    {
        this.minute = minute;
    }

    public int getSecond()
    {
        return second;
    }

    public void setSecond(int second)
    {
        this.second = second;
    }

    public int getWeek()
    {
        return week;
    }

    public void setWeek(int week)
    {
        this.week = week;
    }

    /**
     * Constructs a zero period
     */
    public Period()
    {
    }

    /**
     * Constructs a new period from a given ISO 8601 duration string, e.g. "P3Y6M4DT12H30M5S".
     *
     * @param period    a period string as defined by ISO8601, except decimal fractions, which are not supported
     */
    public Period(String period)
    {
        fromString(period);
    }

    /**
     * Set Period from a given ISO 8601 duration string, e.g. "P3Y6M4DT12H30M5S".
     *
     * @param period    a period string as defined by ISO 8601, except decimal fractions, which are not supported
     */
    public void fromString(String period)
    {
        try {
            PeriodParser parser = new PeriodParser(Parser.getTokenStream(period, PeriodLexer.class));
            parser.setPeriod(this);
            parser.parse();
        }
        catch ( Exception exception ) {
            throw new RuntimeException(
                    String.format("Fail to parse '%s': %s", period, exception.getMessage()));
        }
        normalize();
    }

    /**
     * Get period as ISO 8601 duration, e.g. "P3Y6M4DT12H30M5S".
     *
     * Individual date/time components do not exceed their natural range, e.g. "PT25H" is returned se "P1DT1H".
     * Returns the shortest representation possible, i.e. omits zero components, except for zero period, which (instead
     * of just "P") is returned as "PT0S" to make it more readable.
     *
     * @return string of ISO 8601 duration
     */
    public String toString()
    {
        normalize();

        StringBuilder period = new StringBuilder();
        if ( getYear() != 0 ) {
            period.append(getYear() + "Y");
        }
        if ( getMonth() != 0 ) {
            period.append(getMonth() + "M");
        }
        if ( getDay() != 0 ) {
            period.append(getDay() + "D");
        }
        StringBuilder time = new StringBuilder();
        if ( getHour() != 0 ) {
            time.append(getHour() + "H");
        }
        if ( getMinute() != 0 ) {
            time.append(getMinute() + "M");
        }
        if ( getSecond() != 0 ) {
            time.append(getSecond() + "S");
        }
        if ( time.length() > 0 ) {
            period.append("T");
            period.append(time);
        }
        if ( period.length() == 0 ) {
            period.append("T0S");
        }
        return "P" + period.toString();
    }

    /**
     * Perform normalization of period. Months should be < 12, hours < 24, minutes < 60
     * and seconds < 60.
     */
    public void normalize()
    {
        if ( second >= 60 ) {
            minute += second / 60;
            second %= 60;
        }
        if ( minute >= 60 ) {
            hour += minute / 60;
            minute %= 60;
        }
        if ( hour >= 24 ) {
            day += hour / 24;
            hour %= 24;
        }
        if ( month >= 12 ) {
            year += month / 12;
            month %= 12;
        }
        if ( week > 0 ) {
            day += week * 7;
            week = 0;
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        
        return toString().equals(o.toString());
    }

    /**
     * Adds a period of time to this period and returns the resulting period.
     *
     * Does not modify this object.
     *
     * @param period    time period to add to this one
     * @return a new period - sum of this and given period
     */
    public Period add(Period period)
    {
        throw new RuntimeException("TODO: Implement Period.add");
    }
}
