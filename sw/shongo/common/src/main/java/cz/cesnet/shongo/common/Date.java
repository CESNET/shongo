package cz.cesnet.shongo.common;

import cz.cesnet.shongo.common.util.Parser;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Represents absolute date.
 *
 * @author Martin Srom
 */
public class Date implements Comparable<Date>
{
    /**
     * Null field value.
     */
    public static final int NullValue = Integer.MAX_VALUE;

    private int year;
    private int month;
    private int day;

    /**
     * Construct date by field values.
     *
     * @param year
     * @param month
     * @param day
     */
    public Date(int year, int month, int day)
    {
        setYear(year);
        setMonth(month);
        setDay(day);
    }

    public Date()
    {
        this(NullValue, NullValue, NullValue);
    }

    public Date(int year)
    {
        this(year, NullValue, NullValue);
    }

    public Date(int year, int month)
    {
        this(year, NullValue, NullValue);
    }

    /**
     * Construct date from ISO8601 string, e.g. "2012-01-01".
     *
     * @param date
     */
    public Date(String date)
    {
        fromString(date);
    }

    /**
     * Get date year.
     *
     * @return year
     */
    public int getYear()
    {
        return year;
    }

    /**
     * Set date year.
     *
     * @param year
     */
    public void setYear(int year)
    {
        if (year == 0 || year == NullValue) {
            this.year = NullValue;
            return;
        }
        assert (year >= 1 && year <= 9999) : "Year should be in range 0 to 9999.";
        this.year = year;
    }

    /**
     * Get date month.
     *
     * @return month
     */
    public int getMonth()
    {
        return month;
    }

    /**
     * Set date month.
     *
     * @param month
     */
    public void setMonth(int month)
    {
        if (month == 0 || month == NullValue) {
            this.month = NullValue;
            return;
        }
        assert (month >= 1 && month <= 12) : "Month should be in range 0 to 12.";
        this.month = month;
    }

    /**
     * Get date day.
     *
     * @return day
     */
    public int getDay()
    {
        return day;
    }

    /**
     * Set date day
     *
     * @param day
     */
    public void setDay(int day)
    {
        if (day == 0 || day == NullValue) {
            this.day = NullValue;
            return;
        }
        assert (day >= 1 && day <= 31) : "Day should be in range 0 to 31.";
        this.day = day;
    }

    /**
     * Check whether all fields have NullValue.
     *
     * @return boolean
     */
    public boolean isEmpty()
    {
        return getYear() == NullValue && getMonth() == NullValue && getDay() == NullValue;
    }

    /**
     * Clear all fields.
     */
    public void setEmpty()
    {
        year = NullValue;
        month = NullValue;
        day = NullValue;
    }

