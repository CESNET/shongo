package cz.cesnet.shongo.util;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link DateTimeFormatter#roundDuration},
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DateTimeFormatterTest
{
    /**
     * Test for {@link DateTimeFormatter#roundDuration},
     *
     * @throws Exception
     */
    @Test
    public void testRoundPeriod() throws Exception
    {
        Assert.assertEquals(Period.months(2),
                DateTimeFormatter.roundDuration(
                        Period.months(1).withWeeks(4).withDays(2).withHours(23).withMinutes(59).withSeconds(59)));

        Assert.assertEquals(Period.days(1).withHours(2),
                DateTimeFormatter.roundDuration(Period.hours(25).withMinutes(59).withSeconds(20)));

        Assert.assertEquals(Period.years(2).withWeeks(1),
                DateTimeFormatter.roundDuration(Period.years(2).withWeeks(1).withDays(3)));

        Assert.assertEquals(Period.seconds(5),
                DateTimeFormatter.roundDuration(Period.seconds(5)));
    }
}
