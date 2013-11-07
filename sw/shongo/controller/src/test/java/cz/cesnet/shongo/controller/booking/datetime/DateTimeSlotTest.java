package cz.cesnet.shongo.controller.booking.datetime;

import cz.cesnet.shongo.controller.booking.datetime.DateTimeSlot;
import cz.cesnet.shongo.controller.booking.datetime.PeriodicDateTime;
import cz.cesnet.shongo.controller.booking.datetime.PeriodicDateTimeSlot;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

/**
 * Time slot tests
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DateTimeSlotTest
{
    private DateTimeSlot timeSlot;

    @Before
    public void setUp() throws Exception
    {
        // Lecture on Thursdays at 12:00-14:00 in March (1.3. - 31.3.2012)
        PeriodicDateTime periodicDateTime = new PeriodicDateTime(
                DateTime.parse("2012-03-01T12:00"), Period.parse("P1W"), LocalDate.parse("2012-03-31"));
        timeSlot = new PeriodicDateTimeSlot(periodicDateTime, Period.parse("PT2H"));
    }

    @Test
    public void testGetters() throws Exception
    {
        Assert.assertEquals(Period.parse("PT120M").normalizedStandard(), timeSlot.getDuration().normalizedStandard());
    }

    @Test
    public void testIsActive() throws Exception
    {
        for (int day = 1; day < 31; day += 7) {
            String date = String.format("2012-03-%02d", day);
            Assert.assertFalse("Should not be active " + date, timeSlot.isActive(DateTime.parse(date + "T11:59")));
            Assert.assertTrue("Should be active " + date, timeSlot.isActive(DateTime.parse(date + "T13:00")));
            Assert.assertFalse("Should not be active " + date, timeSlot.isActive(DateTime.parse(date + "T14:01")));
        }
    }

    @Test
    public void testEnumerate() throws Exception
    {
        Interval[] correctTimeSlots = new Interval[]{
                new Interval(DateTime.parse("2012-03-01T12:00"), Period.parse("PT2H")),
                new Interval(DateTime.parse("2012-03-08T12:00"), Period.parse("PT2H")),
                new Interval(DateTime.parse("2012-03-15T12:00"), Period.parse("PT2H")),
                new Interval(DateTime.parse("2012-03-22T12:00"), Period.parse("PT2H")),
                new Interval(DateTime.parse("2012-03-29T12:00"), Period.parse("PT2H"))
        };
        List<Interval> timeSlots = timeSlot.enumerate();
        Assert.assertEquals(correctTimeSlots.length, timeSlots.size());
        for (int index = 0; index < correctTimeSlots.length; index++) {
            Assert.assertEquals(correctTimeSlots[index], timeSlots.get(index));
        }
    }

    @Test
    public void testEnumerateRange() throws Exception
    {
        Interval[] correctTimeSlots = new Interval[]{
                new Interval(DateTime.parse("2012-03-08T12:00"), Period.parse("PT2H")),
                new Interval(DateTime.parse("2012-03-15T12:00"), Period.parse("PT2H")),
                new Interval(DateTime.parse("2012-03-22T12:00"), Period.parse("PT2H")),
        };
        List<Interval> timeSlots = timeSlot.enumerate(
                DateTime.parse("2012-03-02"), DateTime.parse("2012-03-23"));
        Assert.assertEquals(correctTimeSlots.length, timeSlots.size());
        for (int index = 0; index < correctTimeSlots.length; index++) {
            Assert.assertEquals(correctTimeSlots[index], timeSlots.get(index));
        }
    }

    @Test
    public void testGetEarliest() throws Exception
    {
        DateTime referenceDateTime = DateTime.parse("2012-03-08T14:01");
        Assert.assertEquals(new Interval(DateTime.parse("2012-03-15T12:00"), Period.parse("PT2H")),
                timeSlot.getEarliest(referenceDateTime));
    }

    @Test
    public void testEquals() throws Exception
    {
        Assert.assertEquals(timeSlot, timeSlot);

        PeriodicDateTime periodicDateTime = new PeriodicDateTime(
                DateTime.parse("2012-03-01T12:00"), Period.parse("P1W"), LocalDate.parse("2012-03-31"));
        DateTimeSlot timeSlot = new PeriodicDateTimeSlot(periodicDateTime, new Period("PT2H"));
        Assert.assertEquals(timeSlot, this.timeSlot);
    }
}
