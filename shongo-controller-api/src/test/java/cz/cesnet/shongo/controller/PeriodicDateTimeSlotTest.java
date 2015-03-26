package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Tests for {@link cz.cesnet.shongo.controller.api.PeriodicDateTimeSlot}.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class PeriodicDateTimeSlotTest
{
    @Test
    public void testSortedSlots()
    {
        SortedSet<PeriodicDateTimeSlot> slots = new TreeSet<PeriodicDateTimeSlot>();

        PeriodicDateTimeSlot periodicDateTime1 = new PeriodicDateTimeSlot();
        periodicDateTime1.setStart(DateTime.parse("2012-03-01T12:00"));
        periodicDateTime1.setPeriod(Period.parse("P1W"));
        periodicDateTime1.setEnd(new LocalDate("2012-03"));


        PeriodicDateTimeSlot periodicDateTime2 = new PeriodicDateTimeSlot();
        periodicDateTime2.setStart(DateTime.parse("2012-03-02T12:00"));
        periodicDateTime2.setPeriod(Period.parse("P1W"));
        periodicDateTime2.setEnd(new LocalDate("2012-03"));

        PeriodicDateTimeSlot periodicDateTime3 = new PeriodicDateTimeSlot();
        periodicDateTime3.setStart(DateTime.parse("2012-03-03T12:00"));
        periodicDateTime3.setPeriod(Period.parse("P1W"));
        periodicDateTime3.setEnd(new LocalDate("2012-04"));

        slots.add(periodicDateTime1);
        slots.add(periodicDateTime2);
        slots.add(periodicDateTime3);
        Assert.assertEquals(periodicDateTime1, slots.first());

        slots.clear();

        slots.add(periodicDateTime3);
        slots.add(periodicDateTime1);
        slots.add(periodicDateTime2);

        Assert.assertEquals(periodicDateTime1, slots.first());
    }
}
