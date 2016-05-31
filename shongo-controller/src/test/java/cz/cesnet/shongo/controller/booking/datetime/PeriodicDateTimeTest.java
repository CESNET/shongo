package cz.cesnet.shongo.controller.booking.datetime;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import cz.cesnet.shongo.api.Converter;

import java.util.List;

/**
 * Periodic date/time tests
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PeriodicDateTimeTest
{
    private PeriodicDateTime periodicDateTime;

    @Before
    public void setUp() throws Exception
    {
        // Lecture on Thursdays at 12:00 in March (1.3. - 31.3.2012)
        periodicDateTime = new PeriodicDateTime();
        periodicDateTime.setStart(DateTime.parse("2012-03-01T12:00"));
        periodicDateTime.setPeriod(Period.parse("P1W"));
        periodicDateTime.setEnd(LocalDate.parse("2012-03-31"));

        // Except 15.3 (week 12. - 18.3.)
        periodicDateTime.addRule(PeriodicDateTime.RuleType.DISABLE,
                LocalDate.parse("2012-03-12"), LocalDate.parse("2012-03-18"));

        // With extra lecture on 30.3.
        periodicDateTime.addRule(PeriodicDateTime.RuleType.EXTRA,
                LocalDate.parse("2012-03-30"));
    }

    @Test
    public void testGetters() throws Exception
    {
        Assert.assertEquals(DateTime.parse("2012-03-01T12:00"), periodicDateTime.getStart());
        Assert.assertEquals(Period.parse("P1W"), periodicDateTime.getPeriod());
        Assert.assertEquals(LocalDate.parse("2012-03-31"), periodicDateTime.getEnd());
        Assert.assertEquals(2, periodicDateTime.getRules().size());
        Assert.assertEquals(PeriodicDateTime.RuleType.DISABLE, periodicDateTime.getRules().get(0).getType());
        Assert.assertEquals(PeriodicDateTime.RuleType.EXTRA, periodicDateTime.getRules().get(1).getType());
    }

    @Test
    public void testSetters() throws Exception
    {
        PeriodicDateTime periodicDateTime = new PeriodicDateTime();

        periodicDateTime.setStart(DateTime.parse("2012-05-05"));
        Assert.assertEquals(DateTime.parse("2012-05-05"), periodicDateTime.getStart());

        periodicDateTime.setPeriod(Period.parse("P5W"));
        Assert.assertEquals(Period.parse("P5W"), periodicDateTime.getPeriod());

        periodicDateTime.setEnd(LocalDate.parse("2012-06-06"));
        Assert.assertEquals(LocalDate.parse("2012-06-06"), periodicDateTime.getEnd());
    }

    @Test
    public void testAddRule() throws Exception
    {
        PeriodicDateTime periodicDateTime = new PeriodicDateTime();
        Assert.assertEquals(0, periodicDateTime.getRules().size());
        periodicDateTime.addRule(PeriodicDateTime.RuleType.EXTRA, new LocalDate());
        Assert.assertEquals(1, periodicDateTime.getRules().size());
    }

    @Test
    public void testClearRules() throws Exception
    {
        PeriodicDateTime periodicDateTime = new PeriodicDateTime();
        periodicDateTime.addRule(PeriodicDateTime.RuleType.EXTRA, new LocalDate());
        Assert.assertEquals(1, periodicDateTime.getRules().size());
        periodicDateTime.clearRules();
        Assert.assertEquals(0, periodicDateTime.getRules().size());
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
        Assert.assertEquals(correctDateTimes.length, dateTimes.size());
        for (int index = 0; index < correctDateTimes.length; index++) {
            Assert.assertEquals(correctDateTimes[index], dateTimes.get(index));
        }

        try {
            new PeriodicDateTime(DateTime.parse("2012-01-01T12:00"), Period.parse("P1W")).enumerate();
            Assert.fail("Should fail due to infinity.");
        }
        catch (Exception exception) {
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
        Assert.assertEquals(correctDateTimes.length, dateTimes.size());
        for (int index = 0; index < dateTimes.size(); index++) {
            Assert.assertEquals(correctDateTimes[index], dateTimes.get(index));
        }
    }

    @Test
    public void testGetEarliest() throws Exception
    {
        DateTime referenceDateTime = DateTime.parse("2012-03-08T12:01");
        Assert.assertEquals(DateTime.parse("2012-03-22T12:00"), periodicDateTime.getEarliest(referenceDateTime));
    }

    @Test
    public void testEquals() throws Exception
    {
        Assert.assertEquals(periodicDateTime, periodicDateTime);

        PeriodicDateTime periodicDateTime = new PeriodicDateTime();
        periodicDateTime.addRule(PeriodicDateTime.RuleType.EXTRA, LocalDateTime.parse("2012-03-01T12:00"));
        periodicDateTime.addRule(PeriodicDateTime.RuleType.EXTRA, LocalDateTime.parse("2012-03-08T12:00"));
        periodicDateTime.addRule(PeriodicDateTime.RuleType.EXTRA, LocalDateTime.parse("2012-03-22T12:00"));
        periodicDateTime.addRule(PeriodicDateTime.RuleType.EXTRA, LocalDateTime.parse("2012-03-29T12:00"));
        periodicDateTime.addRule(PeriodicDateTime.RuleType.EXTRA, LocalDateTime.parse("2012-03-30T12:00"));
        Assert.assertEquals(periodicDateTime, this.periodicDateTime);
    }

    @Test
    public void testEndMatching() throws Exception
    {
        PeriodicDateTime periodicDateTime = new PeriodicDateTime();
        periodicDateTime.setStart(DateTime.parse("2012-03-01T12:00"));
        periodicDateTime.setPeriod(Period.parse("P1W"));
        periodicDateTime.setEnd(Converter.convertStringToReadablePartial("2012-03-29"));
        Assert.assertEquals(5, periodicDateTime.enumerate().size());
    }

    @Test
    public void testEnumerateWithoutPeriod() throws Exception
    {
        Assert.assertEquals(5, this.periodicDateTime.enumerate().size());
        this.periodicDateTime.setPeriod(Period.ZERO);
        this.periodicDateTime.enumerate();
        Assert.assertEquals(2, this.periodicDateTime.enumerate().size());
    }

}