package cz.cesnet.shongo.common;

/**
 * Perid/duration tests
 *
 * @author Martin Srom
 */
public class PeriodTest extends junit.framework.TestCase
{
    public void testCommon()
    {
        assertEquals(new Period("P1W"),new Period("P7D"));
        assertEquals(new Period("P1W"),new Period("P3D").add(new Period("P4D")));
    }
}
