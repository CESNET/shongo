package cz.cesnet.shongo.common;

import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * Time tests
 *
 * @author Martin Srom
 */
public class TimeTest
{
    @Test
    public void testGetters() throws Exception
    {
        Time time = new Time("12:01:02");
        assertEquals(12, time.getHour());
        assertEquals(01, time.getMinute());
        assertEquals(02, time.getSecond());
    }

    @Test
    public void testSetters() throws Exception
    {
        Time time = new Time();
        time.setHour(13);
        time.setMinute(03);
        time.setSecond(04);
        assertEquals(new Time("13:03:04"), time);
    }

    @Test
    public void testIsEmpty() throws Exception
    {
        assertTrue(new Time().isEmpty());
        assertFalse(new Time("12:01:01").isEmpty());
    }

    @Test
    public void testClear() throws Exception
    {
        Time time = new Time("12:01:01");
        time.clear();
        assertTrue(time.isEmpty());
    }

    @Test
    public void testFromString() throws Exception
    {
        assertEquals(new Time("12:01:02"), new Time("120102"));
        assertEquals(new Time("12:01"), new Time("1201"));
        assertEquals(new Time("12"), new Time("12"));
    }

    @Test
    public void testToString() throws Exception
    {
        assertEquals("12:01:02", new Time("120102").toString());
        assertEquals("12:01", new Time("1201").toString());
        assertEquals("12", new Time("12").toString());
    }

    @Test
    public void testEquals() throws Exception
    {
        assertEquals(new Time("12:01:02"), new Time("120102"));
        assertEquals(new Time("12:01"), new Time("120101"));
        assertEquals(new Time("12:01"), new Time("120102"));
        assertEquals(new Time("12"), new Time("120101"));
        assertEquals(new Time("12"), new Time("120201"));
    }

    @Test
    public void testCompareTo() throws Exception
    {
        Time time1 = new Time("12:01:01");
        Time time2 = new Time("12:01");
        Time time3 = new Time("12");
        Time time4 = new Time("11:01:01");
        Time time5 = new Time("12:01:02");

        assertEquals(0, time1.compareTo(time2));
        assertEquals(0, time2.compareTo(time1));
        assertEquals(0, time2.compareTo(time3));
        assertEquals(0, time3.compareTo(time2));
        assertEquals(0, time1.compareTo(time3));
        assertEquals(0, time3.compareTo(time1));

        assertEquals(-1, time4.compareTo(time5));
        assertEquals(1, time5.compareTo(time4));
    }

    @Test
    public void testAdd() throws Exception
    {
        assertEquals(new Time("01:01:00"), new Time("23:59:59").addHour(1).addMinute(1).addSecond(1));
        assertEquals(new Time("02:02:02"), new Time("23:59:59").addHour(49).addMinute(61).addSecond(63));
    }

    public void testAddOverflow() throws Exception
    {
        assertEquals(1, new Time("23:59:59").addHourInplace(1));
        assertEquals(0, new Time("22:59:59").addHourInplace(1));
        assertEquals(1, new Time("23:59:59").addMinuteInplace(1));
        assertEquals(0, new Time("23:58:59").addMinuteInplace(1));
        assertEquals(1, new Time("23:59:59").addSecondInplace(1));
        assertEquals(0, new Time("23:59:58").addSecondInplace(1));
    }

    @Test
    public void testSubtract() throws Exception
    {
        assertEquals(new Time("23:59:59"), new Time("00:00:00").addSecond(-1));
        assertEquals(new Time("22:58:59"), new Time("00:00:00").addHour(-1).addMinute(-1).addSecond(-1));
    }

    @Test
    public void testSubtractUnderflow() throws Exception
    {
        assertEquals(-1, new Time("00:00:00").addHourInplace(-1));
        assertEquals(-1, new Time("00:00:00").addHourInplace(-24));
        assertEquals(-2, new Time("00:00:00").addHourInplace(-25));
        assertEquals(-3, new Time("00:00:00").addHourInplace(-49));
        assertEquals(0, new Time("01:00:00").addHourInplace(-1));
        assertEquals(-1, new Time("00:00:00").addMinuteInplace(-1));
        assertEquals(0, new Time("00:01:00").addMinuteInplace(-1));
        assertEquals(-1, new Time("00:00:00").addSecondInplace(-1));
        assertEquals(0, new Time("00:00:01").addSecondInplace(-1));
    }
}
