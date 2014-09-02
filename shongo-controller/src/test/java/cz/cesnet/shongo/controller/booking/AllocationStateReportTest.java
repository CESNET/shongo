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
        String resourceId = createResource(resource);

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
     * Test {@link AllocationStateReport.ReusementInvalidSlot}
     */
    @Test
    public void testPermanentRoomCapacityInvalidSlot() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.setAllocatable(true);
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        createResource(mcu);

        ReservationRequest permanentRoomReservationRequest = new ReservationRequest();
        permanentRoomReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        permanentRoomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        permanentRoomReservationRequest.setSpecification(new RoomSpecification(Technology.H323));
        permanentRoomReservationRequest.setReusement(ReservationRequestReusement.OWNED);
        String permanentRoomReservationRequestId = allocate(permanentRoomReservationRequest);
        checkAllocated(permanentRoomReservationRequestId);

        ReservationRequest capacityReservationRequest = new ReservationRequest();
        capacityReservationRequest.setSlot("2013-01-01T00:00", "PT2H");
        capacityReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        capacityReservationRequest.setReusedReservationRequestId(permanentRoomReservationRequestId, true);
        capacityReservationRequest.setSpecification(new RoomSpecification(5));
        String capacityReservationRequestId = allocate(capacityReservationRequest);
        checkAllocationFailed(capacityReservationRequestId);

        finish(capacityReservationRequestId, AllocationStateReport.ReusementInvalidSlot.class);
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
        String resourceId = createResource(resource);

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

    /**
     * Test {@link AllocationStateReport.ReusementAlreadyUsed}
     */
    @Test
    public void testPermanentRoomAlreadyUsed() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.setAllocatable(true);
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        createResource(mcu);

        ReservationRequest permanentRoomReservationRequest = new ReservationRequest();
        permanentRoomReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        permanentRoomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        permanentRoomReservationRequest.setSpecification(new RoomSpecification(Technology.H323));
        permanentRoomReservationRequest.setReusement(ReservationRequestReusement.OWNED);
        String permanentRoomReservationRequestId = allocate(permanentRoomReservationRequest);
        checkAllocated(permanentRoomReservationRequestId);

        ReservationRequest capacityReservationRequest1 = new ReservationRequest();
        capacityReservationRequest1.setSlot("2012-03-01T14:00", "PT2H");
        capacityReservationRequest1.setPurpose(ReservationRequestPurpose.SCIENCE);
        capacityReservationRequest1.setReusedReservationRequestId(permanentRoomReservationRequestId, true);
        capacityReservationRequest1.setSpecification(new RoomSpecification(5));
        String capacityReservationRequestId1 = allocate(capacityReservationRequest1);
        checkAllocated(capacityReservationRequestId1);

        ReservationRequest capacityReservationRequest2 = new ReservationRequest();
        capacityReservationRequest2.setSlot("2012-03-01T14:00", "PT2H");
        capacityReservationRequest2.setPurpose(ReservationRequestPurpose.SCIENCE);
        capacityReservationRequest2.setReusedReservationRequestId(permanentRoomReservationRequestId, true);
        capacityReservationRequest2.setSpecification(new RoomSpecification(5));
        String capacityReservationRequestId2 = allocate(capacityReservationRequest2);
        checkAllocationFailed(capacityReservationRequestId2);

        finish(capacityReservationRequestId2, AllocationStateReport.ReusementAlreadyUsed.class);
    }

    @Test
    public void testExceedMaximumFuture() throws Exception
    {
        Resource resource1 = new Resource();
        resource1.setName("resource");
        resource1.setAllocatable(true);
        resource1.setMaximumFuture(new DateTime("2012-11-01T00:00"));
        resource1.addCapability(new AliasProviderCapability("test", AliasType.ROOM_NAME));
        createResource(resource1);

        Resource resource2 = new Resource();
        resource2.setName("resource");
        resource2.setAllocatable(false);
        resource2.setMaximumFuture(new DateTime("2012-11-01T00:00"));
        resource2.addCapability(new AliasProviderCapability("test", AliasType.ROOM_NAME));
        createResource(resource2);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2013-01-01T00:00", "PT1H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(AliasType.ROOM_NAME));
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
        createResource(aliasProviderFirst);

        Resource aliasProviderSecond = new Resource();
        aliasProviderSecond.setName("aliasProvider2");
        aliasProviderSecond.setAllocatable(true);
        aliasProviderSecond.setMaximumFuture(new DateTime("2012-12-01T00:00"));
        aliasProviderSecond.addCapability(new AliasProviderCapability("{hash}", AliasType.ROOM_NAME));
        createResource(aliasProviderSecond);

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
        createResource(roomProviderFirst);

        DeviceResource roomProviderSecond = new DeviceResource();
        roomProviderSecond.setName("roomProvider2");
        roomProviderSecond.setAllocatable(true);
        roomProviderSecond.setMaximumFuture(new DateTime("2012-12-01T00:00"));
        roomProviderSecond.addTechnology(Technology.H323);
        roomProviderSecond.addCapability(new RoomProviderCapability(20));
        createResource(roomProviderSecond);

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
        createResource(roomProvider);

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
        createResource(roomProviderFirst);

        DeviceResource roomProviderSecond = new DeviceResource();
        roomProviderSecond.setName("roomProvider2");
        roomProviderSecond.setAllocatable(true);
        roomProviderSecond.addTechnology(Technology.H323);
        roomProviderSecond.addCapability(new AliasProviderCapability("{hash}", AliasType.ROOM_NAME));
        roomProviderSecond.addCapability(new RoomProviderCapability(5, new AliasType[]{AliasType.ROOM_NAME}));
        createResource(roomProviderSecond);

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
        createResource(aliasProvider);

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
        createResource(aliasProvider);

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
     * Test {@link AllocationStateReport.ResourceNotFound.Type#RECORDING}
     */
    @Test
    public void testRecordingNotFound() throws Exception
    {
        DeviceResource roomProvider = new DeviceResource();
        roomProvider.setName("roomProvider");
        roomProvider.setAllocatable(true);
        roomProvider.addTechnology(Technology.H323);
        roomProvider.addCapability(new AliasProviderCapability("{hash}", AliasType.ROOM_NAME));
        roomProvider.addCapability(new RoomProviderCapability(10, new AliasType[]{AliasType.ROOM_NAME}));
        createResource(roomProvider);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1D");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification(Technology.H323);
        RoomAvailability roomAvailability = roomSpecification.createAvailability();
        roomAvailability.setParticipantCount(5);
        roomAvailability.addServiceSpecification(new RecordingServiceSpecification(true));
        reservationRequest.setSpecification(roomSpecification);
        String reservationRequestId = allocate(reservationRequest);
        checkAllocationFailed(reservationRequestId);

        AllocationStateReport.ResourceNotFound resourceNotFound =
                finish(reservationRequestId, AllocationStateReport.ResourceNotFound.class);
        Assert.assertEquals(AllocationStateReport.ResourceNotFound.Type.RECORDING, resourceNotFound.getType());
    }

    /**
     * Test {@link AllocationStateReport.RecordingCapacityExceeded}
     */
    @Test
    public void testRecordingCapacityExceeded() throws Exception
    {
        DeviceResource roomProvider = new DeviceResource();
        roomProvider.setName("roomProvider");
        roomProvider.setAllocatable(true);
        roomProvider.addTechnology(Technology.H323);
        roomProvider.addCapability(new AliasProviderCapability("{hash}", AliasType.ROOM_NAME));
        roomProvider.addCapability(new RoomProviderCapability(10, new AliasType[]{AliasType.ROOM_NAME}));
        roomProvider.addCapability(new RecordingCapability(1));
        createResource(roomProvider);

        ReservationRequest reservationRequestFirst = new ReservationRequest();
        reservationRequestFirst.setSlot("2012-01-01T00:00", "P1D");
        reservationRequestFirst.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecificationFirst = new RoomSpecification(Technology.H323);
        RoomAvailability roomAvailabilityFirst = roomSpecificationFirst.createAvailability();
        roomAvailabilityFirst.setParticipantCount(5);
        roomAvailabilityFirst.addServiceSpecification(new RecordingServiceSpecification(true));
        reservationRequestFirst.setSpecification(roomSpecificationFirst);
        allocateAndCheck(reservationRequestFirst);

        ReservationRequest reservationRequestSecond = new ReservationRequest();
        reservationRequestSecond.setSlot("2012-01-01T00:00", "P1D");
        reservationRequestSecond.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecificationSecond = new RoomSpecification(Technology.H323);
        RoomAvailability roomAvailabilitySecond = roomSpecificationSecond.createAvailability();
        roomAvailabilitySecond.setParticipantCount(5);
        roomAvailabilitySecond.addServiceSpecification(new RecordingServiceSpecification(true));
        reservationRequestSecond.setSpecification(roomSpecificationSecond);
        String reservationRequestSecondId = allocate(reservationRequestSecond);
        checkAllocationFailed(reservationRequestSecondId);

        finish(reservationRequestSecondId, AllocationStateReport.RecordingCapacityExceeded.class);
    }

    /**
     * Test {@link AllocationStateReport.RecordingRoomCapacityExceed}
     */
    @Test
    public void testRecordingRoomCapacityExceeded() throws Exception
    {
        DeviceResource roomProvider = new DeviceResource();
        roomProvider.setName("roomProvider");
        roomProvider.setAllocatable(true);
        roomProvider.addTechnology(Technology.H323);
        roomProvider.addCapability(new AliasProviderCapability("{hash}", AliasType.ROOM_NAME));
        roomProvider.addCapability(new RoomProviderCapability(5, new AliasType[]{AliasType.ROOM_NAME}));
        createResource(roomProvider);

        DeviceResource recorder = new DeviceResource();
        recorder.setName("recorder");
        recorder.setAllocatable(true);
        recorder.addTechnology(Technology.H323);
        recorder.addCapability(new RecordingCapability(1));
        createResource(recorder);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T00:00", "P1D");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification(Technology.H323);
        RoomAvailability roomAvailability = roomSpecification.createAvailability();
        roomAvailability.setParticipantCount(5);
        roomAvailability.addServiceSpecification(new RecordingServiceSpecification(true));
        reservationRequest.setSpecification(roomSpecification);
        String reservationRequestSecondId = allocate(reservationRequest);
        checkAllocationFailed(reservationRequestSecondId);

        finish(reservationRequestSecondId, AllocationStateReport.RecordingRoomCapacityExceed.class);
    }

    /**
     * Perform check.
     *
     * @param reservationRequestId
     * @param requiredType
     */
    private <T extends AllocationStateReport.UserError> T finish(String reservationRequestId, Class<T> requiredType)
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
        return requiredType.cast(userError);
    }
}
