package cz.cesnet.shongo.common;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.StringType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Represents absolute date that can be partially filled (e.g., only year can be set and it equals
 * and compares to all dates with the same year).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public final class PartialDate extends Date
{
    /**
     * Null field value.
     */
    public static final int NullValue = -1;

    /**
     * Constructor for date with all fields empty.
     */
    public PartialDate()
    {
        this(NullValue, NullValue, NullValue);
    }

    /**
     * Constructor for date with month and day empty.
     *
     * @param year
     */
    public PartialDate(int year)
    {
        this(year, NullValue, NullValue);
    }

    /**
     * Constructor for date with day empty.
     *
     * @param year
     * @param month
     */
    public PartialDate(int year, int month)
    {
        this(year, month, NullValue);
    }

    /**
     * Constructor for date.
     *
     * @param year
     * @param month
     * @param day
     */
    public PartialDate(int year, int month, int day)
    {
        super(year, month, day);
    }

    /**
     * Construct date from ISO8601 string, e.g. "2012-01-01".
     *
     * @param date
     */
    public PartialDate(String date)
    {
        super(date);
    }

    @Override
    protected void clear()
    {
        setYear(NullValue);
        setMonth(NullValue);
        setDay(NullValue);
    }

    @Override
    public int getYear()
    {
        return (year != NullValue ? year : 0);
    }

    @Override
    public void setYear(int year)
    {
        if (year != NullValue && (year < 0 || year > 9999)) {
            throw new IllegalArgumentException("Year should be in range 1 to 9999 or empty(" + year + ").");
        }
        this.year = (year == 0 ? NullValue : year);
    }

    @Override
    public int getMonth()
    {
        return (month != NullValue ? month : 0);
    }

    @Override
    public void setMonth(int month)
    {
        if (month != NullValue && (month < 0 || month > 12)) {
            throw new IllegalArgumentException("Month should be in range 1 to 12 or empty(" + month + ").");
        }
        this.month = (month == 0 ? NullValue : month);
    }

    @Override
    public int getDay()
    {
        return (day != NullValue ? day : 0);
    }

    @Override
    public void setDay(int day)
    {
        if (day != NullValue && (day < 0 || day > 31)) {
            throw new IllegalArgumentException("Day should be in range 1 to 31 or empty(" + day + ").");
        }
        this.day = (day == 0 ? NullValue : day);
    }

    /**
     * @return true if year field is empty.
     */
    public boolean isYearEmpty()
    {
        return year == NullValue;
    }

    /**
     * @return true if month field is empty.
     */
    public boolean isMonthEmpty()
    {
        return month == NullValue;
    }

    /**
     * @return true if day field is empty.
     */
    public boolean isDayEmpty()
    {
        return day == NullValue;
    }

    /**
     * @return true if all fields are empty, otherwise false
     */
    public boolean isEmpty()
    {
        return year == NullValue && month == NullValue && day == NullValue;
    }

    @Override
    public String toString()
    {
        return String.format("%04d-%02d-%02d", getYear(), getMonth(), getDay());
    }

    @Override
    public boolean equals(Object object)
    {
        throw new RuntimeException("TODO: Implement");
    }

    @Override
    public int compareTo(Date date)
    {
        if (this == date) {
            return 0;
        }

        if (true) {
            throw new RuntimeException("TODO: Implement");
        }

        // Fields should be both empty or nonempty
        if ((year == NullValue && date.year != NullValue) || (year != NullValue && date.year == NullValue)) {
            throw new IllegalStateException("Can't compare dates with empty year in only one of them ["
                    + toString() + ", " + date.toString() + "].");
        }
        if ((month == NullValue && date.month != NullValue) || (month != NullValue && date.month == NullValue)) {
            throw new IllegalStateException("Can't compare dates with empty month in only one of them ["
                    + toString() + ", " + date.toString() + "].");
        }
        if ((day == NullValue && date.day != NullValue) || (day != NullValue && date.day == NullValue)) {
            throw new IllegalStateException("Can't compare dates with empty day in only one of them ["
                    + toString() + ", " + date.toString() + "].");
        }

        // Check years
        if (year == NullValue) {
            if (month != NullValue) {
                throw new IllegalStateException("Can't compare month with empty year. ("
                        + toString() + ", " + date.toString() + ").");
            }
            if (day != NullValue) {
                throw new IllegalStateException("Can't compare day with empty year. ("
                        + toString() + ", " + date.toString() + ").");
            }
        }

        // Check months
        if (month == NullValue) {
            if (day != NullValue) {
                throw new IllegalStateException("Can't compare day with empty month. ("
                        + toString() + ", " + date.toString() + ").");
            }
        }

        return super.compareTo(date);
    }

    @Override
    public PartialDate clone()
    {
        return new PartialDate(year, month, day);
    }

    @Override
    public PartialDate add(int year, int month, int day)
    {
        if (month > 0) {
            if (this.month == NullValue) {
                throw new IllegalStateException("Can't add to month because it is empty.");
            }
        }

        if (year > 0) {
            if (this.year == NullValue) {
                throw new IllegalStateException("Can't add to year because it is empty.");
            }
        }

        if (day > 0) {
            if (this.day == NullValue) {
                throw new IllegalStateException("Can't add to day because it is empty.");
            }
            if (this.month == NullValue) {
                throw new IllegalStateException("Can't add to day because month is empty.");
            }
            if (this.year == NullValue) {
                throw new IllegalStateException("Can't add to day because year is empty.");
            }
        }

        return (PartialDate) super.add(year, month, day);
    }

    @Override
    public PartialDate subtract(int year, int month, int day)
    {
        if (month > 0) {
            if (this.month == NullValue) {
                throw new IllegalStateException("Can't subtract from month because it is empty.");
            }
        }

        if (year > 0) {
            if (this.year == NullValue) {
                throw new IllegalStateException("Can't subtract from year because it is empty.");
            }
        }

        if (day > 0) {
            if (this.day == NullValue) {
                throw new IllegalStateException("Can't subtract from day because it is empty.");
            }
            if (this.month == NullValue) {
                throw new IllegalStateException("Can't subtract from day because month is empty.");
            }
            if (this.year == NullValue) {
                throw new IllegalStateException("Can't subtract from day because year is empty.");
            }
        }

        return (PartialDate) super.add(year, month, day);
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
    public PartialDate merge(Date date)
    {
        int resultYear = (date.year != NullValue ? date.year : year);
        int resultMonth = (date.month != NullValue ? date.month : month);
        int resultDay = (date.day != NullValue ? date.day : day);
        return new PartialDate(resultYear, resultMonth, resultDay);
    }

    @Override
    public int[] sqlTypes()
    {
        return new int[]{Types.VARCHAR};
    }

    @Override
    public Class returnedClass()
    {
        return PartialDate.class;
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
            return new PartialDate((String) value);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session)
            throws HibernateException, SQLException
    {
        if (value == null) {
            st.setNull(index, Types.VARCHAR);
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
