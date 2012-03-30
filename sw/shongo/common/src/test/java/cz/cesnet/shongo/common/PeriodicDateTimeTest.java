package cz.cesnet.shongo.common;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Periodic date/time tests
 *
 * @author Martin Srom
 */
public class PeriodicDateTimeTest
{
    private PeriodicDateTime periodicDateTime;

    @Before
    public void setUp() throws Exception
    {
        // Lecture on Thursdays at 12:00 in March (1.3. - 31.3.2012)
        // except 15.3 (week 12. - 18.3.)
        // and with extra lecture on 30.3.
        periodicDateTime = new PeriodicDateTime(
                new AbsoluteDateTime("20120301T12:00"), new Period("P1W"), new AbsoluteDateTime("20120331"));
        periodicDateTime.addRule(PeriodicDateTime.RuleType.Disable,
                new AbsoluteDateTime("20120312"), new AbsoluteDateTime("20120318"));
        periodicDateTime.addRule(PeriodicDateTime.RuleType.Extra,
                new AbsoluteDateTime("20120330"));
    }

    @Test
    public void testEnumerate() throws Exception
    {
        AbsoluteDateTime[] correctDateTimes = {
                new AbsoluteDateTime("201201T12:00"),
                new AbsoluteDateTime("201201T12:00"),
                new AbsoluteDateTime("201201T12:00")
        };
        AbsoluteDateTime[] dateTimes = periodicDateTime.enumerate();
        assertEquals(dateTimes.length, correctDateTimes.length);
        for (int index = 0; index < correctDateTimes.length; index++) {
            assertEquals(dateTimes[index], correctDateTimes[index]);
        }
    }
}
