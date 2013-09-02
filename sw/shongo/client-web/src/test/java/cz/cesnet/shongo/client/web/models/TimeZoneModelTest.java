package cz.cesnet.shongo.client.web.models;

import junit.framework.Assert;
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
        Map<String, String> timeZones = TimeZoneModel.getTimeZones(DateTime.now());
        for (String timeZoneId : timeZones.keySet()) {
            System.out.printf("%s -> %s\n", timeZoneId, timeZones.get(timeZoneId));
        }
        String id = timeZones.keySet().iterator().next();
        DateTimeZone dateTimeZone = DateTimeZone.forID(id);
        Assert.assertNotNull(dateTimeZone);
    }
}
