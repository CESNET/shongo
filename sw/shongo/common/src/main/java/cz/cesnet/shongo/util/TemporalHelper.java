package cz.cesnet.shongo.util;

import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Helper for manipulating/formatting temporal data types.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TemporalHelper
{
    /**
     * @param interval
     * @return formatted given {@code interval} to string
     */
    public static String formatInterval(Interval interval)
    {
        StringBuilder builder = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
        builder.append(formatter.print(interval.getStart()));
        builder.append(", ");
        builder.append(interval.toPeriod().normalizedStandard().toString());
        return builder.toString();
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
            period = period.withDays(period.getDays() + 31 * period.getMonths()).withMonths(0);
        }
        return period.toStandardDuration();
    }
}
