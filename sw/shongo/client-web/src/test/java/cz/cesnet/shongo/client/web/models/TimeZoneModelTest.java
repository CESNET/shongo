package cz.cesnet.shongo.client.web.models;

import junit.framework.Assert;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import java.util.Map;

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
        Map<String, String> timeZones = TimeZoneModel.getTimeZones();
        String id = timeZones.keySet().iterator().next();
        DateTimeZone dateTimeZone = DateTimeZone.forID(id);
        Assert.assertNotNull(dateTimeZone);
    }
}
