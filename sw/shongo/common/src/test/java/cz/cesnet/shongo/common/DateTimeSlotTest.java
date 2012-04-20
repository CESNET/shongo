package cz.cesnet.shongo.common;

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
    private DateTimeSlot timeSlot;

    @Before
    public void setUp() throws Exception
    {
        // Lecture on Thursdays at 12:00-14:00 in March (1.3. - 31.3.2012)
        PeriodicDateTime periodicDateTime = new PeriodicDateTime(
                new AbsoluteDateTime("2012-03-01 T12:00"), new Period("P1W"), new AbsoluteDateTime("2012-03-31"));
        timeSlot = new DateTimeSlot(periodicDateTime, new Period("PT2H"));
    }

    @Test
    public void testGetters() throws Exception
    {
        assertTrue(timeSlot.getStart() instanceof PeriodicDateTime);
        assertEquals(new Period("PT120M"), timeSlot.getDuration());
    }

    @Test
    public void testIsActive() throws Exception
    {
        for (int day = 1; day < 31; day += 7) {
            String date = String.format("2012-03-%02d", day);
            assertFalse("Should not be active " + date, timeSlot.isActive(new AbsoluteDateTime(date + " T11:59")));
            assertTrue("Should be active " + date, timeSlot.isActive(new AbsoluteDateTime(date + " T13:00")));
            assertFalse("Should not be active " + date, timeSlot.isActive(new AbsoluteDateTime(date + " T14:01")));
        }
    }

    @Test
    public void testEnumerate() throws Exception
    {
        DateTimeSlot[] correctTimeSlots = new DateTimeSlot[]{
                new DateTimeSlot(new AbsoluteDateTime("2012-03-01 T12:00"), new Period("PT2H")),
                new DateTimeSlot(new AbsoluteDateTime("2012-03-08 T12:00"), new Period("PT2H")),
                new DateTimeSlot(new AbsoluteDateTime("2012-03-15 T12:00"), new Period("PT2H")),
                new DateTimeSlot(new AbsoluteDateTime("2012-03-22 T12:00"), new Period("PT2H")),
                new DateTimeSlot(new AbsoluteDateTime("2012-03-29 T12:00"), new Period("PT2H"))
        };
        List<AbsoluteDateTimeSlot> timeSlots = timeSlot.enumerate();
        assertEquals(correctTimeSlots.length, timeSlots.size());
        for (int index = 0; index < correctTimeSlots.length; index++) {
            assertEquals(correctTimeSlots[index], timeSlots.get(index));
        }
    }

    @Test
    public void testEnumerateRange() throws Exception
    {
        DateTimeSlot[] correctTimeSlots = new DateTimeSlot[]{
                new DateTimeSlot(new AbsoluteDateTime("2012-03-08 T12:00"), new Period("PT2H")),
                new DateTimeSlot(new AbsoluteDateTime("2012-03-15 T12:00"), new Period("PT2H")),
                new DateTimeSlot(new AbsoluteDateTime("2012-03-22 T12:00"), new Period("PT2H")),
        };
        List<AbsoluteDateTimeSlot> timeSlots = timeSlot.enumerate(
                new AbsoluteDateTime("2012-03-02"), new AbsoluteDateTime("2012-03-23"));
        assertEquals(correctTimeSlots.length, timeSlots.size());
        for (int index = 0; index < correctTimeSlots.length; index++) {
            assertEquals(correctTimeSlots[index], timeSlots.get(index));
        }
    }

    @Test
    public void testGetEarliest() throws Exception
    {
        AbsoluteDateTime referenceDateTime = new AbsoluteDateTime("2012-03-08 T12:01");
        assertEquals(new DateTimeSlot(new AbsoluteDateTime("2012-03-15 T12:00"), new Period("PT2H")),
                timeSlot.getEarliest(referenceDateTime));
    }

    @Test
    public void testEquals() throws Exception
    {
        assertEquals(timeSlot, timeSlot);

        PeriodicDateTime periodicDateTime = new PeriodicDateTime(
                new AbsoluteDateTime("2012-03-01 T12:00"), new Period("P1W"), new AbsoluteDateTime("2012-03-31"));
        DateTimeSlot timeSlot = new DateTimeSlot(periodicDateTime, new Period("PT2H"));
        assertEquals(timeSlot, this.timeSlot);
    }
}
