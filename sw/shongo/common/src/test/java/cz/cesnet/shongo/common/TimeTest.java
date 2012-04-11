package cz.cesnet.shongo.common;

import org.junit.Test;

import static junit.framework.Assert.*;
import static junitx.framework.Assert.assertNotEquals;

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
        time.setEmpty();
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
        assertEquals(new Time("12:01"), new Time("1201"));
        assertNotEquals(new Time("12:01"), new Time("120102"));
        assertNotEquals(new Time("12"), new Time("120101"));
    }

    @Test
    public void testMatch() throws Exception
    {
        assertTrue(new Time("12:01:02").match(new Time("120102")));
        assertTrue(new Time("12:01").match(new Time("120101")));
        assertTrue(new Time("12:01").match(new Time("120102")));
        assertTrue(new Time("12").match(new Time("120101")));
        assertTrue(new Time("12").match(new Time("120201")));
    }

    @Test
    public void testCompareTo() throws Exception
    {
        Time time1 = new Time("12:01:01");
        Time time2 = new Time("12:01");
        Time time3 = new Time("1201");
        Time time4 = new Time("11:01:01");
        Time time5 = new Time("12:01:02");

        try {
            assertEquals(0, time1.compareTo(time2));
            fail("AssertionError should be thrown.");
        }
        catch (AssertionError error) {
        }

        try {
            assertEquals(0, time2.compareTo(time1));
            fail("AssertionError should be thrown.");
        }
        catch (AssertionError error) {
        }

        assertEquals(0, time2.compareTo(time3));
        assertEquals(-1, time4.compareTo(time5));
        assertEquals(1, time5.compareTo(time4));
    }

    @Test
    public void testAdd() throws Exception
    {
        assertEquals(new Time("01:01:00"), new Time("23:59:59").add(new Time(1, 1, 1)));
        assertEquals(new Time("02:02:02"), new Time("23:59:59").add(49, 61, 63));
    }

    @Test
    public void testAddOverflow() throws Exception
    {
        assertEquals(1, new Time("23:59:59").add(1, 0, 0).popOverflow());
        assertEquals(0, new Time("22:59:59").add(1, 0, 0).popOverflow());
        assertEquals(1, new Time("23:59:59").add(0, 1, 0).popOverflow());
        assertEquals(0, new Time("23:58:59").add(0, 1, 0).popOverflow());
        assertEquals(1, new Time("23:59:59").add(0, 0, 1).popOverflow());
        assertEquals(0, new Time("23:59:58").add(0, 0, 1).popOverflow());
    }

    @Test
    public void testSubtract() throws Exception
    {
        assertEquals(new Time("23:59:59"), new Time("00:00:00").subtract(0, 0, 1));
        assertEquals(new Time("22:58:59"), new Time("00:00:00").subtract(new Time(1, 1, 1)));
    }

    @Test
    public void testSubtractUnderflow() throws Exception
    {
        assertEquals(1, new Time("00:00:00").subtract(1, 0, 0).popUnderflow());
        assertEquals(1, new Time("00:00:00").subtract(24, 0, 0).popUnderflow());
        assertEquals(2, new Time("00:00:00").subtract(25, 0, 0).popUnderflow());
        assertEquals(3, new Time("00:00:00").subtract(49, 0, 0).popUnderflow());
        assertEquals(0, new Time("01:00:00").subtract(1, 0, 0).popUnderflow());
        assertEquals(1, new Time("00:00:00").subtract(0, 1, 0).popUnderflow());
        assertEquals(2, new Time("00:00:00").subtract(0, 24 * 60 + 1, 0).popUnderflow());
        assertEquals(0, new Time("00:01:00").subtract(0, 1, 0).popUnderflow());
        assertEquals(1, new Time("00:00:00").subtract(0, 0, 1).popUnderflow());
        assertEquals(2, new Time("00:00:00").subtract(0, 0, 24 * 60 * 60 + 1).popUnderflow());
        assertEquals(0, new Time("00:00:01").subtract(0, 0, 1).popUnderflow());
    }
}
