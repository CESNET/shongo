package cz.cesnet.shongo.common;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author Ondrej Bouda
 */
public class AbsoluteDateTimeTest
{
    @Test
    public void testToString() throws Exception
    {
        assertEquals("0000-00-00T00:00:00", new AbsoluteDateTime().toString());
        assertEquals("2007-04-05T14:30:00", new AbsoluteDateTime("2007-04-05T14:30:00").toString());
    }

    @Test
    public void testFromString() throws Exception
    {
        assertEquals("Omitting some date/time parts should work",
                "2007-04-00T14:30:00", new AbsoluteDateTime("2007-04T14:30").toString());

        assertEquals("ISO short time format should be supported",
                "2007-04-05T14:30:00", new AbsoluteDateTime("20070405T1430").toString());
        assertEquals("ISO short date/time format should be supported",
                "2007-04-05T14:30:00", new AbsoluteDateTime("20070405T143000").toString());

        assertEquals("Extra whitespace in the datetime string should be accepted as an extension to ISO 8601",
                "2007-04-05T14:30:00", new AbsoluteDateTime("2007-04-05 T14:30").toString());
    }

    @Test
    public void testEquals() throws Exception
    {
        assertEquals(new AbsoluteDateTime("20070405T143000"), new AbsoluteDateTime("2007-04-05 T14:30"));
        assertEquals(new AbsoluteDateTime("20070400T14"), new AbsoluteDateTime("2007-04-00T14:00:00"));
    }

    @Test
    public void testGetEarliest() throws Exception
    {
        assertEquals(new AbsoluteDateTime("1234-04-05T14:30"),
                new AbsoluteDateTime("1234-04-05T14:30").getEarliest(new AbsoluteDateTime("1234-04-05T14:29")));

        assertNull("A datetime which will not occur since given datetime",
                new AbsoluteDateTime("9876-04-05T14:30").getEarliest(new AbsoluteDateTime("9876-04-05T14:30:01")));

        assertNull("The comparison should work with strict inequality",
                new AbsoluteDateTime("1234-04-05T14:30").getEarliest(new AbsoluteDateTime("1234-04-05T14:30")));

        // NOTE: requires correctly set system clock (at least with precision to centuries)
        assertEquals(new AbsoluteDateTime("9876-04-05T14:30:00"),
                new AbsoluteDateTime("9876-04-05T14:30").getEarliest());

        assertNull("A datetime which will never occur",
                new AbsoluteDateTime("1234-04-05T14:30").getEarliest());
    }

    @Test
    public void testCompareTo() throws Exception
    {

    }

    @Test
    public void testBefore() throws Exception
    {

    }

    @Test
    public void testAfter() throws Exception
    {

    }

    @Test
    public void testAdd() throws Exception
    {
        AbsoluteDateTime dt = new AbsoluteDateTime();
        dt.add(new Period("P1DT1H"));
        assertEquals(new AbsoluteDateTime("0000-00-01T01:00:00"), dt);
    }

    @Test
    public void testSubtract() throws Exception
    {

    }
}
