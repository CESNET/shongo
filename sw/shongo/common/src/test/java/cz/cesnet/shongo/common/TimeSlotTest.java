package cz.cesnet.shongo.common;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * Time slot tests
 *
 * @author Martin Srom
 */
public class TimeSlotTest
{
    private TimeSlot timeSlot;

    @Before
    public void setUp() throws Exception
    {
        // Lecture on Thursdays at 12:00-14:00 in March (1.3. - 31.3.2012)
        PeriodicDateTime periodicDateTime = new PeriodicDateTime(
                new AbsoluteDateTime("2012-03-01 T12:00"), new Period("P1W"), new AbsoluteDateTime("2012-03-31"));
        timeSlot = new TimeSlot(periodicDateTime, new Period("P2H"));
    }

    @Test
    public void testGetters() throws Exception
    {
        assertTrue(timeSlot.getDateTime() instanceof PeriodicDateTime);
        assertEquals(timeSlot.getDuration(), new Period("P60M"));
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
        TimeSlot[] correctTimeSlots = new TimeSlot[]{
                new TimeSlot(new AbsoluteDateTime("2012-03-01 T12:00"), new Period("P2H")),
                new TimeSlot(new AbsoluteDateTime("2012-03-08 T12:00"), new Period("P2H")),
                new TimeSlot(new AbsoluteDateTime("2012-03-15 T12:00"), new Period("P2H")),
                new TimeSlot(new AbsoluteDateTime("2012-03-22 T12:00"), new Period("P2H")),
                new TimeSlot(new AbsoluteDateTime("2012-03-29 T12:00"), new Period("P2H"))
        };
        TimeSlot[] timeSlots = timeSlot.enumerate();
        assertEquals(timeSlots.length, correctTimeSlots.length);
        for (int index = 0; index < correctTimeSlots.length; index++) {
            assertEquals(timeSlots[index], correctTimeSlots[index]);
        }
    }

    @Test
    public void testEnumerateRange() throws Exception
    {
        TimeSlot[] correctTimeSlots = new TimeSlot[]{
                new TimeSlot(new AbsoluteDateTime("2012-03-08 T12:00"), new Period("P2H")),
                new TimeSlot(new AbsoluteDateTime("2012-03-15 T12:00"), new Period("P2H")),
                new TimeSlot(new AbsoluteDateTime("2012-03-22 T12:00"), new Period("P2H")),
        };
        TimeSlot[] timeSlots = timeSlot.enumerate(
                new AbsoluteDateTime("2012-03-02"), new AbsoluteDateTime("2012-03-23"));
        assertEquals(timeSlots.length, correctTimeSlots.length);
        for (int index = 0; index < correctTimeSlots.length; index++) {
            assertEquals(timeSlots[index], correctTimeSlots[index]);
        }
    }

    @Test
    public void testGetEarliest() throws Exception
    {
        AbsoluteDateTime referenceDateTime = new AbsoluteDateTime("2012-01-08 T12:01");
        assertEquals(timeSlot.getEarliest(referenceDateTime), new TimeSlot(
                new AbsoluteDateTime("2012-03-22 T12:00"), new Period("P2H")));
    }

    @Test
    public void testEquals() throws Exception
    {
        PeriodicDateTime periodicDateTime = new PeriodicDateTime(
                new AbsoluteDateTime("2012-03-01 T12:00"), new Period("P1W"), new AbsoluteDateTime("2012-03-31"));
        TimeSlot timeSlot = new TimeSlot(periodicDateTime, new Period("P2H"));
        assertEquals(this.timeSlot, timeSlot);
    }
}
