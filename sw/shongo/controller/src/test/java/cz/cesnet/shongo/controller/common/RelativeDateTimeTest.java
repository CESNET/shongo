package cz.cesnet.shongo.controller.common;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Relative date/time tests
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RelativeDateTimeTest
{
    private RelativeDateTimeSpecification relativeDateTime;

    @Before
    public void setUp() throws Exception
    {
        relativeDateTime = new RelativeDateTimeSpecification("P1Y2M3DT4H5M6S");
    }

    @Test
    public void testGetters() throws Exception
    {
        assertEquals(Period.parse("P1Y2M3DT4H5M6S"), relativeDateTime.getDuration());
    }

    @Test
    public void testSetters() throws Exception
    {
        RelativeDateTimeSpecification relativeDateTime = new RelativeDateTimeSpecification("P2W");
        assertEquals(Period.parse("P2W"), relativeDateTime.getDuration());
    }

    @Test
    public void testGetEarliest() throws Exception
    {
        DateTime referenceDateTime = DateTime.parse("2012-01-01T12:00:00");
        assertEquals(DateTime.parse("2013-03-04T16:05:06"), relativeDateTime.getEarliest(referenceDateTime));
    }

    @Test
    public void testEquals() throws Exception
    {
        assertEquals(relativeDateTime, relativeDateTime);
        assertEquals(new RelativeDateTimeSpecification("P1W"), new RelativeDateTimeSpecification("P7D"));
    }
}
