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

/**
 * Represents absolute time
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Time implements Comparable<Time>, Cloneable, UserType
{
    /**
     * Hour of time.
     */
    protected int hour;

    /**
     * Minute of time.
     */
    protected int minute;

    /**
     * Second of time.
     */
    protected int second;

    /**
     * Positive number means overflow, negative number means underflow which
     * was made in add or subtract operation.
     */
    protected int overflow;

    /**
     * Constructor.
     */
    public Time()
    {
        clear();
    }

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

    /**
     * Construct time by field values.
     *
     * @param hour
     * @param minute
     * @param second
     * @param overflow
     */
    public Time(int hour, int minute, int second, int overflow)
    {
        setHour(hour);
        setMinute(minute);
        setSecond(second);
        this.overflow = overflow;
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

    /**
     * Clear all fields
     */
    public void clear()
    {
        hour = 0;
        minute = 0;
        second = 0;
        overflow = 0;
    }

    /**
     * Parse time from string
     *
     * @param time
     */
    public void fromString(String time)
    {
        clear();
        try {
            TimeParser parser = new TimeParser(Parser.getTokenStream(time, TimeLexer.class));
            parser.parse();

            if (parser.hour != null) {
                setHour(parser.hour);
            }
            if (parser.minute != null) {
                setMinute(parser.minute);
            }
            if (parser.second != null) {
                setSecond(parser.second);
            }
        }
        catch (Exception exception) {
            throw new IllegalArgumentException(
                    String.format("Failed to parse time '%s': %s", time, exception.getMessage()));
        }
    }

    /**
     * @return {@link #hour}
     */
    public int getHour()
    {
        return hour;
    }

    /**
     * @param hour sets the {@link #hour}
     */
    public void setHour(int hour)
    {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("Hour should be in range 0 to 23 (" + hour + ").");
        }
        this.hour = hour;
        this.overflow = 0;
    }

    /**
     * @return {@link #minute}
     */
    public int getMinute()
    {
        return minute;
    }

    /**
     * @param minute sets the {@link #minute}
     */
    public void setMinute(int minute)
    {
        if (minute < 0 || minute > 59) {
            throw new IllegalArgumentException("Minute should be in range 0 to 59 (" + minute + ").");
        }
        this.minute = minute;
        this.overflow = 0;
    }

    /**
     * @return {@link #second}
     */
    public int getSecond()
    {
        return second;
    }

    /**
     * @param second sets the {@link #second}
     */
    public void setSecond(int second)
    {
        if (second < 0 || second > 59) {
            throw new IllegalArgumentException("Second should be in range 0 to 59 (" + second + ").");
        }
        this.second = second;
        this.overflow = 0;
    }

    /**
     * Get time as ISO8601 string.
     *
     * @return string of ISO8601 time
     */
    public String toString()
    {
        StringBuilder time = new StringBuilder();
        time.append(String.format("%02d:%02d:%02d", getHour(), getMinute(), getSecond()));
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
        result = 37 * result + getHour();
        result = 37 * result + getMinute();
        result = 37 * result + getSecond();
        return result;
    }

    @Override
    public int compareTo(Time time)
    {
        if (this == time) {
            return 0;
        }

        // Compare hours
        if (hour < time.hour) {
            return -1;
        }
        else if (hour > time.hour) {
            return 1;
        }

        // Compare minutes
        if (minute < time.minute) {
            return -1;
        }
        else if (minute > time.minute) {
            return 1;
        }

        // Compare seconds
        if (second < time.second) {
            return -1;
        }
        else if (second > this.second) {
            return 0;
        }

        return 0;
    }

    @Override
    public Time clone()
    {
        return new Time(hour, minute, second);
    }

    /**
     * Add given time to this time. This object is not modified.
     *
     * @param time
     * @return result of addition
     */
    public Time add(Time time)
    {
        return add(time.getHour(), time.getMinute(), time.getSecond());
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
            resultSecond += second;
            if (resultSecond >= 60) {
                minute += resultSecond / 60;
                resultSecond %= 60;
            }
        }

        if (minute > 0) {
            resultMinute += minute;
            if (resultMinute >= 60) {
                hour += resultMinute / 60;
                resultMinute %= 60;
            }
        }

        if (hour > 0) {
            resultHour += hour;
            if (resultHour >= 24) {
                resultOverflow = resultHour / 24;
                resultHour %= 24;
            }
        }

        Time time = this.clone();
        time.hour = resultHour;
        time.minute = resultMinute;
        time.second = resultSecond;
        time.overflow = resultOverflow;
        return time;
    }

    /**
     * Subtract given time from this time. This object is not modified.
     *
     * @param time
     * @return result of subtraction
     */
    public Time subtract(Time time)
    {
        return subtract(time.getHour(), time.getMinute(), time.getSecond());
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

        Time time = this.clone();
        time.hour = resultHour;
        time.minute = resultMinute;
        time.second = resultSecond;
        time.overflow = resultOverflow;
        return time;
    }

    /**
     * Get overflowed days.
     *
     * @return overflowed days
     */
    public int getOverflow()
    {
        if (overflow < 0) {
            throw new IllegalStateException("Overflow should be positive.");
        }
        return overflow;
    }

    /**
     * Get under-flowed days.
     *
     * @return under-flowed days
     */
    public int getUnderflow()
    {
        if (overflow > 0) {
            throw new IllegalStateException("Underflow should be negative.");
        }
        return -overflow;
    }

    @Override
    public int[] sqlTypes()
    {
        return new int[]{Types.TIME};
    }

    @Override
    public Class returnedClass()
    {
        return Time.class;
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
            return new Time((String) value);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session)
            throws HibernateException, SQLException
    {
        if (value == null) {
            st.setNull(index, Types.TIME);
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
