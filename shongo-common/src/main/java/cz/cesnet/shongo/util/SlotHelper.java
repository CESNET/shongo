package cz.cesnet.shongo.util;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.Date;

/**
 * Help class for print thread info
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class SlotHelper {
    public static boolean areIntervalsColliding(DateTime start, DateTime end, Interval interval)
    {
        Chronology chronology = interval.getChronology();
        Interval newInterval = new Interval(start,end);
        return areIntervalsColliding(new Interval(newInterval,chronology),interval);
    }

    public static boolean areIntervalsColliding(DateTime start, Integer minutesDuration, Integer hoursDuration,
                                                Integer daysDuration, Interval interval)
    {
        Chronology chronology = interval.getChronology();
        Interval newInterval = new Interval(start,start.plusDays(daysDuration).plusHours(hoursDuration).plusMillis(minutesDuration));
        return areIntervalsColliding(new Interval(newInterval,chronology),interval);
    }

    public static boolean areIntervalsColliding(Interval firstInterval, Interval secondInterval)
    {
        if (firstInterval == null || secondInterval == null) {
            return false;
        }

        DateTime firstStart = firstInterval.getStart();
        DateTime firstEnd = firstInterval.getEnd();
        DateTime secondStart = secondInterval.getStart();
        DateTime secondEnd = secondInterval.getEnd();

        if (firstStart == null || firstEnd == null || secondStart == null || secondEnd == null) {
            return false;
        }


        if (firstStart.equals(secondStart) || firstEnd.equals(secondEnd)) {
            return true;
        }
        if ((firstStart.isAfter(secondStart) && firstStart.isBefore(secondEnd))
                    || (firstEnd.isAfter(secondStart) && firstEnd.isBefore(secondEnd))) {
                return true;
        }
        if ((secondStart.isAfter(firstStart) && secondStart.isBefore(firstEnd))
                || (secondEnd.isAfter(firstStart) && secondEnd.isBefore(firstEnd))) {
            return true;
        }

        return false;
    }
}
