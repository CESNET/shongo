package cz.cesnet.shongo.util;

import cz.cesnet.shongo.TodoImplementException;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.PeriodFormat;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Formatter of date/times to user.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DateTimeFormatter
{
    public static final Type SHORT = Type.SHORT;
    public static final Type LONG = Type.LONG;

    /**
     * @param type
     * @return {@link DateTimeFormatter} of given {@code type}
     */
    public static DateTimeFormatter getInstance(Type type)
    {
        return new DateTimeFormatter(type);
    }

    /**
     * @param type
     * @param locale
     * @param dateTimeZone
     * @return {@link DateTimeFormatter} of given {@code type} and for given {@code locale}
     */
    public static DateTimeFormatter getInstance(Type type, Locale locale, DateTimeZone dateTimeZone)
    {
        return new DateTimeFormatter(type).with(locale, dateTimeZone);
    }

    /**
     * Time formatter.
     */
    private final org.joda.time.format.DateTimeFormatter timeFormatter;

    /**
     * Date formatter.
     */
    private final org.joda.time.format.DateTimeFormatter dateFormatter;

    /**
     * Date/time formatter.
     */
    private final org.joda.time.format.DateTimeFormatter dateTimeFormatter;

    /**
     * Period formatter.
     */
    private final org.joda.time.format.PeriodFormatter periodFormatter;

    /**
     * Timezone.
     */
    private final DateTimeZone dateTimeZone;

    /**
     * Constructor.
     *
     * @param type
     */
    private DateTimeFormatter(Type type)
    {
        switch (type) {
            case SHORT:
                this.timeFormatter = DateTimeFormat.forStyle("-S");
                this.dateFormatter = DateTimeFormat.forStyle("M-");
                this.dateTimeFormatter = DateTimeFormat.forStyle("MS");
                break;
            default:
                this.timeFormatter = DateTimeFormat.forStyle("-M");
                this.dateFormatter = DateTimeFormat.forStyle("M-");
                this.dateTimeFormatter = DateTimeFormat.forStyle("MM");
                break;
        }
        this.dateTimeZone = DateTimeZone.getDefault();
        this.periodFormatter = PeriodFormat.getDefault();
    }

    /**
     * Constructor.
     *
     * @param dateTimeFormatter
     * @param locale
     */
    private DateTimeFormatter(DateTimeFormatter dateTimeFormatter, Locale locale, DateTimeZone dateTimeZone)
    {
        this.timeFormatter = dateTimeFormatter.timeFormatter.withLocale(locale);
        this.dateFormatter = dateTimeFormatter.dateFormatter.withLocale(locale);
        this.dateTimeFormatter = dateTimeFormatter.dateTimeFormatter.withLocale(locale);
        this.dateTimeZone = (dateTimeZone != null ? dateTimeZone : DateTimeZone.getDefault());
        this.periodFormatter = PeriodFormat.wordBased(locale);
    }

    /**
     * @param locale
     * @return {@link DateTimeFormatter} for given {@code locale}
     */
    public DateTimeFormatter with(Locale locale)
    {
        return new DateTimeFormatter(this, locale, this.dateTimeZone);
    }

    /**
     * @param locale
     * @param dateTimeZone
     * @return {@link DateTimeFormatter} for given {@code locale} and {@code dateTimeZone}
     */
    public DateTimeFormatter with(Locale locale, DateTimeZone dateTimeZone)
    {
        return new DateTimeFormatter(this, locale, dateTimeZone);
    }

    /**
     * @param dateTime
     * @return formatted given {@code dateTime} as time
     */
    public String formatTime(DateTime dateTime)
    {
        if (dateTime == null) {
            return "";
        }
        return timeFormatter.print(dateTime.withZone(dateTimeZone));
    }

    /**
     * @param dateTime
     * @return formatted given {@code dateTime} as date
     */
    public String formatDate(DateTime dateTime)
    {
        if (dateTime == null) {
            return "";
        }
        return dateFormatter.print(dateTime.withZone(dateTimeZone));
    }

    /**
     * @param readablePartial
     * @return formatted given {@code readablePartial} as date
     */
    public String formatDate(ReadablePartial readablePartial)
    {
        if (readablePartial == null) {
            return "";
        }
        return dateFormatter.print(readablePartial);
    }

    /**
     * @param dateTime
     * @return formatted given {@code dateTime} as date and time
     */
    public String formatDateTime(DateTime dateTime)
    {
        if (dateTime == null) {
            return "";
        }
        return dateTimeFormatter.print(dateTime.withZone(dateTimeZone));
    }

    /**
     * @param interval
     * @return formatted given {@code interval}
     */
    public String formatInterval(Interval interval)
    {
        if (interval == null) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        DateTime start = interval.getStart().withZone(dateTimeZone);
        DateTime end = interval.getEnd().withZone(dateTimeZone);
        if (start.withTimeAtStartOfDay().equals(end.withTimeAtStartOfDay())) {
            stringBuilder.append(dateFormatter.print(start));
            stringBuilder.append(" ");
            stringBuilder.append(timeFormatter.print(start));
            stringBuilder.append(" - ");
            stringBuilder.append(timeFormatter.print(end));
        }
        else {
            stringBuilder.append(dateTimeFormatter.print(start));
            stringBuilder.append(" - ");
            stringBuilder.append(dateTimeFormatter.print(end));
        }
        return stringBuilder.toString();
    }

    /**
     * @param interval
     * @return formatted given {@code interval} as date range
     */
    public String formatIntervalDate(Interval interval)
    {
        if (interval == null) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(dateFormatter.print(interval.getStart().withZone(dateTimeZone)));
        stringBuilder.append(" - ");
        stringBuilder.append(dateFormatter.print(interval.getEnd().withZone(dateTimeZone)));
        return stringBuilder.toString();
    }

    /**
     * @param interval
     * @return formatted given {@code interval} as two lines
     */
    public String formatIntervalMultiLine(Interval interval)
    {
        if (interval == null) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        DateTime start = interval.getStart().withZone(dateTimeZone);
        DateTime end = interval.getEnd().withZone(dateTimeZone);
        if (start.withTimeAtStartOfDay().equals(end.withTimeAtStartOfDay())) {
            stringBuilder.append("<div class='date-time'><table><tr><td>");
            stringBuilder.append(dateFormatter.print(start));
            stringBuilder.append("</td><td>");
            stringBuilder.append(timeFormatter.print(start));
            stringBuilder.append("</td></tr><tr><td></td><td>");
            stringBuilder.append(timeFormatter.print(end));
            stringBuilder.append("</td></tr></table></div>");
        }
        else {
            stringBuilder.append(dateTimeFormatter.print(start));
            stringBuilder.append("<br/>");
            stringBuilder.append(dateTimeFormatter.print(end));
        }
        return stringBuilder.toString();
    }

    /**
     * @param duration
     * @return formatted given {@code duration}
     */
    public String formatDuration(Period duration)
    {
        return periodFormatter.print(roundDuration(duration));
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
    public static Period roundDuration(Period period)
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
     * Type of the {@link DateTimeFormatter}.
     */
    public static enum Type
    {
        /**
         * Time seconds are invisible.
         */
        SHORT,

        /**
         * Time seconds are visible.
         */
        LONG
    }
}
