package cz.cesnet.shongo.controller.common;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class AbsoluteDateTimeTest
{
    AbsoluteDateTimeSpecification absoluteDateTime;

    @Before
    public void setUp()
    {
        absoluteDateTime = new AbsoluteDateTimeSpecification("2012-01-02T13:04:05");
    }

    @Test
    public void testToString() throws Exception
    {
        assertTrue(new AbsoluteDateTimeSpecification("2007-04-05T14:30:00").toString()
                .startsWith("2007-04-05T14:30:00"));
    }

    @Test
    public void testFromString() throws Exception
    {
        // Not supported by now
        /*assertEquals("Omitting some date/time parts should work",
                "2007-04-01T14:30:00", new AbsoluteDateTimeSpecification("2007-04T14:30").toString());
        assertEquals("ISO short date/time format should be supported",
                new AbsoluteDateTimeSpecification("2007-04-05T14:30:00"),
                new AbsoluteDateTimeSpecification("20070405T1430"));
        assertEquals("Omitting some date/time parts should work",
                "2007-01-01T14:00:00", new AbsoluteDateTimeSpecification("2007T14").toString());
        assertEquals("ISO short time format should be supported",
                "2007-04-05T14:30:00", new AbsoluteDateTimeSpecification("20070405T143000").toString());
        assertEquals("Extra whitespace in the datetime string should be accepted as an extension to ISO 8601",
                new AbsoluteDateTimeSpecification("20070405T14:30:00"),
                new AbsoluteDateTimeSpecification("2007-04-05 T14:30"));*/
    }

    @Test
    public void testEquals() throws Exception
    {
        assertEquals(absoluteDateTime, absoluteDateTime);
        assertEquals(new AbsoluteDateTimeSpecification("2007-04-05T14:30"),
                new AbsoluteDateTimeSpecification("2007-04-05T14:30:00"));
        assertEquals(new AbsoluteDateTimeSpecification("2007-04-01T14"),
                new AbsoluteDateTimeSpecification("2007-04-01T14:00"));
    }

    @Test
    public void testGetEarliest() throws Exception
    {
        assertEquals(DateTime.parse("1234-04-05T14:30"),
                new AbsoluteDateTimeSpecification("1234-04-05T14:30").getEarliest(DateTime.parse("1234-04-05T14:29")));

        assertNull("A datetime which will not occur since given datetime",
                new AbsoluteDateTimeSpecification("9876-04-05T14:30").getEarliest(DateTime.parse("9876-04-05T14:31")));

        assertNull("The comparison should work with strict inequality",
                new AbsoluteDateTimeSpecification("1234-04-05T14:30").getEarliest(DateTime.parse("1234-04-05T14:30")));

        // NOTE: requires correctly set system clock (at least with precision to centuries)
        assertEquals(DateTime.parse("9876-04-05T14:30"),
                new AbsoluteDateTimeSpecification("9876-04-05T14:30").getEarliest());

        assertNull("A datetime which will never occur",
                new AbsoluteDateTimeSpecification("1234-04-05T14:30").getEarliest());
    }

    @Test
    public void testCompareTo() throws Exception
    {
        AbsoluteDateTimeSpecification dt1 = new AbsoluteDateTimeSpecification("1234-04-05T14:30");
        AbsoluteDateTimeSpecification dt2 = new AbsoluteDateTimeSpecification("1234-04-05T14:30");
        AbsoluteDateTimeSpecification dt3 = new AbsoluteDateTimeSpecification("1234-04-05T14:30");
        AbsoluteDateTimeSpecification dt4 = new AbsoluteDateTimeSpecification("2010-01-12T00");
        AbsoluteDateTimeSpecification dt5 = new AbsoluteDateTimeSpecification("2010-01-12T13");

        assertEquals(0, dt1.compareTo(dt2));
        assertEquals(0, dt2.compareTo(dt1));
        assertEquals(0, dt1.compareTo(dt3));
        assertTrue(dt3.compareTo(dt4) < 0);
        assertTrue(dt4.compareTo(dt3) > 0);
        assertTrue(dt4.compareTo(dt5) < 0);
        assertTrue(dt5.compareTo(dt4) > 0);
    }

    @Test
    public void testBefore() throws Exception
    {
        AbsoluteDateTimeSpecification dt1 = new AbsoluteDateTimeSpecification("1234-04-05T14:30");
        AbsoluteDateTimeSpecification dt2 = new AbsoluteDateTimeSpecification("1234-04-05T14:30");
        AbsoluteDateTimeSpecification dt3 = new AbsoluteDateTimeSpecification("1234-04-5T14:31");
        AbsoluteDateTimeSpecification dt4 = new AbsoluteDateTimeSpecification("2010-01-12T00");
        AbsoluteDateTimeSpecification dt5 = new AbsoluteDateTimeSpecification("2010-01-12T13");

        assertFalse(dt1.before(dt2));
        assertFalse(dt2.before(dt1));
        assertTrue(dt2.before(dt3));
        assertFalse(dt3.before(dt2));
        assertTrue(dt3.before(dt4));
        assertFalse(dt4.before(dt3));
        assertTrue(dt4.before(dt5));
        assertFalse(dt5.before(dt4));

        assertTrue(dt1.beforeOrEqual(dt2));
        assertTrue(dt2.beforeOrEqual(dt1));
        assertTrue(dt2.beforeOrEqual(dt3));
        assertFalse(dt3.beforeOrEqual(dt2));
        assertTrue(dt3.beforeOrEqual(dt4));
        assertFalse(dt4.beforeOrEqual(dt3));
        assertTrue(dt4.beforeOrEqual(dt5));
        assertFalse(dt5.beforeOrEqual(dt4));
    }

    @Test
    public void testAfter() throws Exception
    {
        AbsoluteDateTimeSpecification dt1 = new AbsoluteDateTimeSpecification("1234-04-05T14:30");
        AbsoluteDateTimeSpecification dt2 = new AbsoluteDateTimeSpecification("1234-04-05T14:30");
        AbsoluteDateTimeSpecification dt3 = new AbsoluteDateTimeSpecification("1234-04-5T14:31");
        AbsoluteDateTimeSpecification dt4 = new AbsoluteDateTimeSpecification("2010-01-12T00");
        AbsoluteDateTimeSpecification dt5 = new AbsoluteDateTimeSpecification("2010-01-12T13");

        assertFalse(dt1.after(dt2));
        assertFalse(dt2.after(dt1));
        assertFalse(dt2.after(dt3));
        assertTrue(dt3.after(dt2));
        assertFalse(dt3.after(dt4));
        assertTrue(dt4.after(dt3));
        assertFalse(dt4.after(dt5));
        assertTrue(dt5.after(dt4));

        assertTrue(dt1.afterOrEqual(dt2));
        assertTrue(dt2.afterOrEqual(dt1));
        assertFalse(dt2.afterOrEqual(dt3));
        assertTrue(dt3.afterOrEqual(dt2));
        assertFalse(dt3.afterOrEqual(dt4));
        assertTrue(dt4.afterOrEqual(dt3));
        assertFalse(dt4.afterOrEqual(dt5));
        assertTrue(dt5.afterOrEqual(dt4));
    }
}
