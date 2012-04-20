package cz.cesnet.shongo.common;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class AbsoluteDateTimeTest
{
    AbsoluteDateTime absoluteDateTime;

    @Before
    public void setUp()
    {
        absoluteDateTime = new AbsoluteDateTime("2012-01-02 T13:04:05");
    }

    @Test
    public void testToString() throws Exception
    {
        assertEquals("0000-00-00", new AbsoluteDateTime().toString());
        assertEquals("2007-04-05T14:30:00", new AbsoluteDateTime("2007-04-05T14:30:00").toString());
    }

    @Test
    public void testFromString() throws Exception
    {
        assertEquals("Omitting some date/time parts should work",
                "2007-04-00T14:30", new AbsoluteDateTime("2007-04T14:30").toString());
        assertEquals("Omitting some date/time parts should work",
                "2007-00-00T14", new AbsoluteDateTime("2007T14").toString());

        assertEquals("ISO short time format should be supported",
                "2007-04-05T14:30", new AbsoluteDateTime("20070405T1430").toString());
        assertEquals("ISO short date/time format should be supported",
                "2007-04-05T14:30:00", new AbsoluteDateTime("20070405T143000").toString());

        assertEquals("Extra whitespace in the datetime string should be accepted as an extension to ISO 8601",
                "2007-04-05T14:30", new AbsoluteDateTime("2007-04-05 T14:30").toString());
    }

    @Test
    public void testEquals() throws Exception
    {
        assertEquals(absoluteDateTime, absoluteDateTime);
        assertEquals(new AbsoluteDateTime("20070405T1430"), new AbsoluteDateTime("2007-04-05 T14:30"));
        assertEquals(new AbsoluteDateTime("20070400T14"), new AbsoluteDateTime("2007-04-00T14"));
    }

    @Test
    public void testMatch() throws Exception
    {
        assertTrue(absoluteDateTime.match(absoluteDateTime));
        assertTrue(new AbsoluteDateTime("20070405T143000").match(new AbsoluteDateTime("2007-04-05 T14:30")));
        assertTrue(new AbsoluteDateTime("20070400T14").match(new AbsoluteDateTime("2007-04-00T14:00:00")));
    }

    @Test
    public void testGetEarliest() throws Exception
    {
        assertEquals(new AbsoluteDateTime("1234-04-05T14:30"),
                new AbsoluteDateTime("1234-04-05T14:30").getEarliest(new AbsoluteDateTime("1234-04-05T14:29")));

        assertNull("A datetime which will not occur since given datetime",
                new AbsoluteDateTime("9876-04-05T14:30").getEarliest(new AbsoluteDateTime("9876-04-05T14:31")));

        assertNull("The comparison should work with strict inequality",
                new AbsoluteDateTime("1234-04-05T14:30").getEarliest(new AbsoluteDateTime("1234-04-05T14:30")));

        // NOTE: requires correctly set system clock (at least with precision to centuries)
        assertEquals(new AbsoluteDateTime("9876-04-05T14:30"),
                new AbsoluteDateTime("9876-04-05T14:30").getEarliest());

        assertNull("A datetime which will never occur",
                new AbsoluteDateTime("1234-04-05T14:30").getEarliest());
    }

    @Test
    public void testCompareTo() throws Exception
    {
        AbsoluteDateTime dt1 = new AbsoluteDateTime("1234-04-05 T14:30");
        AbsoluteDateTime dt2 = new AbsoluteDateTime("1234-04-05 T14:30");
        AbsoluteDateTime dt3 = new AbsoluteDateTime("1234-04-05 T14:30");
        AbsoluteDateTime dt4 = new AbsoluteDateTime("2010-01-12 T00");
        AbsoluteDateTime dt5 = new AbsoluteDateTime("2010-01-12 T13");

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
        AbsoluteDateTime dt1 = new AbsoluteDateTime("1234-04-05 T14:30");
        AbsoluteDateTime dt2 = new AbsoluteDateTime("1234-04-05 T14:30");
        AbsoluteDateTime dt3 = new AbsoluteDateTime("1234-04-05 T14:31");
        AbsoluteDateTime dt4 = new AbsoluteDateTime("2010-01-12 T00");
        AbsoluteDateTime dt5 = new AbsoluteDateTime("2010-01-12 T13");

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
        AbsoluteDateTime dt1 = new AbsoluteDateTime("1234-04-05 T14:30");
        AbsoluteDateTime dt2 = new AbsoluteDateTime("1234-04-05 T14:30");
        AbsoluteDateTime dt3 = new AbsoluteDateTime("1234-04-05 T14:31");
        AbsoluteDateTime dt4 = new AbsoluteDateTime("2010-01-12 T00");
        AbsoluteDateTime dt5 = new AbsoluteDateTime("2010-01-12 T13");

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

    @Test
    public void testAdd() throws Exception
    {
        AbsoluteDateTime dt = new AbsoluteDateTime("2012-02-28 T12:00");
        AbsoluteDateTime result = dt.add(new Period("P1DT1H"));
        assertEquals("The original datetime object should not be modified",
                new AbsoluteDateTime("2012-02-28 T12:00"), dt);
        assertEquals(new AbsoluteDateTime("2012-02-29 T13:00"), result);

        AbsoluteDateTime dt2 = new AbsoluteDateTime("2012-02-28 T12:00");
        AbsoluteDateTime result2 = dt2.add(new Period("PT13H"));
        assertEquals(new AbsoluteDateTime("2012-02-29 T01:00"), result2);
    }

    @Test
    public void testSubtract() throws Exception
    {
        AbsoluteDateTime dt = new AbsoluteDateTime("2012-02-28 T12:00");
        AbsoluteDateTime result = dt.subtract(new Period("PT13H"));
        assertEquals("The original datetime object should not be modified",
                new AbsoluteDateTime("2012-02-28 T12:00"), dt);
        assertEquals(new AbsoluteDateTime("2012-02-27 T23:00"), result);

        assertEquals(new AbsoluteDateTime("1234-12-12 T12:34:56"),
                new AbsoluteDateTime("1234-12-12 T12:34:56").
                        add(new Period("P8Y1DT8M1S")).subtract(new Period("P8Y1DT8M1S")));
    }
}
