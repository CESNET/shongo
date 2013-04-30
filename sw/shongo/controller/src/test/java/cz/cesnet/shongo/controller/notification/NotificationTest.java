package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.Temporal;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link Notification},
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class NotificationTest
{
    /**
     * Test for {@link cz.cesnet.shongo.Temporal#roundPeriod},
     *
     * @throws Exception
     */
    @Test
    public void testRoundPeriod() throws Exception
    {
        Assert.assertEquals(Period.months(2), Temporal.roundPeriod(
                Period.months(1).withWeeks(4).withDays(2).withHours(23).withMinutes(59).withSeconds(59)));

        Assert.assertEquals(Period.days(1).withHours(2), Temporal.roundPeriod(
                Period.hours(25).withMinutes(59).withSeconds(20)));

        Assert.assertEquals(Period.years(2).withWeeks(1), Temporal.roundPeriod(
                Period.years(2).withWeeks(1).withDays(3)));
    }
}
