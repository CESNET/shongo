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
    public void testGetters() throws Exception
    {
        assertEquals(relativeDateTime.getDuration(), new Period("P1Y2M3DT4H5M6S"));
    }

    @Test
    public void testSetters() throws Exception
    {
        RelativeDateTime relativeDateTime = new RelativeDateTime();
        relativeDateTime.setDuration(new Period("P2W"));
        assertEquals(relativeDateTime.getDuration(), new Period("P2W"));
    }

    @Test
    public void testGetEarliest() throws Exception
    {
        AbsoluteDateTime referenceDateTime = new AbsoluteDateTime("2012-01-01 T12:00:00");
        assertEquals(relativeDateTime.getEarliest(referenceDateTime), new AbsoluteDateTime("2013-03-04 T16:05:06"));
    }

    @Test
    public void testEquals() throws Exception
    {
        assertEquals(new RelativeDateTime(new Period("P1W")), new RelativeDateTime(new Period("P7D")));
    }
}
