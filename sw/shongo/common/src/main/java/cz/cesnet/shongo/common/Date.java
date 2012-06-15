package cz.cesnet.shongo.common;

import cz.cesnet.shongo.common.util.Parser;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.StringType;
import org.hibernate.usertype.UserType;

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
public class Date implements Comparable<Date>, Cloneable, UserType
{
    /**
     * Year of date.
     */
    protected int year;

    /**
     * Month of date.
     */
    protected int month;

    /**
     * Day of date.
     */
    protected int day;

    /**
     * Constructor.
     */
    public Date()
    {
        clear();
    }

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
     * Clear all fields
     */
    protected void clear()
    {
        year = 1;
        month = 1;
        day = 1;
    }

    /**
     * Parse date from string
     *
     * @param date
     */
    public void fromString(String date)
    {
        clear();
        try {
            DateParser parser = new DateParser(Parser.getTokenStream(date, DateLexer.class));
            parser.parse();
            if (parser.year != null) {
                setYear(parser.year);
            }
            if (parser.month != null) {
                setMonth(parser.month);
            }
            if (parser.day != null) {
                setDay(parser.day);
            }
        }
        catch (Exception exception) {
            throw new IllegalArgumentException(
                    String.format("Failed to parse date '%s': %s", date, exception.getMessage()));
        }
    }

    /**
     * @return {@link #year}
     */
    public int getYear()
    {
        return year;
    }

    /**
     * @param year sets the {@link #year}
     */
    public void setYear(int year)
    {
        if (year <= 0 || year > 9999) {
            throw new IllegalArgumentException("Year should be in range 1 to 9999 (" + year + ").");
        }
        this.year = year;
    }

    /**
     * @return {@link #month}
     */
    public int getMonth()
    {
        return month;
    }

    /**
     * @param month sets the {@link #month}
     */
    public void setMonth(int month)
    {
        if (month <= 0 || month > 12) {
            throw new IllegalArgumentException("Month should be in range 1 to 12 (" + month + ").");
        }
        this.month = month;
    }

    /**
     * @return {@link #day}
     */
    public int getDay()
    {
        return day;
    }

    /**
     * @param day sets the {@link #day}
     */
    public void setDay(int day)
    {
        if (day <= 0 || day > 31) {
            throw new IllegalArgumentException("Day should be in range 1 to 31 (" + day + ").");
        }

        this.day = day;
    }

    /**
     * Get date as ISO8601 string.
     *
     * @return string of ISO8601 date
     */
    public String toString()
    {
        return String.format("%04d-%02d-%02d", getYear(), getMonth(), getDay());
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
        result = 37 * result + getYear();
        result = 37 * result + getMonth();
        result = 37 * result + getDay();
        return result;
    }

    @Override
    public int compareTo(Date date)
    {
        if (this == date) {
            return 0;
        }

        // Compare years
        if (year < date.year) {
            return -1;
        }
        else if (year > date.year) {
            return 1;
        }

        // Compare months
        if (month < date.month) {
            return -1;
        }
        else if (month > date.month) {
            return 1;
        }

        // Compare days
        if (day < date.day) {
            return -1;
        }
        else if (day > date.day) {
            return 1;
        }

        return 0;
    }

    @Override
    public Date clone()
    {
        return new Date(year, month, day);
    }

    /**
     * Add given date to this date. This object is not modified.
     *
     * @param date
     * @return result of addition
     */
    public Date add(Date date)
    {
        return add(date.getYear(), date.getMonth(), date.getDay());
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
            resultMonth += month;
            resultMonth -= 1;
            if (resultMonth >= 12) {
                year += resultMonth / 12;
                resultMonth %= 12;
            }
            resultMonth += 1;
        }

        if (year > 0) {
            resultYear += year;
        }

        if (day > 0) {
            // Add days by Calendar in UTC timezone
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.set(resultYear, resultMonth - 1, resultDay);
            calendar.add(Calendar.DAY_OF_MONTH, day);
            resultYear = calendar.get(Calendar.YEAR);
            resultMonth = calendar.get(Calendar.MONTH) + 1;
            resultDay = calendar.get(Calendar.DAY_OF_MONTH);
        }

        Date date = this.clone();
        date.year = resultYear;
        date.month = resultMonth;
        date.day = resultDay;
        return date;
    }

    /**
     * Subtract given date from this date. This object is not modified.
     *
     * @param date
     * @return result of subtraction
     */
    public Date subtract(Date date)
    {
        return subtract(date.getYear(), date.getMonth(), date.getDay());
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
            resultYear -= year;
        }

        if (day > 0) {
            // Add days by Calendar in UTC timezone
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            calendar.set(resultYear, resultMonth - 1, resultDay);
            calendar.add(Calendar.DAY_OF_MONTH, -day);
            resultYear = calendar.get(Calendar.YEAR);
            resultMonth = calendar.get(Calendar.MONTH) + 1;
            resultDay = calendar.get(Calendar.DAY_OF_MONTH);
        }

        Date date = this.clone();
        date.year = resultYear;
        date.month = resultMonth;
        date.day = resultDay;
        return date;
    }

    @Override
    public int[] sqlTypes()
    {
        return new int[]{Types.DATE};
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
        if (value == null) {
            return null;
        }
        else {
            return new Date((String) value);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session)
            throws HibernateException, SQLException
    {
        if (value == null) {
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
