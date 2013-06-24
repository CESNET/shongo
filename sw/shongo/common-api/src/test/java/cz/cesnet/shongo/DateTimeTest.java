package cz.cesnet.shongo;

import cz.cesnet.shongo.api.Converter;
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
    public void testTimeZones() throws Exception
    {
        Temporal.initialize();
        DateTimeZone defaultZone = DateTimeZone.getDefault();

        // DateTime.parse parses to given timezone
        DateTimeZone.setDefault(DateTimeZone.forID("+11:00"));
        DateTime dateTime = DateTime.parse("2012-01-01T12:00+04:00");
        Assert.assertEquals("+04:00", dateTime.getChronology().getZone().getID());
        DateTimeZone.setDefault(defaultZone);

        // Interval.parse parses to default timezone (Java runtime timezone)
        DateTimeZone.setDefault(DateTimeZone.forID("+11:00"));
        Interval intervalByDefault = Interval.parse("2012-01-01T06:00+05:00/2012-01-01T08:00+06:00");
        Assert.assertEquals("2012-01-01T12:00:00.000+11:00", intervalByDefault.getStart().toString());
        Assert.assertEquals("2012-01-01T13:00:00.000+11:00", intervalByDefault.getEnd().toString());
        DateTimeZone.setDefault(defaultZone);

        // Converter.Atomic.convertStringToDateTime parses to default timezone (Java runtime timezone)
        DateTimeZone.setDefault(DateTimeZone.forID("+00:00"));
        DateTime dateTimeByConverter =
                Converter.convertStringToDateTime("2012-01-01T06:00+05:00");
        Assert.assertEquals("2012-01-01T01:00:00.000Z", dateTimeByConverter.toString());
        DateTimeZone.setDefault(defaultZone);

        // Converter.Atomic.convertStringToInterval parses to default timezone (Java runtime timezone)
        DateTimeZone.setDefault(DateTimeZone.forID("+00:00"));
        Interval intervalByConverter =
                Converter.convertStringToInterval("2012-01-01T06:00+05:00/2012-01-01T08:00+06:00");
        Assert.assertEquals("2012-01-01T01:00:00.000Z", intervalByConverter.getStart().toString());
        Assert.assertEquals("2012-01-01T02:00:00.000Z", intervalByConverter.getEnd().toString());
        DateTimeZone.setDefault(defaultZone);
    }
}
