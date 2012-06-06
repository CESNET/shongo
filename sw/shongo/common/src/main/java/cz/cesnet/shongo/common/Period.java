package cz.cesnet.shongo.common;

import cz.cesnet.shongo.common.util.Parser;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.StringType;
import org.hibernate.usertype.UserType;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.print.DocFlavor;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Represents a duration or period.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Period implements Comparable<Period>, UserType
{
    private int year = 0;
    private int month = 0;
    private int day = 0;
    private int week = 0;
    private int hour = 0;
    private int minute = 0;
    private int second = 0;

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

    public int getWeek()
    {
        return week;
    }

    public void setWeek(int week)
    {
        this.week = week;
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

    /**
     * Constructs a zero period
     */
    public Period()
    {
    }

    /**
     * Constructs a new period from a given ISO 8601 duration string, e.g. "P3Y6M4DT12H30M5S".
     *
     * @param period a period string as defined by ISO8601, except decimal fractions, which are not supported
     */
    public Period(String period)
    {
        fromString(period);
    }

    /**
     * Set Period from a given ISO 8601 duration string, e.g. "P3Y6M4DT12H30M5S".
     *
     * @param period a period string as defined by ISO 8601, except decimal fractions, which are not supported
     */
    public void fromString(String period)
    {
        try {
            PeriodParser parser = new PeriodParser(Parser.getTokenStream(period, PeriodLexer.class));
            parser.setPeriod(this);
            parser.parse();
        }
        catch (Exception exception) {
            throw new RuntimeException(
                    String.format("Failed to parse period '%s': %s", period, exception.getMessage()));
        }
        normalize();
    }

    /**
     * Get period as ISO 8601 duration, e.g. "P3Y6M4DT12H30M5S".
     * <p/>
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
        if (getYear() != 0) {
            period.append(getYear() + "Y");
        }
        if (getMonth() != 0) {
            period.append(getMonth() + "M");
        }
        if (getDay() != 0) {
            period.append(getDay() + "D");
        }
        StringBuilder time = new StringBuilder();
        if (getHour() != 0) {
            time.append(getHour() + "H");
        }
        if (getMinute() != 0) {
            time.append(getMinute() + "M");
        }
        if (getSecond() != 0) {
            time.append(getSecond() + "S");
        }
        if (time.length() > 0) {
            period.append("T");
            period.append(time);
        }
        if (period.length() == 0) {
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
        if (second >= 60) {
            minute += second / 60;
            second %= 60;
        }
        if (minute >= 60) {
            hour += minute / 60;
            minute %= 60;
        }
        if (hour >= 24) {
            day += hour / 24;
            hour %= 24;
        }
        if (month >= 12) {
            year += month / 12;
            month %= 12;
        }
        if (week > 0) {
            day += week * 7;
            week = 0;
        }
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
        Period period = (Period) object;
        normalize();
        period.normalize();
        if (year != period.year || month != period.month || day != period.day) {
            return false;
        }
        if (hour != period.hour || minute != period.minute || second != period.second) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode()
    {
        int result = 13;
        result = 37 * result + year;
        result = 37 * result + month;
        result = 37 * result + day;
        result = 37 * result + hour;
        result = 37 * result + minute;
        result = 37 * result + second;
        return result;
    }

    @Override
    public int compareTo(Period period)
    {
        normalize();
        period.normalize();

        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (this == period) {
            return EQUAL;
        }

        int year1 = getYear();
        int year2 = period.getYear();
        if (year1 < year2) {
            return BEFORE;
        }
        else if (year1 > year2) {
            return AFTER;
        }

        int month1 = getMonth();
        int month2 = period.getMonth();
        if (month1 < month2) {
            return BEFORE;
        }
        else if (month1 > month2) {
            return AFTER;
        }

        int day1 = getDay();
        int day2 = period.getDay();
        if (day1 < day2) {
            return BEFORE;
        }
        else if (day1 > day2) {
            return AFTER;
        }

        int hour1 = getHour();
        int hour2 = period.getHour();
        if (hour1 < hour2) {
            return BEFORE;
        }
        else if (hour1 > hour2) {
            return AFTER;
        }

        int minute1 = getMinute();
        int minute2 = period.getMinute();
        if (minute1 < minute2) {
            return BEFORE;
        }
        else if (minute1 > minute2) {
            return AFTER;
        }

        int second1 = getSecond();
        int second2 = period.getSecond();
        if (second1 < second2) {
            return BEFORE;
        }
        else if (second1 > second2) {
            return AFTER;
        }

        return EQUAL;
    }

    /**
     * Adds a period of time to this period and returns the resulting period.
     * <p/>
     * Does not modify this object.
     *
     * @param period time period to add to this one
     * @return a new period - sum of this and given period
     */
    public Period add(Period period)
    {
        Period resultPeriod = new Period();
        resultPeriod.setYear(getYear() + period.getYear());
        resultPeriod.setMonth(getMonth() + period.getMonth());
        resultPeriod.setDay(getDay() + period.getDay());
        resultPeriod.setWeek(getWeek() + period.getWeek());
        resultPeriod.setHour(getHour() + period.getHour());
        resultPeriod.setMinute(getMinute() + period.getMinute());
        resultPeriod.setSecond(getSecond() + period.getSecond());
        resultPeriod.normalize();
        return resultPeriod;
    }

    @Override
    public int[] sqlTypes()
    {
        return new int[] { Types.VARCHAR };
    }

    @Override
    public Class returnedClass()
    {
        return Period.class;
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
        String value = (String) StringType.INSTANCE.nullSafeGet(rs, names, session, owner);
        if ( value == null ) {
            return null;
        }
        else {
            return new Period(value);
        }
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session)
            throws HibernateException, SQLException
    {
        if ( value == null ) {
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
