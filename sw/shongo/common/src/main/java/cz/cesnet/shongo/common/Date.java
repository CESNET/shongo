package cz.cesnet.shongo.common;

import cz.cesnet.shongo.common.util.Parser;

/**
 * Represents absolute date
 *
 * @author Martin Srom
 */
public class Date implements Comparable<Date>
{
    /**
     * Null field value.
     */
    public final int NullValue = Integer.MAX_VALUE;

    private int year;
    private int month;
    private int day;

    /**
     * Construct empty date
     */
    public Date()
    {
        clear();
    }

    /**
     * Construct date from ISO8601 string, e.g. "2012-01-01"
     *
     * @param date
     */
    public Date(String date)
    {
        fromString(date);
    }

    public int getYear()
    {
        return year;
    }

    public void setYear(int year)
    {
        if ( year == 0 || year == NullValue ) {
            this.year = NullValue;
            return;
        }
        assert (year >= 1 && year <= 9999) : "Year should be in range 0 to 9999.";
        this.year = year;
    }

    public int getMonth()
    {
        return month;
    }

    public void setMonth(int month)
    {
        if ( month == 0 || month == NullValue ) {
            this.month = NullValue;
            return;
        }
        assert (month >= 1 && month <= 12) : "Month should be in range 0 to 12.";
        this.month = month;
    }

    public int getDay()
    {
        return day;
    }

    public void setDay(int day)
    {
        if ( day == 0 || day == NullValue ) {
            this.day = NullValue;
            return;
        }
        assert (day >= 1 && day <= 31) : "Day should be in range 0 to 31.";
        this.day = day;
    }

    /**
     * Check whether all fields have NullValue
     *
     * @return boolean
     */
    public boolean isEmpty()
    {
        return getYear() == NullValue && getMonth() == NullValue && getDay() == NullValue;
    }

    /**
     * Clear all fields
     */
    public void clear()
    {
        year = NullValue;
        month = NullValue;
        day = NullValue;
    }

    /**
     * Set date from an ISO8601 string, e.g. "2007-04-05".

     * @param date date specification as defined by ISO8601, e.g. "2007-04-05"
     */
    public void fromString(String date)
    {
        clear();
        try {
            DateParser parser = new DateParser(Parser.getTokenStream(date, DateLexer.class));
            parser.setDate(this);
            parser.parse();
        }
        catch (Exception exception) {
            throw new RuntimeException(
                    String.format("Failed to parse date '%s': %s", date, exception.getMessage()));
        }
    }

    /**
     * Get date as ISO8601 string
     *
     * @return string of ISO8601 date
     */
    public String toString()
    {
        int year = getYear();
        if (year == NullValue) {
            year = 0;
        }
        int month = getMonth();
        if (month == NullValue) {
            month = 0;
        }
        int day = getDay();
        if (day == NullValue) {
            day = 0;
        }
        return String.format("%04d-%02d-%02d", year, month, day);
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
        Date date = (Date) object;
        if (getYear() != NullValue && date.getYear() != NullValue && getYear() != date.getYear()) {
            return false;
        }
        if (getMonth() != NullValue && date.getMonth() != NullValue && getMonth() != date.getMonth()) {
            return false;
        }
        if (getDay() != NullValue && date.getDay() != NullValue && getDay() != date.getDay()) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Date date)
    {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (this == date) {
            return EQUAL;
        }

        int year1 = getYear();
        int year2 = date.getYear();
        if (year1 != NullValue && year2 != NullValue) {
            if (year1 < year2) {
                return BEFORE;
            }
            else if (year1 > year2) {
                return AFTER;
            }
        }

        int month1 = getMonth();
        int month2 = date.getMonth();
        if (month1 != NullValue && month2 != NullValue) {
            if (month1 < month2) {
                return BEFORE;
            }
            else if (month1 > month2) {
                return AFTER;
            }
        }

        int day1 = getDay();
        int day2 = date.getDay();
        if (day1 != NullValue && day2 != NullValue) {
            if (day1 < day2) {
                return BEFORE;
            }
            else if (day1 > day2) {
                return AFTER;
            }
        }

        return EQUAL;
    }

    /**
     * Clone date.
     *
     * @return cloned instance of date
     */
    public Date clone()
    {
        Date date = new Date();
        date.setYear(getYear());
        date.setMonth(getMonth());
        date.setDay(getDay());
        return date;
    }

    /**
     * Add to year.
     *
     * @param year
     */
    public void addYearInplace(int year)
    {
        if ( this.year == NullValue )
            throw new RuntimeException("Can't add to year because it is empty.");
        this.year += year;
    }

    /**
     * Add to month.
     *
     * @param month
     */
    public void addMonthInplace(int month)
    {
        // Update month
        if ( this.month == NullValue )
            throw new RuntimeException("Can't add to month because it is empty.");
        this.month += month;

        // Overflow to years
        this.month -= 1;
        if ( this.month < 0 || this.month >= 12 ) {
            addYearInplace(this.month / 12);
            this.month %= 12;
            if ( this.month < 0 ) {
                this.month += 12;
                addYearInplace(-1);
            }
        }
        this.month += 1;
    }

    /**
     * Add to day.
     *
     * @param day
     */

    public void addDayInplace(int day)
    {
        //To change body of created methods use File | Settings | File Templates.
    }

    /**
     * Add to year and return new date object. This object will not be modified.
     *
     * @param year
     * @return a new date
     */
    public Date addYear(int year)
    {
        Date date = clone();
        date.addYearInplace(year);
        return date;
    }

    /**
     * Add to month and return new date object. This object will not be modified.
     *
     * @param month
     * @return a new date
     */
    public Date addMonth(int month)
    {
        Date date = clone();
        date.addMonthInplace(month);
        return date;
    }

    /**
     * Add to day and return new date object. This object will not be modified.
     *
     * @param day
     * @return a new date
     */
    public Date addDay(int day)
    {
        Date date = clone();
        date.addDayInplace(day);
        return date;
    }
}
