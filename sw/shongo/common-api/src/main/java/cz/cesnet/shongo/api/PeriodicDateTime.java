package cz.cesnet.shongo.api;

import org.joda.time.DateTime;
import org.joda.time.Period;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PeriodicDateTime extends ComplexType
{
    public DateTime start;

    public Period period;

    public static PeriodicDateTime create(DateTime start, Period period)
    {
        PeriodicDateTime periodicDateTime = new PeriodicDateTime();
        periodicDateTime.start = start;
        periodicDateTime.period = period;
        return periodicDateTime;
    }
}
