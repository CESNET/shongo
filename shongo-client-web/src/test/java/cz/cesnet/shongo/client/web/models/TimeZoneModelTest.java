package cz.cesnet.shongo.client.web.models;

import org.junit.Assert;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.junit.Test;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Tests for {@link TimeZoneModel}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TimeZoneModelTest
{
    @Test
    public void test() throws Exception
    {
        System.err.println("\nCS\n");
        Map<String, String> timeZonesCs = TimeZoneModel.getTimeZones(new Locale("cs"), DateTime.now());
        for (String timeZoneId : timeZonesCs.keySet()) {
            System.out.printf("%s -> %s\n", timeZoneId, timeZonesCs.get(timeZoneId));
        }
        System.err.println("\nEnglish\n");
        Map<String, String> timeZones = TimeZoneModel.getTimeZones(new Locale("en"), DateTime.now());
        for (String timeZoneId : timeZones.keySet()) {
            System.out.printf("%s -> %s\n", timeZoneId, timeZones.get(timeZoneId));
        }
        String id = timeZones.keySet().iterator().next();
        DateTimeZone dateTimeZone = DateTimeZone.forID(id);
        Assert.assertNotNull(dateTimeZone);
    }
}
