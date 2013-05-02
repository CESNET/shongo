package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.controller.common.DateTimeSpecification;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link Resource}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceTest
{
    @Test
    public void testIsAvailableAt() throws Exception
    {
        DeviceResource resource = new DeviceResource();
        resource.setMaximumFuture(DateTimeSpecification.fromString("P4M"));

        AliasProviderCapability capability1 = new AliasProviderCapability("test", AliasType.ROOM_NAME);
        resource.addCapability(capability1);

        AliasProviderCapability capablity2 = new AliasProviderCapability("test", AliasType.ROOM_NAME);
        capablity2.setMaximumFuture(DateTimeSpecification.fromString("P1Y"));
        resource.addCapability(capablity2);

        DateTime dateTime = DateTime.now();
        Assert.assertTrue(resource.isAvailableInFuture(DateTime.parse("0"), dateTime));
        Assert.assertTrue(resource.isAvailableInFuture(dateTime.plus(Period.parse("P2M")), dateTime));
        Assert.assertTrue(resource.isAvailableInFuture(dateTime.plus(Period.parse("P4M")), dateTime));
        Assert.assertFalse(resource.isAvailableInFuture(dateTime.plus(Period.parse("P5M")), dateTime));

        Assert.assertTrue(capability1.isAvailableInFuture(DateTime.parse("0"), dateTime));
        Assert.assertTrue(
                capability1.isAvailableInFuture(dateTime.plus(Period.parse("P2M")), dateTime));
        Assert.assertTrue(capability1.isAvailableInFuture(dateTime.plus(Period.parse("P4M")), dateTime));
        Assert.assertFalse(
                capability1.isAvailableInFuture(dateTime.plus(Period.parse("P5M")), dateTime));

        Assert.assertTrue(capablity2.isAvailableInFuture(DateTime.parse("0"), dateTime));
        Assert.assertTrue(capablity2.isAvailableInFuture(dateTime.plus(Period.parse("P2M")), dateTime));
        Assert.assertTrue(
                capablity2.isAvailableInFuture(dateTime.plus(Period.parse("P4M")), dateTime));
        Assert.assertTrue(capablity2.isAvailableInFuture(dateTime.plus(Period.parse("P8M")), dateTime));
        Assert.assertFalse(
                capablity2.isAvailableInFuture(dateTime.plus(Period.parse("P13M")), dateTime));
    }
}