    /**
     * Set date from an ISO8601 string, e.g. "2007-04-05".
     *
     * @param date date specification as defined by ISO8601, e.g. "2007-04-05"
     */
    public void fromString(String date)
    {
        setEmpty();
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
     * Get date as ISO8601 string.
     *
     * @return string of ISO8601 date
     */
    public String toString()
    {
        int year = this.year;
        if (year == NullValue) {
            year = 0;
        }
        int month = this.month;
        if (month == NullValue) {
            month = 0;
        }
        int day = this.day;
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
        if (year != date.year) {
            return false;
        }
        if (month != date.month) {
            return false;
        }
        if (day != date.day) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int result = 13;
        result = 37 * result + (year != NullValue ? year : 0);
        result = 37 * result + (month != NullValue ? month : 0);
        result = 37 * result + (day != NullValue ? day : 0);
        return result;
    }

    @Override
    public int compareTo(Date date)
    {
        if (this == date) {
            return 0;
        }

        // Fields should be both empty or nonempty
        assert ((year == NullValue && date.year == NullValue) || (year != NullValue && date.year != NullValue)) :
                "Can't compare dates with empty year in only one of them.";
        assert ((month == NullValue && date.month == NullValue) || (month != NullValue && date.month != NullValue)) :
                "Can't compare dates with empty month in only one of them.";
        assert ((day == NullValue && date.day == NullValue) || (day != NullValue && date.day != NullValue)) :
                "Can't compare dates with empty day in only one of them.";

        // Compare years
        if (year != NullValue && date.year != NullValue) {
            if (year < date.year) {
                return -1;
            }
            else if (year > date.year) {
                return 1;
            }
        }
        else {
            assert (month == NullValue && date.month == NullValue) : "Can't compare months with empty year.";
            assert (day == NullValue && date.day == NullValue) : "Can't compare days with empty year.";
        }

        // Compare months
        if (month != NullValue && date.month != NullValue) {
            if (month < date.month) {
                return -1;
            }
            else if (month > date.month) {
                return 1;
            }
        }
        else {
            assert (day == NullValue && date.day == NullValue) : "Can't compare days with empty year.";
        }

        // Compare days
        if (day != NullValue && date.day != NullValue) {
            if (day < date.day) {
                return -1;
            }
            else if (day > date.day) {
                return 1;
            }
        }

        return 0;
    }

    /**
     * Checks whether this date equals the given date by skipping
     * all empty fields (in this or given date).
     *
     * @param date
     * @return true if this date matches the given date,
     *         false otherwise
     */
    public boolean match(Date date)
    {
        if (this == date) {
            return true;
        }
        if (date == null) {
            return false;
        }
        if (year != NullValue && date.year != NullValue && year != date.year) {
            return false;
        }
        if (month != NullValue && date.month != NullValue && month != date.month) {
            return false;
        }
        if (day != NullValue && date.day != NullValue && day != date.day) {
            return false;
        }
        return true;
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
     * Add given date to this date. This object is not modified.
     *
     * @param date
     * @return result of addition
     */
    public Date add(Date date)
    {
        int year = (date.year != NullValue ? date.year : 0);
        int month = (date.month != NullValue ? date.month : 0);
        int day = (date.day != NullValue ? date.day : 0);
        return add(year, month, day);
    }

    /**
     * Add given date to this date. This object is not modified.
     *
     * @param year
     * @param month
     * @param day
     * @return result of addition
     */
    public Date add(int year, int month, int day)
    {
        Date result = clone();

        if (month > 0) {
            assert (result.month != NullValue) : "Can't add to month because it is empty.";
            result.month += month;
            result.month -= 1;
            if (result.month >= 12) {
                year += result.month / 12;
                result.month %= 12;
            }
            result.month += 1;
        }

        if (year > 0) {
            assert (result.year != NullValue) : "Can't add to year because it is empty.";
            result.year += year;
        }

        if (day > 0) {
            assert (result.day != NullValue) : "Can't add to day because it is empty.";
            assert (result.month != NullValue) : "Can't add to day because month is empty.";
            assert (result.year != NullValue) : "Can't add to day because year is empty.";

            // Add days by Calendar in UTC timezone
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.set(result.year, result.month - 1, result.day);
            calendar.add(Calendar.DAY_OF_MONTH, day);
            result.year = calendar.get(Calendar.YEAR);
            result.month = calendar.get(Calendar.MONTH) + 1;
            result.day = calendar.get(Calendar.DAY_OF_MONTH);
        }

        return result;
    }

    /**
     * Subtract given date from this date. This object is not modified.
     *
     * @param date
     * @return result of subtraction
     */
    public Date subtract(Date date)
    {
        int year = (date.year != NullValue ? date.year : 0);
        int month = (date.month != NullValue ? date.month : 0);
        int day = (date.day != NullValue ? date.day : 0);
        return subtract(year, month, day);
    }

    /**
     * Subtract given date from this date. This object is not modified.
     *
     * @param year
     * @param month
     * @param day
     * @return result of subtraction
     */
    public Date subtract(int year, int month, int day)
    {
        Date result = clone();

        if (month > 0) {
            assert (result.month != NullValue) : "Can't add to month because it is empty.";
            result.month -= month;
            result.month -= 1;
            if (result.month < 0) {
                year += -result.month / 12;
                result.month %= 12;
                if (result.month != 0) {
                    result.month += 12;
                    year++;
                }
            }
            result.month += 1;
        }

        if (year > 0) {
            assert (result.year != NullValue) : "Can't add to year because it is empty.";
            result.year -= year;
        }

        if (day > 0) {
            assert (result.day != NullValue) : "Can't add to day because it is empty.";
            assert (result.month != NullValue) : "Can't add to day because month is empty.";
            assert (result.year != NullValue) : "Can't add to day because year is empty.";

            // Add days by Calendar in UTC timezone
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.set(result.year, result.month - 1, result.day);
            calendar.add(Calendar.DAY_OF_MONTH, -day);
            result.year = calendar.get(Calendar.YEAR);
            result.month = calendar.get(Calendar.MONTH) + 1;
            result.day = calendar.get(Calendar.DAY_OF_MONTH);
        }

        return result;
    }

    /**
     * Merge this date with given date and return
     * result. This and given date stay unchanged.
     * <p/>
     * The returned date contains values from this date
     * replaced by non-empty values from given date.
     *
     * @param date Date to merge
     */
    public Date merge(Date date)
    {
        Date result = new Date();
        result.year = (date.year != NullValue ? date.year : year);
        result.month = (date.month != NullValue ? date.month : month);
        result.day = (date.day != NullValue ? date.day : day);
        return result;
    }
}
