package cz.cesnet.shongo.common;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Date tests
 *
 * @author Martin Srom
 */
public class DateTest
{
    @Test
    public void testGetters() throws Exception
    {
        //throw new RuntimeException("TODO: Implement");
    }

    @Test
    public void testSetters() throws Exception
    {
        //throw new RuntimeException("TODO: Implement");
    }

    @Test
    public void testIsEmpty() throws Exception
    {
        //throw new RuntimeException("TODO: Implement");
    }

    @Test
    public void testClear() throws Exception
    {
        //throw new RuntimeException("TODO: Implement");
    }

    @Test
    public void testFromString() throws Exception
    {
        //throw new RuntimeException("TODO: Implement");
    }

    @Test
    public void testToString() throws Exception
    {
        //throw new RuntimeException("TODO: Implement");
    }

    @Test
    public void testEquals() throws Exception
    {
        //throw new RuntimeException("TODO: Implement");
    }

    @Test
    public void testCompareTo() throws Exception
    {
        //throw new RuntimeException("TODO: Implement");
    }

    @Test
    public void testAdd() throws Exception
    {
        assertEquals(new Date("2015-01-31"), new Date("2012-12-31").addYear(2).addMonth(1));
        assertEquals(new Date("2013-12-31"), new Date("2012-12-31").addMonth(12));
        assertEquals(new Date("2015-01-31"), new Date("2012-12-31").addMonth(25));
    }

    @Test
    public void testSubtract() throws Exception
    {
        assertEquals(new Date("2010-12-01"), new Date("2012-01-01").addYear(-1).addMonth(-1));
        assertEquals(new Date("2010-01-01"), new Date("2012-01-01").addMonth(-24));
        assertEquals(new Date("2011-12-31"), new Date("2012-01-01").addDay(-1));
        assertEquals(new Date("2012-02-29"), new Date("2012-04-01").addMonth(-1).addDay(-1));
        assertEquals(new Date("2011-02-28"), new Date("2012-04-01").addYear(-1).addMonth(-1).addDay(-1));
    }
}
