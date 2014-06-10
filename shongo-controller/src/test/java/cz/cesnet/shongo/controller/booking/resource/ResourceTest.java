package cz.cesnet.shongo.controller.booking.resource;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractExecutorTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.DeviceResource;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.Resource;
import cz.cesnet.shongo.controller.api.ResourceReservation;
import cz.cesnet.shongo.controller.api.RoomProviderCapability;
import cz.cesnet.shongo.controller.api.RoomReservation;
import cz.cesnet.shongo.controller.api.RoomSpecification;

import cz.cesnet.shongo.controller.api.request.ResourceListRequest;
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
public class ResourceTest extends AbstractExecutorTest
{
    @Test
    public void testIsAvailableAt() throws Exception
    {
        ResourceListRequest resourceListRequest = new ResourceListRequest();
        resourceListRequest.setSecurityToken(SECURITY_TOKEN_ROOT);
        resourceListRequest.addCapabilityClass(RoomProviderCapability.class);
        resourceListRequest.addTechnology(Technology.H323);
        getResourceService().listResources(resourceListRequest);

        cz.cesnet.shongo.controller.booking.resource.DeviceResource resource =
                new cz.cesnet.shongo.controller.booking.resource.DeviceResource();
        resource.setMaximumFuture(DateTimeSpecification.fromString("P4M"));

        cz.cesnet.shongo.controller.booking.alias.AliasProviderCapability capability1 =
                new cz.cesnet.shongo.controller.booking.alias.AliasProviderCapability("test", AliasType.ROOM_NAME);
        resource.addCapability(capability1);

        cz.cesnet.shongo.controller.booking.alias.AliasProviderCapability capablity2 =
                new cz.cesnet.shongo.controller.booking.alias.AliasProviderCapability("test", AliasType.ROOM_NAME);
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
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        String resourceId = createResource(resource);

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

        cz.cesnet.shongo.controller.api.ResourceReservation resourceReservation = (ResourceReservation) checkAllocated(
                request1Id);
        Assert.assertEquals("Allocated time slot should be only in future.",
                Interval.parse("2012-07-01/2013-01-01"), resourceReservation.getSlot());

        checkNotAllocated(request2Id);
    }

    @Test
    public void testAllocationOrder() throws Exception
    {
        // AliasProvider
        Resource firstAliasProvider = new Resource();
        firstAliasProvider.setName("firstAliasProvider");
        firstAliasProvider.addCapability(new AliasProviderCapability("{hash}", AliasType.ROOM_NAME));
        firstAliasProvider.setAllocatable(true);
        firstAliasProvider.setAllocationOrder(2);
        String firstAliasProviderId = createResource(firstAliasProvider);

        Resource secondAliasProvider = new Resource();
        secondAliasProvider.setName("secondAliasProvider");
        secondAliasProvider.addCapability(new AliasProviderCapability("{hash}", AliasType.ROOM_NAME));
        secondAliasProvider.setAllocatable(true);
        secondAliasProvider.setAllocationOrder(1);
        String secondAliasProviderId = createResource(secondAliasProvider);

        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        aliasReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        aliasReservationRequest.setSpecification(new AliasSpecification(AliasType.ROOM_NAME));
        AliasReservation aliasReservation = (AliasReservation) allocateAndCheck(aliasReservationRequest);
        Assert.assertEquals(secondAliasProviderId, aliasReservation.getResourceId());

        // RoomProvider
        DeviceResource firstMcu = new DeviceResource();
        firstMcu.setName("firstMcu");
        firstMcu.addTechnology(Technology.H323);
        firstMcu.addCapability(new RoomProviderCapability(10));
        firstMcu.setAllocatable(true);
        firstMcu.setAllocationOrder(2);
        String firstMcuId = createResource(firstMcu);

        DeviceResource secondMcu = new DeviceResource();
        secondMcu.setName("secondMcu");
        secondMcu.addTechnology(Technology.H323);
        secondMcu.addCapability(new RoomProviderCapability(10));
        secondMcu.setAllocatable(true);
        secondMcu.setAllocationOrder(1);
        String secondMcuId = createResource(secondMcu);

        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        roomReservationRequest.setSpecification(new RoomSpecification(5, Technology.H323));
        RoomReservation roomReservation = (RoomReservation) allocateAndCheck(roomReservationRequest);
        Assert.assertEquals(secondMcuId, roomReservation.getResourceId());
        String roomExecutableId = roomReservation.getExecutable().getId();

        // RoomProvider
        DeviceResource firstTcs = new DeviceResource();
        firstTcs.setName("firstTcs");
        firstTcs.addTechnology(Technology.H323);
        firstTcs.addCapability(new RecordingCapability());
        firstTcs.setAllocatable(true);
        firstTcs.setAllocationOrder(2);
        String firstTcsId = createResource(firstTcs);

        DeviceResource secondTcs = new DeviceResource();
        secondTcs.setName("secondMcu");
        secondTcs.addTechnology(Technology.H323);
        secondTcs.addCapability(new RecordingCapability());
        secondTcs.setAllocatable(true);
        secondTcs.setAllocationOrder(1);
        String secondTcsId = createResource(secondTcs);

        // Recording
        ReservationRequest recordingReservationRequest = new ReservationRequest();
        recordingReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        recordingReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        recordingReservationRequest.setSpecification(
                RecordingServiceSpecification.forExecutable(roomExecutableId, true));
        allocateAndCheck(recordingReservationRequest);
        RecordingService recordingService = getExecutableService(roomExecutableId, RecordingService.class);
        Assert.assertEquals(secondTcsId, recordingService.getResourceId());
    }
}
