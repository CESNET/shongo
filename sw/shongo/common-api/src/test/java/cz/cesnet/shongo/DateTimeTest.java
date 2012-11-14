package cz.cesnet.shongo;

import org.joda.time.chrono.ISOChronology;
import org.junit.Assert;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.junit.Test;

/**
 * Tests for {@link DateTime}, {@link Period} and {@link Interval}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DateTimeTest
{
    @Test
    public void test()
    {
        // TODO: test chronology parsing
        /*DateTime dateTime1 = DateTime.parse("2012-01-01T12:00+01:00");
        DateTime dateTime2 = DateTime.parse("2012-01-01T12:00+01:00");
        Assert.assertEquals(dateTime1, dateTime2);

        // Interval isn't able to parse chronology
        Interval interval1 = Interval.parse("2012-01-01T00:00/2012-01-01T23:59");
        Interval interval2 = Interval.parse("2012-01-01T00:00/2012-01-01T23:59");
        Assert.assertEquals(interval1, interval2);*/
    }
}
