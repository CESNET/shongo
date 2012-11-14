package cz.cesnet.shongo.controller.common;

import cz.cesnet.shongo.controller.request.DateTimeSlotSpecification;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.*;

/**
 * Time slot tests
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DateTimeSlotTest
{
    private DateTimeSlotSpecification timeSlot;

    @Before
    public void setUp() throws Exception
    {
        // Lecture on Thursdays at 12:00-14:00 in March (1.3. - 31.3.2012)
        PeriodicDateTimeSpecification periodicDateTime = new PeriodicDateTimeSpecification(
                DateTime.parse("2012-03-01T12:00"), Period.parse("P1W"), LocalDate.parse("2012-03-31"));
        timeSlot = new DateTimeSlotSpecification(periodicDateTime, Period.parse("PT2H"));
    }

    @Test
    public void testGetters() throws Exception
    {
        assertTrue(timeSlot.getStart() instanceof PeriodicDateTimeSpecification);
        assertEquals(Period.parse("PT120M").normalizedStandard(), timeSlot.getDuration().normalizedStandard());
    }

    @Test
    public void testIsActive() throws Exception
    {
        for (int day = 1; day < 31; day += 7) {
            String date = String.format("2012-03-%02d", day);
            assertFalse("Should not be active " + date, timeSlot.isActive(DateTime.parse(date + "T11:59")));
            assertTrue("Should be active " + date, timeSlot.isActive(DateTime.parse(date + "T13:00")));
            assertFalse("Should not be active " + date, timeSlot.isActive(DateTime.parse(date + "T14:01")));
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
        assertEquals(correctTimeSlots.length, timeSlots.size());
        for (int index = 0; index < correctTimeSlots.length; index++) {
            assertEquals(correctTimeSlots[index], timeSlots.get(index));
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
        assertEquals(correctTimeSlots.length, timeSlots.size());
        for (int index = 0; index < correctTimeSlots.length; index++) {
            assertEquals(correctTimeSlots[index], timeSlots.get(index));
        }
    }

    @Test
    public void testGetEarliest() throws Exception
    {
        DateTime referenceDateTime = DateTime.parse("2012-03-08T12:01");
        assertEquals(new Interval(DateTime.parse("2012-03-15T12:00"), Period.parse("PT2H")),
                timeSlot.getEarliest(referenceDateTime));
    }

    @Test
    public void testEquals() throws Exception
    {
        assertEquals(timeSlot, timeSlot);

        PeriodicDateTimeSpecification periodicDateTime = new PeriodicDateTimeSpecification(
                DateTime.parse("2012-03-01T12:00"), Period.parse("P1W"), LocalDate.parse("2012-03-31"));
        DateTimeSlotSpecification timeSlot = new DateTimeSlotSpecification(periodicDateTime, new Period("PT2H"));
        assertEquals(timeSlot, this.timeSlot);
    }
}
