package cz.cesnet.shongo.controller.booking;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestReusement;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

/**
 * Tests for {@link AllocationStateReport}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AllocationStateReportTest extends AbstractControllerTest
{
    /**
     * Test {@link AllocationStateReport.ReusementInvalidSlot}
     */
    @Test
    public void testReusedReservationRequestInvalidSlot() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        String resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        ReservationRequest reservationRequestFirst = new ReservationRequest();
        reservationRequestFirst.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequestFirst.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestFirst.setSpecification(new ResourceSpecification(resourceId));
        reservationRequestFirst.setReusement(ReservationRequestReusement.OWNED);
        String reservationRequestFirstId = allocate(reservationRequestFirst);
        checkAllocated(reservationRequestFirstId);

        ReservationRequest reservationRequestSecond = new ReservationRequest();
        reservationRequestSecond.setSlot("2013-01-01T00:00", "P1Y");
        reservationRequestSecond.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSecond.setSpecification(new ResourceSpecification(resourceId));
        reservationRequestSecond.setReusedReservationRequestId(reservationRequestFirstId);
        String reservationRequestSecondId = allocate(reservationRequestSecond);
        checkAllocationFailed(reservationRequestSecondId);

        finish(reservationRequestSecondId, AllocationStateReport.ReusementInvalidSlot.class);
    }

    /**
     * Test {@link AllocationStateReport.ReusementAlreadyUsed}
     */
    @Test
    public void testReusedReservationRequestAlreadyUsed() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        String resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        ReservationRequest reservationRequestFirst = new ReservationRequest();
        reservationRequestFirst.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequestFirst.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestFirst.setSpecification(new ResourceSpecification(resourceId));
        reservationRequestFirst.setReusement(ReservationRequestReusement.OWNED);
        String reservationRequestFirstId = allocate(reservationRequestFirst);
        checkAllocated(reservationRequestFirstId);

        ReservationRequest reservationRequestSecond = new ReservationRequest();
        reservationRequestSecond.setSlot("2012-03-01T00:00", "P6M");
        reservationRequestSecond.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSecond.setSpecification(new ResourceSpecification(resourceId));
        reservationRequestSecond.setReusedReservationRequestId(reservationRequestFirstId);
        allocateAndCheck(reservationRequestSecond);

        ReservationRequest reservationRequestThird = new ReservationRequest();
        reservationRequestThird.setSlot("2012-06-01T00:00", "P1M");
        reservationRequestThird.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestThird.setSpecification(new ResourceSpecification(resourceId));
        reservationRequestThird.setReusedReservationRequestId(reservationRequestFirstId);
        String reservationRequestThirdId = allocate(reservationRequestThird);
        checkAllocationFailed(reservationRequestThirdId);

        finish(reservationRequestThirdId, AllocationStateReport.ReusementAlreadyUsed.class);
    }

    @Test
    public void testExceedMaximumFuture() throws Exception
    {
        Resource resource = new Resource();
        resource.setName("resource");
        resource.setAllocatable(true);
        resource.setMaximumFuture(new DateTime("2012-11-01T00:00"));
        String resourceId = getResourceService().createResource(SECURITY_TOKEN, resource);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2013-01-01T00:00", "PT1H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ResourceSpecification(resourceId));
        String reservationRequestId = allocate(reservationRequest);
        checkAllocationFailed(reservationRequestId);

        finish(reservationRequestId, AllocationStateReport.MaximumFutureExceeded.class);
    }

    @Test
    public void testExceedMaximumFutureAlias() throws Exception
    {
        Resource aliasProviderFirst = new Resource();
        aliasProviderFirst.setName("aliasProvider1");
        aliasProviderFirst.setAllocatable(true);
        aliasProviderFirst.setMaximumFuture(new DateTime("2012-11-01T00:00"));
        aliasProviderFirst.addCapability(new AliasProviderCapability("{hash}", AliasType.ROOM_NAME));
        getResourceService().createResource(SECURITY_TOKEN, aliasProviderFirst);

        Resource aliasProviderSecond = new Resource();
        aliasProviderSecond.setName("aliasProvider2");
        aliasProviderSecond.setAllocatable(true);
        aliasProviderSecond.setMaximumFuture(new DateTime("2012-12-01T00:00"));
        aliasProviderSecond.addCapability(new AliasProviderCapability("{hash}", AliasType.ROOM_NAME));
        getResourceService().createResource(SECURITY_TOKEN, aliasProviderSecond);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2013-01-01T00:00", "PT1H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(AliasType.ROOM_NAME));
        String reservationRequestId = allocate(reservationRequest);
        checkAllocationFailed(reservationRequestId);

        finish(reservationRequestId, AllocationStateReport.MaximumFutureExceeded.class);
    }

    @Test
    public void testExceedMaximumFutureRoom() throws Exception
    {
        DeviceResource roomProviderFirst = new DeviceResource();
        roomProviderFirst.setName("roomProvider1");
        roomProviderFirst.setAllocatable(true);
        roomProviderFirst.setMaximumFuture(new DateTime("2012-11-01T00:00"));
        roomProviderFirst.addTechnology(Technology.H323);
        roomProviderFirst.addCapability(new RoomProviderCapability(10));
        getResourceService().createResource(SECURITY_TOKEN, roomProviderFirst);

        DeviceResource roomProviderSecond = new DeviceResource();
        roomProviderSecond.setName("roomProvider2");
        roomProviderSecond.setAllocatable(true);
        roomProviderSecond.setMaximumFuture(new DateTime("2012-12-01T00:00"));
        roomProviderSecond.addTechnology(Technology.H323);
        roomProviderSecond.addCapability(new RoomProviderCapability(20));
        getResourceService().createResource(SECURITY_TOKEN, roomProviderSecond);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2013-01-01T00:00", "PT1H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new RoomSpecification(5, Technology.H323));
        String reservationRequestId = allocate(reservationRequest);
        checkAllocationFailed(reservationRequestId);

        finish(reservationRequestId, AllocationStateReport.MaximumFutureExceeded.class);
    }

    @Test
    public void testExceedMaximumDuration() throws Exception
    {
        DeviceResource roomProvider = new DeviceResource();
        roomProvider.setName("roomProvider");
        roomProvider.setAllocatable(true);
        roomProvider.addTechnology(Technology.H323);
        roomProvider.addCapability(new AliasProviderCapability("{hash}", AliasType.ROOM_NAME));
        roomProvider.addCapability(new RoomProviderCapability(50, new AliasType[]{AliasType.ROOM_NAME}));
        getResourceService().createResource(SECURITY_TOKEN, roomProvider);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new RoomSpecification(5, Technology.H323));
        String reservationRequestId = allocate(reservationRequest);
        checkAllocationFailed(reservationRequestId);

        finish(reservationRequestId, AllocationStateReport.MaximumDurationExceeded.class);
    }

    @Test
    public void testExceedRoomCapacity() throws Exception
    {
        DeviceResource roomProviderFirst = new DeviceResource();
        roomProviderFirst.setName("roomProvider1");
        roomProviderFirst.setAllocatable(true);
        roomProviderFirst.addTechnology(Technology.H323);
        roomProviderFirst.addCapability(new AliasProviderCapability("{hash}", AliasType.ROOM_NAME));
        roomProviderFirst.addCapability(new RoomProviderCapability(10, new AliasType[]{AliasType.ROOM_NAME}));
        getResourceService().createResource(SECURITY_TOKEN, roomProviderFirst);

        DeviceResource roomProviderSecond = new DeviceResource();
        roomProviderSecond.setName("roomProvider2");
        roomProviderSecond.setAllocatable(true);
        roomProviderSecond.addTechnology(Technology.H323);
        roomProviderSecond.addCapability(new AliasProviderCapability("{hash}", AliasType.ROOM_NAME));
        roomProviderSecond.addCapability(new RoomProviderCapability(5, new AliasType[]{AliasType.ROOM_NAME}));
        getResourceService().createResource(SECURITY_TOKEN, roomProviderSecond);

        ReservationRequest reservationRequestFirst = new ReservationRequest();
        reservationRequestFirst.setSlot("2012-01-01T00:00", "PT1H");
        reservationRequestFirst.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestFirst.setSpecification(new RoomSpecification(6, Technology.H323));
        allocateAndCheck(reservationRequestFirst);

        ReservationRequest reservationRequestSecond = new ReservationRequest();
        reservationRequestSecond.setSlot("2012-01-01T00:00", "PT1H");
        reservationRequestSecond.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSecond.setSpecification(new RoomSpecification(6, Technology.H323));
        String reservationRequestSecondId = allocate(reservationRequestSecond);
        checkAllocationFailed(reservationRequestSecondId);

        finish(reservationRequestSecondId, AllocationStateReport.RoomCapacityExceeded.class);
    }

    /**
     * Test {@link AllocationStateReport.AliasAlreadyAllocated}
     */
    @Test
    public void testAliasValueAlreadyAllocated() throws Exception
    {
        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability("{hash}", AliasType.ROOM_NAME).withAllowedAnyRequestedValue());
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        ReservationRequest reservationRequestFirst = new ReservationRequest();
        reservationRequestFirst.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequestFirst.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestFirst.setSpecification(new AliasSpecification(AliasType.ROOM_NAME).withValue("test"));
        AliasReservation aliasReservation = (AliasReservation) allocateAndCheck(reservationRequestFirst);
        Assert.assertEquals("Requested value should be allocated.", "test", aliasReservation.getValue());

        ReservationRequest reservationRequestSecond = new ReservationRequest();
        reservationRequestSecond.setSlot("2012-06-01T00:00", "P1M");
        reservationRequestSecond.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSecond.setSpecification(new AliasSpecification(AliasType.ROOM_NAME).withValue("test"));
        String reservationRequestSecondId = allocate(reservationRequestSecond);
        checkAllocationFailed(reservationRequestSecondId);

        finish(reservationRequestSecondId, AllocationStateReport.AliasAlreadyAllocated.class);
    }

    /**
     * Test {@link AllocationStateReport.AliasNotAvailable}
     */
    @Test
    public void testAliasValueNotAvailable() throws Exception
    {
        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability("test", AliasType.ROOM_NAME));
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        ReservationRequest reservationRequestFirst = new ReservationRequest();
        reservationRequestFirst.setSlot("2012-01-01T00:00", "P1Y");
        reservationRequestFirst.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestFirst.setSpecification(new AliasSpecification(AliasType.ROOM_NAME));
        AliasReservation aliasReservation = (AliasReservation) allocateAndCheck(reservationRequestFirst);
        Assert.assertEquals("Requested value should be allocated.", "test", aliasReservation.getValue());

        ReservationRequest reservationRequestSecond = new ReservationRequest();
        reservationRequestSecond.setSlot("2012-06-01T00:00", "P1Y");
        reservationRequestSecond.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSecond.setSpecification(new AliasSpecification(AliasType.ROOM_NAME));
        String reservationRequestSecondId = allocate(reservationRequestSecond);
        checkAllocationFailed(reservationRequestSecondId);

        finish(reservationRequestSecondId, AllocationStateReport.AliasNotAvailable.class);
    }

    /**
     * Perform check.
     *
     * @param reservationRequestId
     * @param requiredType
     */
    private void finish(String reservationRequestId, Class<? extends AllocationStateReport.UserError> requiredType)
    {
        ReservationService reservationService = getReservationService();
        ReservationRequest reservationRequest = (ReservationRequest)
                reservationService.getReservationRequest(SECURITY_TOKEN, reservationRequestId);
        AllocationStateReport allocationStateReport = reservationRequest.getAllocationStateReport();
        AllocationStateReport.UserError userError = allocationStateReport.toUserError();
        System.err.println(userError.getMessage(UserSettings.LOCALE_ENGLISH));
        System.err.println(userError.getMessage(UserSettings.LOCALE_CZECH));
        if (userError.isUnknown()) {
            System.err.println(allocationStateReport.toString(UserSettings.LOCALE_ENGLISH).trim());
        }
        Assert.assertEquals(requiredType, userError.getClass());
    }
}
