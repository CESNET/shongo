package cz.cesnet.shongo.common;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Relative date/time tests
 *
 * @author Martin Srom
 */
public class RelativeDateTimeTest
{
    @Test
    public void testCommon()
    {
        // Test relative date/time from given base date/time
        AbsoluteDateTime baseDateTime = new AbsoluteDateTime("20120101T12:00:00");
        RelativeDateTime relativeDateTime = new RelativeDateTime(baseDateTime, new Period("P1Y2M3DT4H5M6S"));
        assertEquals(relativeDateTime.getEarliest(), new AbsoluteDateTime("20130304T16:05:06"));
    }
}
