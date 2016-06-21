package cz.cesnet.shongo;

import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormat;
import org.joda.time.format.PeriodFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.Date;

/**
 * Helper for manipulating/formatting temporal data types.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Temporal
{
    private static Logger logger = LoggerFactory.getLogger(Temporal.class);

    /**
     * Represents an infinity period.
     */
    public static final Period PERIOD_INFINITY = Period.years(9998);

    /**
     * The minimum allowed {@link org.joda.time.DateTime} value.
     */
    public static final DateTime DATETIME_INFINITY_START = DateTime.parse("0001-01-01T00:00:00.000");
    /**
     * The maximum allowed {@link org.joda.time.DateTime} value.
     */
    public static final DateTime DATETIME_INFINITY_END = DateTime.parse("9999-01-01T00:00:00.000");

    /**
     * The interval which contains all allowed values.
     */
    public static final Interval INTERVAL_INFINITE = new Interval(DATETIME_INFINITY_START, DATETIME_INFINITY_END);

    /**
     * String which represents a infinite date/time.
     */
    public static final String INFINITY_ALIAS = "*";

    /**
     * {@link PeriodFormatter} for {@link Period}s.
     */
    private static final PeriodFormatter periodFormatter = PeriodFormat.getDefault();

    /**
     * {@link DateTimeFormatter} for {@link DateTime}s.
     */
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Static initialization.
     */
    static {
        logger.debug("Initializing minimum/maximum date/time to {}/{}.",
                DATETIME_INFINITY_START, DATETIME_INFINITY_END);
    }

    /**
     * Empty method used for referencing the {@link Temporal} to be statically initialized before time zone changes
     * are applied (e.g., for unit tests).
     */
    public static void initialize()
    {
    }

    /**
     * @return rounded {@link org.joda.time.DateTime#now()} to seconds
     */
    public static DateTime nowRoundedToSeconds()
    {
        return DateTime.now().withField(DateTimeFieldType.millisOfSecond(), 0);
    }

    /**
     * @return rounded {@link org.joda.time.DateTime#now()} to minutes
     */
    public static DateTime nowRoundedToMinutes()
    {
        return DateTime.now()
                .withField(DateTimeFieldType.secondOfMinute(), 0)
                .withField(DateTimeFieldType.millisOfSecond(), 0);
    }

    /**
     * @return rounded {@link org.joda.time.DateTime#now()} to hours
     */
    public static DateTime nowRoundedToHours()
    {
        return DateTime.now()
                .withField(DateTimeFieldType.minuteOfHour(), 0)
                .withField(DateTimeFieldType.secondOfMinute(), 0)
                .withField(DateTimeFieldType.millisOfSecond(), 0);
    }

    /**
     * @param interval for which the duration should be returned
     * @return duration of given {@code interval} or {@link #PERIOD_INFINITY} if interval specifies
     *         {@link #DATETIME_INFINITY_START} or {@link #DATETIME_INFINITY_END}
     */
    public static Period getIntervalDuration(Interval interval)
    {
        if (interval.getStartMillis() == DATETIME_INFINITY_START.getMillis() ||
                interval.getEndMillis() == DATETIME_INFINITY_END.getMillis()) {
            return PERIOD_INFINITY;
        }
        return interval.toPeriod();
    }

    /**
     * Convert Joda DateTime to SQL Timestamp.
     *
     * @param dateTime to convert
     * @return Timestamp for NativeQuery
     */
    public static Timestamp convertDateTimeToTimestamp(DateTime dateTime)
    {
        return new Timestamp(dateTime.getMillis());
    }

    /**
     * @param interval1
     * @param interval2
     * @return true if both interval are equaled (without chronology match),
     *         false otherwise
     */
    public static boolean isIntervalEqualed(Interval interval1, Interval interval2)
    {
        if (interval1.getStartMillis() != interval2.getStartMillis()) {
            return false;
        }
        if (interval1.getEndMillis() != interval2.getEndMillis()) {
            return false;
        }
        return true;
    }

    /**
     * @param period
     * @param longerThanPeriod
     * @return true if {@code period} is longer than {@code longerThanPeriod},
     *         false otherwise
     */
    public static boolean isPeriodLongerThan(Period period, Period longerThanPeriod)
    {
        if (longerThanPeriod == null) {
            return false;
        }
        return convertPeriodToStandardDuration(period).isLongerThan(convertPeriodToStandardDuration(longerThanPeriod));
    }

    /**
     * @param period to be converted
     * @return given {@code period} converted to standard {@link Duration}
     */
    private static Duration convertPeriodToStandardDuration(Period period)
    {
        if (period.getYears() > 0) {
            period = period.withDays(period.getDays() + 365 * period.getYears()).withYears(0);
        }
        if (period.getMonths() > 0) {
            period = period.withDays(period.getDays() + 30 * period.getMonths()).withMonths(0);
        }
        return period.toStandardDuration();
    }

    /**
     * @param dateTimes from which the minimum should be returned
     * @return minimum date/time from given {@code dateTimes}
     */
    public static DateTime min(DateTime... dateTimes)
    {
        DateTime minDateTime = null;
        for (DateTime dateTime : dateTimes) {
            if (minDateTime == null || dateTime.isBefore(minDateTime)) {
                minDateTime = dateTime;
            }
        }
        return minDateTime;
    }

    /**
     * @param dateTimes from which the maximum should be returned
     * @return maximum date/time from given {@code dateTimes}
     */
    public static DateTime max(DateTime... dateTimes)
    {
        DateTime maxDateTime = null;
        for (DateTime dateTime : dateTimes) {
            if (maxDateTime == null || dateTime.isAfter(maxDateTime)) {
                maxDateTime = dateTime;
            }
        }
        return maxDateTime;
    }

    /**
     * @param dateTime
     * @param minutes
     * @return rounded given {@code dateTime} to {@code minutes}
     */
    public static DateTime roundDateTimeToMinutes(final DateTime dateTime, final int minutes)
    {
        if (minutes < 1 || 60 % minutes != 0) {
            throw new IllegalArgumentException("Minutes must be a factor of 60.");
        }
        final DateTime hour = dateTime.hourOfDay().roundFloorCopy();
        final long millisSinceHour = new Duration(hour, dateTime).getMillis();
        final int roundedMinutes = ((int) Math.ceil(millisSinceHour / 60000.0 / minutes)) * minutes;
        return hour.plusMinutes(roundedMinutes);
    }

    public static Interval roundIntervalToDays(final Interval interval)
    {
        DateTime start = new LocalDate(interval.getStartMillis()).toDateTimeAtStartOfDay();
        DateTime end = new LocalDate(interval.getEndMillis()).toDateTimeAtStartOfDay().plusDays(1);

        return new Interval(start, end);
    }

    /**
     * Checks if given date is between given interval, including edges (rounded to whole days)
     *
     * @param start Start of the interval
     * @param end End of the interval
     * @param date Date to check
     *
     * @return
     */
    public static boolean dateFitsInterval(DateTime start, ReadablePartial end, LocalDate date)
    {
        if (start == null || end == null || date == null) {
            throw new IllegalArgumentException("Parameters must not be null.");
        }

        LocalDate slotStart = start.toLocalDate();
        LocalDate slotEnd = new LocalDate(end);

        return (slotStart.isBefore(date) && slotEnd.isAfter(date)) || slotStart.equals(date) || slotEnd.equals(date);
    }

//    public static DateTime getDayInMonth(DateTime startDate, int orderOfDay, int dayInWeek)
//    {
//        if (orderOfDay < 1 || orderOfDay > 4) {
//            throw new IllegalStateException("Parameter orderOfDay has to be value of 1-4.");
//        }
//        if (dayInWeek < 1 || dayInWeek > 6) {
//        throw new IllegalStateException("Parameter dayInMonth has to be value of 1-6 (Sunday to Saturday.");
//    }
//    }
//    public static DateTime getSlotStart()
//    {
//
//    }
}
