package cz.cesnet.shongo.util;

import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Helper for manipulating/formatting temporal data types.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TemporalHelper
{
    public static String formatInterval(Interval interval)
    {
        StringBuilder builder = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm");
        builder.append(formatter.print(interval.getStart()));
        builder.append(", ");
        builder.append(interval.toPeriod().normalizedStandard().toString());
        return builder.toString();
    }
}
