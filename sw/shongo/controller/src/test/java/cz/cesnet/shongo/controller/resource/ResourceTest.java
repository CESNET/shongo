package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.controller.common.RelativeDateTimeSpecification;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

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
        resource.setMaximumFuture(new RelativeDateTimeSpecification("P4M"));

        AliasProviderCapability capability1 = new AliasProviderCapability("test", AliasType.ROOM_NAME);
        resource.addCapability(capability1);

        AliasProviderCapability capablity2 = new AliasProviderCapability("test", AliasType.ROOM_NAME);
        capablity2.setMaximumFuture(new RelativeDateTimeSpecification("P1Y"));
        resource.addCapability(capablity2);

        DateTime referenceDateTime = DateTime.now();
        assertTrue(resource.isAvailableInFuture(DateTime.parse("0"), referenceDateTime));
        assertTrue(resource.isAvailableInFuture(referenceDateTime.plus(Period.parse("P2M")), referenceDateTime));
        assertTrue(resource.isAvailableInFuture(referenceDateTime.plus(Period.parse("P4M")), referenceDateTime));
        assertFalse(resource.isAvailableInFuture(referenceDateTime.plus(Period.parse("P5M")), referenceDateTime));

        assertTrue(capability1.isAvailableInFuture(DateTime.parse("0"), referenceDateTime));
        assertTrue(capability1.isAvailableInFuture(referenceDateTime.plus(Period.parse("P2M")), referenceDateTime));
        assertTrue(capability1.isAvailableInFuture(referenceDateTime.plus(Period.parse("P4M")), referenceDateTime));
        assertFalse(capability1.isAvailableInFuture(referenceDateTime.plus(Period.parse("P5M")), referenceDateTime));

        assertTrue(capablity2.isAvailableInFuture(DateTime.parse("0"), referenceDateTime));
        assertTrue(capablity2.isAvailableInFuture(referenceDateTime.plus(Period.parse("P2M")), referenceDateTime));
        assertTrue(capablity2.isAvailableInFuture(referenceDateTime.plus(Period.parse("P4M")), referenceDateTime));
        assertTrue(capablity2.isAvailableInFuture(referenceDateTime.plus(Period.parse("P8M")), referenceDateTime));
        assertFalse(
                capablity2.isAvailableInFuture(referenceDateTime.plus(Period.parse("P13M")), referenceDateTime));
    }
}
