package cz.cesnet.shongo;

import cz.cesnet.shongo.api.util.Converter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link DateTime}, {@link Period} and {@link Interval}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DateTimeTest
{
    @Test
    public void testChronologyParsing() throws Exception
    {
        DateTimeZone oldDefaultZone = DateTimeZone.getDefault();
        DateTimeZone newDefaultZone = DateTimeZone.forID("+11:00");
        DateTimeZone.setDefault(newDefaultZone);

        DateTime dateTime = DateTime.parse("2012-01-01T12:00+04:00");
        Assert.assertEquals("+04:00", dateTime.getChronology().getZone().getID());

        // Interval.parse isn't able to parse chronology
        Interval intervalByDefault = Interval.parse("2012-01-01T00:00+05:00/2012-02-02T00:00+06:00");
        Assert.assertEquals("+11:00", intervalByDefault.getStart().getChronology().getZone().getID());
        Assert.assertEquals("+11:00", intervalByDefault.getEnd().getChronology().getZone().getID());

        // Converter is able to parse chronology
        Interval intervalByConverter =
                Converter.Atomic.convertStringToInterval("2012-01-01T00:00+05:00/2012-01-01T02:00+06:00");
        Assert.assertEquals("+05:00", intervalByConverter.getStart().getChronology().getZone().getID());
        Assert.assertEquals("+05:00", intervalByConverter.getEnd().getChronology().getZone().getID());
        Assert.assertEquals(0, intervalByConverter.getStart().getHourOfDay());
        Assert.assertEquals(1, intervalByConverter.getEnd().getHourOfDay());

        DateTimeZone.setDefault(oldDefaultZone);
    }
}
