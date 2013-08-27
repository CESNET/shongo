package cz.cesnet.shongo.client.web.models;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.ReadablePartial;
import org.joda.time.format.DateTimeFormat;

import java.util.Locale;

/**
 * Date/time formatter.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DateTimeFormatter
{
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
     * @return {@link DateTimeFormatter} of given {@code type} and for given {@code locale}
     */
    public static DateTimeFormatter getInstance(Type type, Locale locale)
    {
        return new DateTimeFormatter(type).withLocale(locale);
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
    }

    /**
     * Constructor.
     *
     * @param dateTimeFormatter
     * @param locale
     */
    private DateTimeFormatter(DateTimeFormatter dateTimeFormatter, Locale locale)
    {
        this.timeFormatter = dateTimeFormatter.timeFormatter.withLocale(locale);
        this.dateFormatter = dateTimeFormatter.dateFormatter.withLocale(locale);
        this.dateTimeFormatter = dateTimeFormatter.dateTimeFormatter.withLocale(locale);
    }

    /**
     * @param locale
     * @return {@link DateTimeFormatter} for given {@code locale}
     */
    public DateTimeFormatter withLocale(Locale locale)
    {
        return new DateTimeFormatter(this, locale);
    }

    /**
     * @param dateTime
     * @return formatted given {@code dateTime} as time
     */
    public String formatTime(DateTime dateTime)
    {
        return timeFormatter.print(dateTime);
    }

    /**
     * @param dateTime
     * @return formatted given {@code dateTime} as date
     */
    public String formatDate(DateTime dateTime)
    {
        return dateFormatter.print(dateTime);
    }

    /**
     * @param readablePartial
     * @return formatted given {@code readablePartial} as date
     */
    public String formatDate(ReadablePartial readablePartial)
    {
        return dateFormatter.print(readablePartial);
    }

    /**
     * @param dateTime
     * @return formatted given {@code dateTime} as date and time
     */
    public String formatDateTime(DateTime dateTime)
    {
        return dateTimeFormatter.print(dateTime);
    }

    /**
     * @param interval
     * @return formatted given {@code interval}
     */
    public String formatInterval(Interval interval)
    {
        StringBuilder stringBuilder = new StringBuilder();
        DateTime start = interval.getStart();
        DateTime end = interval.getEnd();
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(dateFormatter.print(interval.getStart()));
        stringBuilder.append(" - ");
        stringBuilder.append(dateFormatter.print(interval.getEnd()));
        return stringBuilder.toString();
    }

    /**
     * @param interval
     * @return formatted given {@code interval} as two lines
     */
    public String formatIntervalMultiLine(Interval interval)
    {
        StringBuilder stringBuilder = new StringBuilder();
        DateTime start = interval.getStart();
        DateTime end = interval.getEnd();
        if (start.withTimeAtStartOfDay().equals(end.withTimeAtStartOfDay())) {
            stringBuilder.append("<table class=\"date-time\"><tr><td>");
            stringBuilder.append(dateFormatter.print(start));
            stringBuilder.append("</td><td>");
            stringBuilder.append(timeFormatter.print(start));
            stringBuilder.append("</td></tr><tr><td></td><td>");
            stringBuilder.append(timeFormatter.print(end));
            stringBuilder.append("</td></tr></table>");
        }
        else {
            stringBuilder.append(dateTimeFormatter.print(start));
            stringBuilder.append("<br/>");
            stringBuilder.append(dateTimeFormatter.print(end));
        }
        return stringBuilder.toString();
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
