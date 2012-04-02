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
        periodicDateTime = new PeriodicDateTime();
        periodicDateTime.setStart(new AbsoluteDateTime("2012-03-01 T12:00"));
        periodicDateTime.setPeriod(new Period("P1W"));
        periodicDateTime.setEnd(new AbsoluteDateTime("2012-03-31"));

        // Except 15.3 (week 12. - 18.3.)
        periodicDateTime.addRule(PeriodicDateTime.RuleType.Disable,
                new AbsoluteDateTime("2012-03-12"), new AbsoluteDateTime("2012-03-18"));

        // With extra lecture on 30.3.
        periodicDateTime.addRule(PeriodicDateTime.RuleType.Extra,
                new AbsoluteDateTime("2012-03-30"));
    }

    @Test
    public void testGetters() throws Exception
    {
        assertEquals(periodicDateTime.getStart(), new AbsoluteDateTime("2012-03-01 T12:00"));
        assertEquals(periodicDateTime.getPeriod(), new Period("P1W"));
        assertEquals(periodicDateTime.getEnd(), new AbsoluteDateTime("2012-03-31"));
        assertEquals(periodicDateTime.getRules().length, 2);
        assertEquals(periodicDateTime.getRules()[0].getType(), PeriodicDateTime.RuleType.Disable);
        assertEquals(periodicDateTime.getRules()[1].getType(), PeriodicDateTime.RuleType.Extra);
    }

    @Test
    public void testSetters() throws Exception
    {
        PeriodicDateTime periodicDateTime = new PeriodicDateTime();

        periodicDateTime.setStart(new AbsoluteDateTime("2012-05-05"));
        assertEquals(periodicDateTime.getStart(), new AbsoluteDateTime("2012-05-05"));

        periodicDateTime.setPeriod(new Period("P5W"));
        assertEquals(periodicDateTime.getPeriod(), new Period("P5W"));

        periodicDateTime.setEnd(new AbsoluteDateTime("2012-06-06"));
        assertEquals(periodicDateTime.getEnd(), new AbsoluteDateTime("2012-06-06"));
    }

    @Test
    public void testAddRule() throws Exception
    {
        PeriodicDateTime periodicDateTime = new PeriodicDateTime();
        assertEquals(periodicDateTime.getRules().length, 0);
        periodicDateTime.addRule(PeriodicDateTime.RuleType.Extra, new AbsoluteDateTime());
        assertEquals(periodicDateTime.getRules().length, 1);
    }

    @Test
    public void testClearRules() throws Exception
    {
        PeriodicDateTime periodicDateTime = new PeriodicDateTime();
        periodicDateTime.addRule(PeriodicDateTime.RuleType.Extra, new AbsoluteDateTime());
        assertEquals(periodicDateTime.getRules().length, 1);
        periodicDateTime.clearRules();
        assertEquals(periodicDateTime.getRules().length, 0);
    }

    @Test
    public void testEnumerate() throws Exception
    {
        AbsoluteDateTime[] correctDateTimes = {
                new AbsoluteDateTime("2012-03-01 T12:00"),
                new AbsoluteDateTime("2012-03-08 T12:00"),
                new AbsoluteDateTime("2012-03-22 T12:00"),
                new AbsoluteDateTime("2012-03-29 T12:00"),
        };
        AbsoluteDateTime[] dateTimes = periodicDateTime.enumerate();
        assertEquals(dateTimes.length, correctDateTimes.length);
        for (int index = 0; index < correctDateTimes.length; index++) {
            assertEquals(dateTimes[index], correctDateTimes[index]);
        }
    }

    @Test
    public void testEnumerateRange() throws Exception
    {
        AbsoluteDateTime[] correctDateTimes = {
                new AbsoluteDateTime("2012-03-08 T12:00"),
                new AbsoluteDateTime("2012-03-22 T12:00"),
        };
        AbsoluteDateTime[] dateTimes = periodicDateTime.enumerate(
                new AbsoluteDateTime("2012-03-02"), new AbsoluteDateTime("2012-03-23"));
        assertEquals(dateTimes.length, correctDateTimes.length);
        for (int index = 0; index < correctDateTimes.length; index++) {
            assertEquals(dateTimes[index], correctDateTimes[index]);
        }
    }

    @Test
    public void testGetEarliest() throws Exception
    {
        AbsoluteDateTime referenceDateTime = new AbsoluteDateTime("2012-01-08 T12:01");
        assertEquals(periodicDateTime.getEarliest(referenceDateTime), new AbsoluteDateTime("2012-03-22 T12:00"));
    }

    @Test
    public void testEquals() throws Exception
    {
        PeriodicDateTime periodicDateTime = new PeriodicDateTime();
        periodicDateTime.addRule(PeriodicDateTime.RuleType.Extra, new AbsoluteDateTime("2012-03-01 T12:00"));
        periodicDateTime.addRule(PeriodicDateTime.RuleType.Extra, new AbsoluteDateTime("2012-03-08 T12:00"));
        periodicDateTime.addRule(PeriodicDateTime.RuleType.Extra, new AbsoluteDateTime("2012-03-22 T12:00"));
        periodicDateTime.addRule(PeriodicDateTime.RuleType.Extra, new AbsoluteDateTime("2012-03-29 T12:00"));
        assertEquals(this.periodicDateTime, periodicDateTime);
    }
}