package cz.cesnet.shongo.util;

import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tests for {@link DateTimeFormatter#roundDuration},
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DateTimeFormatterTest
{
    @Test
    public void testXXX() throws Exception
    {
        String code = "format(reservations, \"af\", da, \"x\")";
        Pattern pattern = Pattern.compile("^([\\w]+)\\(([\\w.-]+|\"[^\"]*\")?(\\s*,\\s*([\\w.-]+|\"[^\"]*\"))?(\\s*,\\s*([\\w.-]+|\"[^\"]*\"))?(\\s*,\\s*([\\w.-]+|\"[^\"]*\"))?\\)$");
        Matcher functionMatcher = pattern.matcher(code);
        if (functionMatcher.matches()) {
            int count = 0;
            for (int i = 0; i <= functionMatcher.groupCount(); i++) {
                if ( functionMatcher.group(i) == null) {
                    break;
                }
                count++;
                System.err.println(functionMatcher.group(i));
            }
        }
    }

    @Test
    public void testCzech() throws Exception
    {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.getInstance(
                DateTimeFormatter.SHORT, new Locale("cs"), DateTimeZone.getDefault());
        for (int index = 0; index < 10; index++) {
            System.out.println(dateTimeFormatter.formatDuration(createPeriod(index)));
        }
    }

    private Period createPeriod(int count)
    {
        return Period.years(count).plusMonths(count).plusWeeks(count).plusDays(count)
                .plusHours(count).plusMinutes(count).plusSeconds(count).plusMillis(count);
    }

    /**
     * Test for {@link DateTimeFormatter#roundDuration},
     *
     * @throws Exception
     */
    @Test
    public void testRoundPeriod() throws Exception
    {
        Assert.assertEquals(Period.months(2),
                DateTimeFormatter.roundDuration(
                        Period.months(1).withWeeks(4).withDays(2).withHours(23).withMinutes(59).withSeconds(59)));

        Assert.assertEquals(Period.days(1).withHours(2),
                DateTimeFormatter.roundDuration(Period.hours(25).withMinutes(59).withSeconds(20)));

        Assert.assertEquals(Period.years(2).withWeeks(1),
                DateTimeFormatter.roundDuration(Period.years(2).withWeeks(1).withDays(3)));

        Assert.assertEquals(Period.seconds(5),
                DateTimeFormatter.roundDuration(Period.seconds(5)));
    }
}
