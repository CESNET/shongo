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
 * Represents absolute time that can be partially filled (e.g., only hour can be set and it equals
 * and compares to all time with the same hour).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public final class PartialTime extends Time
{
    /**
     * Null field value.
     */
    public static final int NullValue = Integer.MAX_VALUE;

    /**
     * Constructor for time with all fields empty.
     */
    public PartialTime()
    {
        this(NullValue, NullValue, NullValue, 0);
    }

    /**
     * Constructor for time with all fields empty except hour.
     *
     * @param hour
     */
    public PartialTime(int hour)
    {
        this(hour, NullValue, NullValue, 0);
    }

    /**
     * Constructor for time with second empty.
     *
     * @param hour
     * @param minute
     */
    public PartialTime(int hour, int minute)
    {
        this(hour, minute, NullValue, 0);
    }

    /**
     * Constructor for time.
     *
     * @param hour
     * @param minute
     * @param second
     */
    public PartialTime(int hour, int minute, int second)
    {
        this(hour, minute, second, 0);
    }

    /**
     * Construct time by field values.
     *
     * @param hour
     * @param minute
     * @param second
     * @param overflow
     */
    public PartialTime(int hour, int minute, int second, int overflow)
    {
        super(hour, minute, second, overflow);
    }

    /**
     * Construct time from ISO8601 string, e.g. "12:00:00".
     *
     * @param time
     */
    public PartialTime(String time)
    {
        super(time);
    }

    @Override
    public void clear()
    {
        hour = NullValue;
        minute = NullValue;
        second = NullValue;
        overflow = 0;
    }

    @Override
    public int getHour()
    {
        return (hour != NullValue ? hour : 0);
    }

    @Override
    public void setHour(int hour)
    {
        if (hour != NullValue && (hour < 0 || hour > 23)) {
            throw new IllegalArgumentException("Hour should be in range 0 to 23 or empty (" + hour + ").");
        }
        this.hour = hour;
    }

    @Override
    public int getMinute()
    {
        return (minute != NullValue ? minute : 0);
    }

    @Override
    public void setMinute(int minute)
    {
        if (minute != NullValue && (minute < 0 || minute > 59)) {
            throw new IllegalArgumentException("Minute should be in range 0 to 59 or empty (" + minute + ").");
        }
        this.minute = minute;
    }

    @Override
    public int getSecond()
    {
        return (second != NullValue ? second : 0);
    }

    @Override
    public void setSecond(int second)
    {
        if (second != NullValue && (second < 0 || second > 59)) {
            throw new IllegalArgumentException("Second should be in range 0 to 59 or empty (" + second + ").");
        }
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

    @Override
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
        throw new RuntimeException("TODO: Implement");
    }

    @Override
    public int compareTo(Time time)
    {
        if (this == time) {
            return 0;
        }

        if (true) {
            throw new RuntimeException("TODO: Implement");
        }

        // Fields should be both empty or nonempty
        if (hour == NullValue && time.hour != NullValue || hour != NullValue && time.hour == NullValue) {
            throw new IllegalStateException("Can't compare times with empty hour in only one of them ("
                    + toString() + ", " + time.toString() + ").");
        }
        if (minute == NullValue && time.minute != NullValue || minute != NullValue && time.minute == NullValue) {
            throw new IllegalStateException("Can't compare times with empty minute in only one of them ("
                    + toString() + ", " + time.toString() + ").");
        }
        if (second == NullValue && time.second != NullValue || second != NullValue && time.second == NullValue) {
            throw new IllegalStateException("Can't compare times with empty second in only one of them ("
                    + toString() + ", " + time.toString() + ").");
        }

        // Compare hours
        if (hour == NullValue) {
            if (minute != NullValue || time.minute != NullValue) {
                throw new IllegalStateException("Can't compare minutes with empty hours. ("
                        + toString() + ", " + time.toString() + ").");
            }
            if (second != NullValue || time.second != NullValue) {
                throw new IllegalStateException("Can't compare seconds with empty hours. ("
                        + toString() + ", " + time.toString() + ").");
            }
        }

        // Compare minutes
        if (minute == NullValue) {
            if (second != NullValue || time.second != NullValue) {
                throw new IllegalStateException("Can't compare seconds with empty minutes. ("
                        + toString() + ", " + time.toString() + ").");
            }
        }

        return super.compareTo(time);
    }

    @Override
    public PartialTime clone()
    {
        return new PartialTime(hour, minute, second);
    }

    @Override
    public PartialTime add(int hour, int minute, int second)
    {
        if (second > 0) {
            if (this.second == NullValue) {
                throw new IllegalStateException("Can't add to seconds because it is empty.");
            }
        }

        if (minute > 0) {
            if (this.minute == NullValue) {
                throw new IllegalStateException("Can't add to minutes because it is empty.");
            }
        }

        if (hour > 0) {
            if (this.hour == NullValue) {
                throw new IllegalStateException("Can't add to hours because it is empty.");
            }
        }

        return (PartialTime) super.add(hour, minute, second);
    }

    @Override
    public PartialTime subtract(int hour, int minute, int second)
    {
        if (second > 0) {
            if (this.second == NullValue) {
                throw new IllegalStateException("Can't subtract from seconds because it is empty.");
            }
        }

        if (minute > 0) {
            if (this.minute == NullValue) {
                throw new IllegalStateException("Can't subtract from minutes because it is empty.");
            }
        }

        if (hour > 0) {
            if (this.hour == NullValue) {
                throw new IllegalStateException("Can't subtract from hours because it is empty.");
            }
        }

        return new PartialTime(hour, minute, second);
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
    public PartialTime merge(Time time)
    {
        int resultHour = (time.hour != NullValue ? time.hour : hour);
        int resultMinute = (time.minute != NullValue ? time.minute : minute);
        int resultSecond = (time.second != NullValue ? time.second : second);
        return new PartialTime(resultHour, resultMinute, resultSecond);
    }

    @Override
    public int[] sqlTypes()
    {
        return new int[]{Types.VARCHAR};
    }

    @Override
    public Class returnedClass()
    {
        return PartialTime.class;
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
            return new PartialTime((String) value);
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
