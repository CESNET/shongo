package cz.cesnet.shongo.common;

import cz.cesnet.shongo.common.util.Parser;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.DateType;
import org.hibernate.type.StringType;
import org.hibernate.usertype.UserType;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Represents absolute date.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public final class Date implements Comparable<Date>, Cloneable, UserType
{
    /**
     * Null field value.
     */
    public static final int NullValue = Integer.MAX_VALUE;

    private final int year;
    private final int month;
    private final int day;

    /**
     * Construct date by field values.
     *
     * @param year
     * @param month
     * @param day
     */
    public Date(int year, int month, int day)
    {
        assert (year == NullValue || (year >= 0 && year <= 9999)) : "Year should be in range 1 to 9999 or empty.";
        assert (month == NullValue || (month >= 0 && month <= 12)) : "Month should be in range 1 to 12 or empty.";
        assert (day == NullValue || (day >= 0 && day <= 31)) : "Day should be in range 1 to 31 or empty.";

        this.year = (year == 0 ? NullValue : year);
        this.month = (month == 0 ? NullValue : month);
        this.day = (day == 0 ? NullValue : day);
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
        this(year, month, NullValue);
    }

    /**
     * Construct date from ISO8601 string, e.g. "2012-01-01".
     *
     * @param date
     */
    public Date(String date)
    {
        try {
            DateParser parser = new DateParser(Parser.getTokenStream(date, DateLexer.class));
            parser.parse();

            year = parser.year;
            month = parser.month;
            day = parser.day;
        }
        catch (Exception exception) {
            throw new RuntimeException(
                    String.format("Failed to parse date '%s': %s", date, exception.getMessage()));
        }
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
     * Get date month.
     *
     * @return month
     */
    public int getMonth()
    {
        return month;
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
     * Check whether all fields have NullValue.
     *
     * @return boolean
     */
    public boolean isEmpty()
    {
        return getYear() == NullValue && getMonth() == NullValue && getDay() == NullValue;
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
        if( (year == NullValue && date.year != NullValue) || (year != NullValue && date.year == NullValue)) {
            throw new AssertionError("Can't compare dates with empty year in only one of them ["
                    + toString() + ", " + date.toString() + "].");
        }
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

    @Override
    public Object clone()
    {
        return new Date(year, month, day);
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
        int resultYear = this.year;
        int resultMonth = this.month;
        int resultDay = this.day;

        if (month > 0) {
            assert (resultMonth != NullValue) : "Can't add to month because it is empty.";
            resultMonth += month;
            resultMonth -= 1;
            if (resultMonth >= 12) {
                year += resultMonth / 12;
                resultMonth %= 12;
            }
            resultMonth += 1;
        }

        if (year > 0) {
            assert (resultYear != NullValue) : "Can't add to year because it is empty.";
            resultYear += year;
        }

        if (day > 0) {
            assert (resultDay != NullValue) : "Can't add to day because it is empty.";
            assert (resultDay != NullValue) : "Can't add to day because month is empty.";
            assert (resultDay != NullValue) : "Can't add to day because year is empty.";

            // Add days by Calendar in UTC timezone
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.set(resultYear, resultMonth - 1, resultDay);
            calendar.add(Calendar.DAY_OF_MONTH, day);
            resultYear = calendar.get(Calendar.YEAR);
            resultMonth = calendar.get(Calendar.MONTH) + 1;
            resultDay = calendar.get(Calendar.DAY_OF_MONTH);
        }

        return new Date(resultYear, resultMonth, resultDay);
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
        int resultYear = this.year;
        int resultMonth = this.month;
        int resultDay = this.day;

        if (month > 0) {
            assert (resultMonth != NullValue) : "Can't add to month because it is empty.";
            resultMonth -= month;
            resultMonth -= 1;
            if (resultMonth < 0) {
                year += -resultMonth / 12;
                resultMonth %= 12;
                if (resultMonth != 0) {
                    resultMonth += 12;
                    year++;
                }
            }
            resultMonth += 1;
        }

        if (year > 0) {
            assert (resultYear != NullValue) : "Can't add to year because it is empty.";
            resultYear -= year;
        }

        if (day > 0) {
            assert (resultDay != NullValue) : "Can't add to day because it is empty.";
            assert (resultMonth != NullValue) : "Can't add to day because month is empty.";
            assert (resultYear != NullValue) : "Can't add to day because year is empty.";

            // Add days by Calendar in UTC timezone
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.set(resultYear, resultMonth - 1, resultDay);
            calendar.add(Calendar.DAY_OF_MONTH, -day);
            resultYear = calendar.get(Calendar.YEAR);
            resultMonth = calendar.get(Calendar.MONTH) + 1;
            resultDay = calendar.get(Calendar.DAY_OF_MONTH);
        }

        return new Date(resultYear, resultMonth, resultDay);
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
        int resultYear = (date.year != NullValue ? date.year : year);
        int resultMonth = (date.month != NullValue ? date.month : month);
        int resultDay = (date.day != NullValue ? date.day : day);
        return new Date(resultYear, resultMonth, resultDay);
    }

    @Override
    public int[] sqlTypes()
    {
        return new int[] { Types.VARCHAR };
    }

    @Override
    public Class returnedClass()
    {
        return Date.class;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException
    {
        if (x == null || y == null) {
            return false;
        }
        return x.equals(y);
    }

    @Override
    public int hashCode(Object x) throws HibernateException
    {
        if (x != null) {
            return x.hashCode();
        }
        return 0;
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner)
            throws HibernateException, SQLException
    {
        Object value = StringType.INSTANCE.nullSafeGet(rs, names, session, owner);
        if ( value == null ) {
            return null;
        }
        else {
            return new Date((String)value);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session)
            throws HibernateException, SQLException
    {
        if ( value == null ) {
            st.setNull(index, Types.DATE);
        }
        else {
            st.setString(index, value.toString());
        }
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException
    {
        return value;
    }

    @Override
    public boolean isMutable()
    {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException
    {
        return (Serializable) value;
    }

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException
    {
        return cached;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException
    {
        return original;
    }
}
