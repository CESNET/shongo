package cz.cesnet.shongo.common;

import org.junit.Test;

import static junit.framework.Assert.*;
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
        Date date = new Date("2012-01-02");
        assertEquals(2012, date.getYear());
        assertEquals(01, date.getMonth());
        assertEquals(02, date.getDay());
    }

    @Test
    public void testSetters() throws Exception
    {
        Date date = new Date();
        date.setYear(2013);
        date.setMonth(03);
        date.setDay(04);
        assertEquals(new Date("2013-03-04"), date);
    }

    @Test
    public void testIsEmpty() throws Exception
    {
        assertTrue(new Date().isEmpty());
        assertFalse(new Date("2012-01-01").isEmpty());
    }

    @Test
    public void testClear() throws Exception
    {
        Date date = new Date("2012-01-01");
        date.setEmpty();
        assertTrue(date.isEmpty());
    }

    @Test
    public void testFromString() throws Exception
    {
        assertEquals(new Date("2012-01-02"), new Date("20120102"));
        assertEquals(new Date("2012-01"), new Date("201201"));
        assertEquals(new Date("2012"), new Date("2012"));
    }

    @Test
    public void testToString() throws Exception
    {
        assertEquals("2012-01-02", new Date("20120102").toString());
        assertEquals("2012-01-00", new Date("201201").toString());
        assertEquals("2012-00-00", new Date("2012").toString());
    }

    @Test
    public void testEquals() throws Exception
    {
        assertEquals(new Date("2012-01-02"), new Date("20120102"));
        assertEquals(new Date("2012-01"), new Date("201201"));
        assertEquals(new Date("2012"), new Date("2012"));
    }

    @Test
    public void testMatch() throws Exception
    {
        assertTrue(new Date("2012-01-02").match(new Date("20120102")));
        assertTrue(new Date("2012-01").match(new Date("20120102")));
        assertTrue(new Date("2012-01").match(new Date("20120103")));
        assertTrue(new Date("2012").match(new Date("20120101")));
        assertTrue(new Date("2012").match(new Date("20120201")));
    }

    @Test
    public void testCompareTo() throws Exception
    {
        Date date1 = new Date("2012-01-01");
        Date date2 = new Date("2012-01");
        Date date3 = new Date("201201");
        Date date4 = new Date("2011-01-01");
        Date date5 = new Date("2012-01-02");

        try {

            assertEquals(0, date1.compareTo(date2));
            fail("AssertionError should be thrown.");
        }
        catch (AssertionError error) {
        }

        try {
            assertEquals(0, date2.compareTo(date1));
            fail("AssertionError should be thrown.");
        }
        catch (AssertionError error) {
        }

        assertEquals(0, date2.compareTo(date3));
        assertEquals(-1, date4.compareTo(date5));
        assertEquals(1, date5.compareTo(date4));
    }

    @Test
    public void testAdd() throws Exception
    {
        assertEquals(new Date("2015-01-31"), new Date("2012-12-31").add(2, 1, 0));
        assertEquals(new Date("2013-12-31"), new Date("2012-12-31").add(0, 12, 0));
        assertEquals(new Date("2015-01-31"), new Date("2012-12-31").add(0, 25, 0));
        assertEquals(new Date("2012-02-29"), new Date("2011-01-28").add(1, 1, 1));
        assertEquals(new Date("2013-03-01"), new Date("2011-01-28").add(2, 1, 1));
        assertEquals(new Date("2014-03-01"), new Date("2011-01-28").add(3, 1, 1));
        assertEquals(new Date("2015-03-01"), new Date("2011-01-28").add(4, 1, 1));
        assertEquals(new Date("2016-02-29"), new Date("2011-01-28").add(5, 1, 1));
    }

    @Test
    public void testSubtract() throws Exception
    {
        assertEquals(new Date("2010-12-01"), new Date("2012-01-01").subtract(1, 1, 0));
        assertEquals(new Date("2010-01-01"), new Date("2012-01-01").subtract(0, 24, 0));
        assertEquals(new Date("2011-12-31"), new Date("2012-01-01").subtract(0, 0, 1));
        assertEquals(new Date("2012-02-29"), new Date("2012-04-01").subtract(0, 1, 1));
        assertEquals(new Date("2011-02-28"), new Date("2012-04-01").subtract(1, 1, 1));
        assertEquals(new Date("2010-02-28"), new Date("2012-04-01").subtract(2, 1, 1));
        assertEquals(new Date("2009-02-28"), new Date("2012-04-01").subtract(3, 1, 1));
        assertEquals(new Date("2008-02-29"), new Date("2012-04-01").subtract(4, 1, 1));
    }
}
