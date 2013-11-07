package cz.cesnet.shongo.controller.booking.resource;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.AbstractSchedulerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.ResourceReservation;
import cz.cesnet.shongo.controller.booking.alias.AliasProviderCapability;
import cz.cesnet.shongo.controller.booking.datetime.DateTimeSpecification;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link Resource}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceTest extends AbstractControllerTest
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

    @Test
    public void testAllocationOnlyToFuture() throws Exception
    {
        cz.cesnet.shongo.controller.api.Resource resource = new cz.cesnet.shongo.controller.api.Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        String resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        ReservationRequest reservationRequest1 = new ReservationRequest();
        reservationRequest1.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest1.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest1.setSpecification(new cz.cesnet.shongo.controller.api.ResourceSpecification(resourceId));
        String request1Id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest1);

        ReservationRequest reservationRequest2 = new ReservationRequest();
        reservationRequest2.setSlot("2011-01-01T00:00", "P1Y");
        reservationRequest2.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest2.setSpecification(new cz.cesnet.shongo.controller.api.ResourceSpecification(resourceId));
        String request2Id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest2);

        runScheduler(Interval.parse("2012-07-01/2012-08-01"));

        cz.cesnet.shongo.controller.api.ResourceReservation resourceReservation = (ResourceReservation) checkAllocated(request1Id);
        Assert.assertEquals("Allocated time slot should be only in future.",
                Interval.parse("2012-07-01/2013-01-01"), resourceReservation.getSlot());

        checkNotAllocated(request2Id);
    }
}
