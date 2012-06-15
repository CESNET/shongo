package cz.cesnet.shongo.common;

import org.junit.Test;

import static junit.framework.Assert.*;

// TODO: Check all PartialDate tests

/**
 * Date tests
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PartialDateTest
{
    @Test
    public void testGetters() throws Exception
    {
        PartialDate date = new PartialDate("2012-01-02");
        assertEquals(2012, date.getYear());
        assertEquals(01, date.getMonth());
        assertEquals(02, date.getDay());
    }

    @Test
    public void testIsEmpty() throws Exception
    {
        assertTrue(new PartialDate().isEmpty());
        assertTrue(new PartialDate("0000-00-00").isEmpty());
        assertFalse(new PartialDate("2012-01-01").isEmpty());
        assertFalse(new PartialDate("2012-01").isEmpty());
        assertFalse(new PartialDate("2012").isEmpty());
        assertFalse(new PartialDate("2012-00-00").isEmpty());
        assertFalse(new PartialDate("0000-01-00").isEmpty());
        assertFalse(new PartialDate("0000-01-01").isEmpty());
    }

    @Test
    public void testFromString() throws Exception
    {
        assertEquals("2012-01-02", new PartialDate("20120102").toString());
        assertEquals("2012-01-00", new PartialDate("201201").toString());
        assertEquals("2012-00-00", new PartialDate("2012").toString());
    }

    @Test
    public void testToString() throws Exception
    {
        assertEquals("2012-01-02", new PartialDate("20120102").toString());
        assertEquals("2012-01-00", new PartialDate("201201").toString());
        assertEquals("2012-00-00", new PartialDate("2012").toString());
    }

    @Test
    public void testEquals() throws Exception
    {
        assertEquals(new PartialDate("2012-01-02"), new Date("20120102"));
        assertEquals(new PartialDate("2012-01"), new Date("201201"));
        assertEquals(new PartialDate("2012"), new Date("2012"));
        assertEquals(new PartialDate("2012-01-02"), new Date("20120102"));
        assertEquals(new PartialDate("2012-01"), new Date("20120102"));
        assertEquals(new PartialDate("2012-01"), new Date("20120103"));
        assertEquals(new PartialDate("2012"), new Date("20120101"));
        assertEquals(new PartialDate("2012"), new Date("20120201"));
    }

    @Test
    public void testCompareTo() throws Exception
    {
        PartialDate date1 = new PartialDate("2012-01-01");
        PartialDate date2 = new PartialDate("2012-01");
        PartialDate date3 = new PartialDate("201201");
        PartialDate date4 = new PartialDate("2011-01-01");
        PartialDate date5 = new PartialDate("2012-01-02");

        assertEquals(0, date1.compareTo(date2));
        assertEquals(0, date2.compareTo(date1));
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
