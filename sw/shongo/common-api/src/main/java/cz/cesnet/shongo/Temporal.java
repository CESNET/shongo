package cz.cesnet.shongo;

import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormat;
import org.joda.time.format.PeriodFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public static DateTime nowRounded()
    {
        return DateTime.now().withField(DateTimeFieldType.millisOfSecond(), 0);
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

    private static final int YEARS = 1;
    private static final int MONTHS = 2;
    private static final int WEEKS = 4;
    private static final int DAYS = 8;
    private static final int HOURS = 16;
    private static final int MINUTES = 32;


    /**
     * @param period to be rounded
     * @return rounded given {@code period}
     */
    public static Period roundPeriod(Period period)
    {
        period = period.normalizedStandard();

        int years = period.getYears();
        int months = period.getMonths();
        int weeks = period.getWeeks();
        int days = period.getDays();
        int hours = period.getHours();
        int minutes = period.getMinutes();

        // Specifies which field are non-zero
        int nonZeroFields = (years > 0 ? YEARS : 0) | (months > 0 ? MONTHS : 0) | (weeks > 0 ? WEEKS : 0)
                | (days > 0 ? DAYS : 0) | (hours > 0 ? HOURS : 0) | (minutes > 0 ? MINUTES : 0);

        // Seconds are not needed if the period is longer than minute
        if ((nonZeroFields & (YEARS | MONTHS | WEEKS | DAYS | HOURS | MINUTES)) != 0) {
            if (period.getSeconds() >= 30) {
                minutes++;
            }
        }

        // Make hour from minutes
        if (minutes == 60) {
            hours++;
            minutes = 0;
        }
        // Minutes are not needed if the period is longer than day
        if ((nonZeroFields & (YEARS | MONTHS | WEEKS | DAYS)) != 0) {
            if (minutes >= 30) {
                hours++;
            }
            minutes = 0;
        }

        // Make day from hours
        if (hours == 24) {
            days++;
            hours = 0;
        }
        // Hours are not needed if the period is longer than week
        if ((nonZeroFields & (YEARS | MONTHS | WEEKS)) != 0) {
            if (hours >= 12) {
                days++;
            }
            hours = 0;
        }

        // Make week from days
        if (days == 7) {
            weeks++;
            days = 0;
        }
        // Days are not needed if the period is longer than month
        if ((nonZeroFields & (YEARS | MONTHS)) != 0) {
            if (days >= 4) {
                weeks++;
            }
            days = 0;
        }

        // Make month from weeks
        if (weeks == 4) {
            months++;
            weeks = 0;
        }

        return new Period(years, months, weeks, days, hours, minutes, 0, 0);
    }

    /**
     * @param dateTime to be formatted
     * @return formatted given {@code dateTime} to {@link String}
     */
    public static String formatDateTime(DateTime dateTime)
    {
        if (dateTime == null) {
            return "null";
        }
        return dateTimeFormatter.print(dateTime);
    }

    /**
     * @param period to be formatted
     * @return formatted given {@code period} to {@link String}
     */
    public static String formatPeriod(Period period)
    {
        if (period == null) {
            return "null";
        }
        return periodFormatter.print(period);
    }

    /**
     * @param interval to be formatted
     * @return formatted given {@code interval} to string
     */
    public static String formatInterval(Interval interval)
    {
        if (interval == null) {
            return "null";
        }
        StringBuilder builder = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
        builder.append(formatter.print(interval.getStart()));
        builder.append(", ");
        builder.append(interval.toPeriod().normalizedStandard().toString());
        return builder.toString();
    }
}
