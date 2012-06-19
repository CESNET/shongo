package cz.cesnet.shongo.common;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * Periodic date/time tests
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PeriodicDateTimeTest
{
    private PeriodicDateTimeSpecification periodicDateTime;

    @Before
    public void setUp() throws Exception
    {
        // Lecture on Thursdays at 12:00 in March (1.3. - 31.3.2012)
        periodicDateTime = new PeriodicDateTimeSpecification();
        periodicDateTime.setStart(DateTime.parse("2012-03-01T12:00"));
        periodicDateTime.setPeriod(Period.parse("P1W"));
        periodicDateTime.setEnd(LocalDate.parse("2012-03-31"));

        // Except 15.3 (week 12. - 18.3.)
        periodicDateTime.addRule(PeriodicDateTimeSpecification.RuleType.Disable,
                LocalDate.parse("2012-03-12"), LocalDate.parse("2012-03-18"));

        // With extra lecture on 30.3.
        periodicDateTime.addRule(PeriodicDateTimeSpecification.RuleType.Extra,
                LocalDate.parse("2012-03-30"));
    }

    @Test
    public void testGetters() throws Exception
    {
        assertEquals(DateTime.parse("2012-03-01T12:00"), periodicDateTime.getStart());
        assertEquals(Period.parse("P1W"), periodicDateTime.getPeriod());
        assertEquals(LocalDate.parse("2012-03-31"), periodicDateTime.getEnd());
        assertEquals(2, periodicDateTime.getRules().size());
        assertEquals(PeriodicDateTimeSpecification.RuleType.Disable, periodicDateTime.getRules().get(0).getType());
        assertEquals(PeriodicDateTimeSpecification.RuleType.Extra, periodicDateTime.getRules().get(1).getType());
    }

    @Test
    public void testSetters() throws Exception
    {
        PeriodicDateTimeSpecification periodicDateTime = new PeriodicDateTimeSpecification();

        periodicDateTime.setStart(DateTime.parse("2012-05-05"));
        assertEquals(DateTime.parse("2012-05-05"), periodicDateTime.getStart());

        periodicDateTime.setPeriod(Period.parse("P5W"));
        assertEquals(Period.parse("P5W"), periodicDateTime.getPeriod());

        periodicDateTime.setEnd(LocalDate.parse("2012-06-06"));
        assertEquals(LocalDate.parse("2012-06-06"), periodicDateTime.getEnd());
    }

    @Test
    public void testAddRule() throws Exception
    {
        PeriodicDateTimeSpecification periodicDateTime = new PeriodicDateTimeSpecification();
        assertEquals(0, periodicDateTime.getRules().size());
        periodicDateTime.addRule(PeriodicDateTimeSpecification.RuleType.Extra, new LocalDate());
        assertEquals(1, periodicDateTime.getRules().size());
    }

    @Test
    public void testClearRules() throws Exception
    {
        PeriodicDateTimeSpecification periodicDateTime = new PeriodicDateTimeSpecification();
        periodicDateTime.addRule(PeriodicDateTimeSpecification.RuleType.Extra, new LocalDate());
        assertEquals(1, periodicDateTime.getRules().size());
        periodicDateTime.clearRules();
        assertEquals(0, periodicDateTime.getRules().size());
    }

    @Test
    public void testEnumerate() throws Exception
    {
        DateTime[] correctDateTimes = {
                DateTime.parse("2012-03-01T12:00"),
                DateTime.parse("2012-03-08T12:00"),
                DateTime.parse("2012-03-22T12:00"),
                DateTime.parse("2012-03-29T12:00"),
                DateTime.parse("2012-03-30T12:00")
        };
        List<DateTime> dateTimes = periodicDateTime.enumerate();
        assertEquals(correctDateTimes.length, dateTimes.size());
        for (int index = 0; index < correctDateTimes.length; index++) {
            assertEquals(correctDateTimes[index], dateTimes.get(index));
        }
    }

    @Test
    public void testEnumerateRange() throws Exception
    {
        DateTime[] correctDateTimes = {
                DateTime.parse("2012-03-08T12:00"),
                DateTime.parse("2012-03-22T12:00"),
        };
        List<DateTime> dateTimes = periodicDateTime.enumerate(
                DateTime.parse("2012-03-02"), DateTime.parse("2012-03-23"));
        assertEquals(correctDateTimes.length, dateTimes.size());
        for (int index = 0; index < dateTimes.size(); index++) {
            assertEquals(correctDateTimes[index], dateTimes.get(index));
        }
    }

    @Test
    public void testGetEarliest() throws Exception
    {
        DateTime referenceDateTime = DateTime.parse("2012-03-08T12:01");
        assertEquals(DateTime.parse("2012-03-22T12:00"), periodicDateTime.getEarliest(referenceDateTime));
    }

    @Test
    public void testEquals() throws Exception
    {
        assertEquals(periodicDateTime, periodicDateTime);

        PeriodicDateTimeSpecification periodicDateTime = new PeriodicDateTimeSpecification();
        periodicDateTime.addRule(PeriodicDateTimeSpecification.RuleType.Extra, LocalDateTime.parse("2012-03-01T12:00"));
        periodicDateTime.addRule(PeriodicDateTimeSpecification.RuleType.Extra, LocalDateTime.parse("2012-03-08T12:00"));
        periodicDateTime.addRule(PeriodicDateTimeSpecification.RuleType.Extra, LocalDateTime.parse("2012-03-22T12:00"));
        periodicDateTime.addRule(PeriodicDateTimeSpecification.RuleType.Extra, LocalDateTime.parse("2012-03-29T12:00"));
        periodicDateTime.addRule(PeriodicDateTimeSpecification.RuleType.Extra, LocalDateTime.parse("2012-03-30T12:00"));
        assertEquals(periodicDateTime, this.periodicDateTime);
    }
}