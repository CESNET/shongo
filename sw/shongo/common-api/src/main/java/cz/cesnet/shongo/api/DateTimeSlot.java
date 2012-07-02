package cz.cesnet.shongo.api;

import org.joda.time.DateTime;
import org.joda.time.Period;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DateTimeSlot extends ComplexType
{
    @AllowedTypes({DateTime.class, PeriodicDateTime.class})
    public Object dateTime;

    public Period duration;

    public static DateTimeSlot create(Object dateTime, Period duration)
    {
        DateTimeSlot dateTimeSlot = new DateTimeSlot();
        dateTimeSlot.dateTime = dateTime;
        dateTimeSlot.duration = duration;
        return dateTimeSlot;
    }
}
