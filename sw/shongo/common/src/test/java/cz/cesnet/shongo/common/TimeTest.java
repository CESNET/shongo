package cz.cesnet.shongo.common;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

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
        throw new RuntimeException("TODO: Implement");
    }

    @Test
    public void testSetters() throws Exception
    {
        throw new RuntimeException("TODO: Implement");
    }

    @Test
    public void testIsEmpty() throws Exception
    {
        throw new RuntimeException("TODO: Implement");
    }

    @Test
    public void testClear() throws Exception
    {
        throw new RuntimeException("TODO: Implement");
    }

    @Test
    public void testFromString() throws Exception
    {
        throw new RuntimeException("TODO: Implement");
    }

    @Test
    public void testToString() throws Exception
    {
        throw new RuntimeException("TODO: Implement");
    }

    @Test
    public void testEquals() throws Exception
    {
        throw new RuntimeException("TODO: Implement");
    }

    @Test
    public void testCompareTo() throws Exception
    {
        throw new RuntimeException("TODO: Implement");
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
