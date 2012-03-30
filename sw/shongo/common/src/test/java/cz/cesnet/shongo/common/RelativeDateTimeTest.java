package cz.cesnet.shongo.common;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Relative date/time tests
 *
 * @author Martin Srom
 */
public class RelativeDateTimeTest
{
    private RelativeDateTime relativeDateTime;

    @Before
    public void setUp() throws Exception
    {
        relativeDateTime = new RelativeDateTime(new Period("P1Y2M3DT4H5M6S"));
    }

    @Test
    public void testGetEarliest() throws Exception
    {
        AbsoluteDateTime referenceDateTime = new AbsoluteDateTime("20120101T12:00:00");
        assertEquals(relativeDateTime.getEarliest(referenceDateTime), new AbsoluteDateTime("20130304T16:05:06"));
    }
}
