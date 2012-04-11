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
        assertEquals(new AbsoluteDateTime("2012-03-01 T12:00"), periodicDateTime.getStart());
        assertEquals(new Period("P1W"), periodicDateTime.getPeriod());
        assertEquals(new AbsoluteDateTime("2012-03-31"), periodicDateTime.getEnd());
        assertEquals(2, periodicDateTime.getRules().length);
        assertEquals(PeriodicDateTime.RuleType.Disable, periodicDateTime.getRules()[0].getType());
        assertEquals(PeriodicDateTime.RuleType.Extra, periodicDateTime.getRules()[1].getType());
    }

    @Test
    public void testSetters() throws Exception
    {
        PeriodicDateTime periodicDateTime = new PeriodicDateTime();

        periodicDateTime.setStart(new AbsoluteDateTime("2012-05-05"));
        assertEquals(new AbsoluteDateTime("2012-05-05"), periodicDateTime.getStart());

        periodicDateTime.setPeriod(new Period("P5W"));
        assertEquals(new Period("P5W"), periodicDateTime.getPeriod());

        periodicDateTime.setEnd(new AbsoluteDateTime("2012-06-06"));
        assertEquals(new AbsoluteDateTime("2012-06-06"), periodicDateTime.getEnd());
    }

    @Test
    public void testAddRule() throws Exception
    {
        PeriodicDateTime periodicDateTime = new PeriodicDateTime();
        assertEquals(0, periodicDateTime.getRules().length);
        periodicDateTime.addRule(PeriodicDateTime.RuleType.Extra, new AbsoluteDateTime());
        assertEquals(1, periodicDateTime.getRules().length);
    }

    @Test
    public void testClearRules() throws Exception
    {
        PeriodicDateTime periodicDateTime = new PeriodicDateTime();
        periodicDateTime.addRule(PeriodicDateTime.RuleType.Extra, new AbsoluteDateTime());
        assertEquals(1, periodicDateTime.getRules().length);
        periodicDateTime.clearRules();
        assertEquals(0, periodicDateTime.getRules().length);
    }

    @Test
    public void testEnumerate() throws Exception
    {
        AbsoluteDateTime[] correctDateTimes = {
                new AbsoluteDateTime("2012-03-01 T12:00"),
                new AbsoluteDateTime("2012-03-08 T12:00"),
                new AbsoluteDateTime("2012-03-22 T12:00"),
                new AbsoluteDateTime("2012-03-29 T12:00"),
                new AbsoluteDateTime("2012-03-30 T12:00")
        };
        AbsoluteDateTime[] dateTimes = periodicDateTime.enumerate();
        assertEquals(correctDateTimes.length, dateTimes.length);
        for (int index = 0; index < correctDateTimes.length; index++) {
            assertEquals(correctDateTimes[index], dateTimes[index]);
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
        assertEquals(correctDateTimes.length, dateTimes.length);
        for (int index = 0; index < dateTimes.length; index++) {
            assertEquals(correctDateTimes[index], dateTimes[index]);
        }
    }

    @Test
    public void testGetEarliest() throws Exception
    {
        AbsoluteDateTime referenceDateTime = new AbsoluteDateTime("2012-03-08 T12:01");
        assertEquals(new AbsoluteDateTime("2012-03-22 T12:00"), periodicDateTime.getEarliest(referenceDateTime));
    }

    @Test
    public void testEquals() throws Exception
    {
        assertEquals(periodicDateTime, periodicDateTime);

        PeriodicDateTime periodicDateTime = new PeriodicDateTime();
        periodicDateTime.addRule(PeriodicDateTime.RuleType.Extra, new AbsoluteDateTime("2012-03-01 T12:00"));
        periodicDateTime.addRule(PeriodicDateTime.RuleType.Extra, new AbsoluteDateTime("2012-03-08 T12:00"));
        periodicDateTime.addRule(PeriodicDateTime.RuleType.Extra, new AbsoluteDateTime("2012-03-22 T12:00"));
        periodicDateTime.addRule(PeriodicDateTime.RuleType.Extra, new AbsoluteDateTime("2012-03-29 T12:00"));
        periodicDateTime.addRule(PeriodicDateTime.RuleType.Extra, new AbsoluteDateTime("2012-03-30 T12:00"));
        assertEquals(periodicDateTime, this.periodicDateTime);
    }
}