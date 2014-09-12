package cz.cesnet.shongo.controller.booking;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestReusement;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.AvailabilityCheckRequest;
import cz.cesnet.shongo.controller.api.rpc.ReservationService;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for allocation of single virtual room in a {@link cz.cesnet.shongo.controller.booking.compartment.Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReusementTest extends AbstractControllerTest
{
    @Test
    public void testTerminal() throws Exception
    {
        DeviceResource terminal = new DeviceResource();
        terminal.setName("terminal");
        terminal.setAllocatable(true);
        terminal.addTechnology(Technology.H323);
        terminal.addCapability(new TerminalCapability());
        String terminalId = createResource(terminal);

        ReservationRequest terminalReservationRequest = new ReservationRequest();
        terminalReservationRequest.setSlot("2012-01-01T00:00", "P1D");
        terminalReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        terminalReservationRequest.setSpecification(new ResourceSpecification(terminalId));
        terminalReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String terminalReservationRequestId = allocate(terminalReservationRequest);
        Reservation terminalReservation = checkAllocated(terminalReservationRequestId);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ResourceSpecification(terminalId));
        reservationRequest.setReusedReservationRequestId(terminalReservationRequestId);

        String id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        runScheduler();
        Reservation reservation = checkAllocated(id);
        Assert.assertEquals(ExistingReservation.class, reservation.getClass());
        ExistingReservation existingReservation = (ExistingReservation) reservation;
        Assert.assertEquals(terminalReservation.getId(), existingReservation.getReservation().getId());
    }

    @Test
    public void testTerminalWithParent() throws Exception
    {
        Resource lectureRoom = new Resource();
        lectureRoom.setName("lectureRoom");
        lectureRoom.setAllocatable(true);
        String lectureRoomId = createResource(lectureRoom);

        DeviceResource terminal = new DeviceResource();
        terminal.setName("terminal");
        terminal.setParentResourceId(lectureRoomId);
        terminal.setAllocatable(true);
        terminal.addTechnology(Technology.H323);
        terminal.addCapability(new TerminalCapability());
        String terminalId = createResource(terminal);

        ReservationRequest lectureRoomReservationRequest = new ReservationRequest();
        lectureRoomReservationRequest.setSlot("2012-01-01T00:00", "P1D");
        lectureRoomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        lectureRoomReservationRequest.setSpecification(new ResourceSpecification(lectureRoomId));
        lectureRoomReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String lectureRoomReservationRequestId = allocate(lectureRoomReservationRequest);
        Reservation lectureRoomReservation = checkAllocated(lectureRoomReservationRequestId);

        ReservationRequest request = new ReservationRequest();
        request.setSlot("2012-01-01T14:00", "PT2H");
        request.setSpecification(new ResourceSpecification(terminalId));
        request.setPurpose(ReservationRequestPurpose.SCIENCE);
        String id = getReservationService().createReservationRequest(SECURITY_TOKEN, request);
        runScheduler();
        checkAllocationFailed(id);

        request = getReservationRequest(id, ReservationRequest.class);
        request.setReusedReservationRequestId(lectureRoomReservationRequestId);

        Reservation reservation = allocateAndCheck(request);
        Assert.assertEquals(1, reservation.getChildReservationIds().size());
        Reservation childReservation = getReservationService().getReservation(SECURITY_TOKEN,
                reservation.getChildReservationIds().get(0));
        Assert.assertEquals(ExistingReservation.class, childReservation.getClass());
        ExistingReservation childExistingReservation = (ExistingReservation) childReservation;
        Assert.assertEquals(lectureRoomReservation.getId(), childExistingReservation.getReservation().getId());
    }

    @Test
    public void testAlias() throws Exception
    {
        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability("95{digit:1}", AliasType.H323_E164));
        createResource(aliasProvider);

        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        aliasReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        aliasReservationRequest.setSpecification(new AliasSpecification(AliasType.H323_E164));
        aliasReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String aliasReservationRequestId = allocate(aliasReservationRequest);
        Reservation aliasReservation = checkAllocated(aliasReservationRequestId);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(Technology.H323));
        reservationRequest.setReusedReservationRequestId(aliasReservationRequestId);

        String reservationRequestId = allocate(reservationRequest);
        Reservation reservation = checkAllocated(reservationRequestId);
        Assert.assertEquals(ExistingReservation.class, reservation.getClass());
        ExistingReservation existingReservation = (ExistingReservation) reservation;
        Assert.assertEquals(aliasReservation.getId(), existingReservation.getReservation().getId());

        ReservationService service = getReservationService();
        reservationRequest = (ReservationRequest) service.getReservationRequest(SECURITY_TOKEN, reservationRequestId);
        reservationRequestId = allocate(reservationRequest);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, reservationRequestId);
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, aliasReservationRequestId);
        runScheduler();
    }

    @Test
    public void testAliasInCompartment() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.setAllocatable(true);
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(100));
        createResource(mcu);

        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability("950000001", AliasType.H323_E164));
        createResource(aliasProvider);

        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        aliasReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        aliasReservationRequest.setSpecification(new AliasSpecification(AliasType.H323_E164));
        aliasReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String aliasReservationRequestId = allocate(aliasReservationRequest);
        AliasReservation aliasReservation = (AliasReservation) checkAllocated(aliasReservationRequestId);
        Assert.assertEquals(aliasReservation.getValue(), "950000001");

        ReservationRequest compartmentReservationRequest = new ReservationRequest();
        compartmentReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        compartmentReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addParticipant(new ExternalEndpointSetParticipant(Technology.H323, 3));
        compartmentReservationRequest.setSpecification(compartmentSpecification);
        compartmentReservationRequest.setReusedReservationRequestId(aliasReservationRequestId);

        allocateAndCheck(compartmentReservationRequest);
        try {
            getReservationService().deleteReservationRequest(SECURITY_TOKEN, aliasReservationRequestId);
            Assert.fail("Exception that reservation request cannot be deleted should be thrown");
        }
        catch (ControllerReportSet.ReservationRequestNotDeletableException exception) {
        }
    }

    @Test
    public void testUseOnlyValidReusableReservations() throws Exception
    {
        DeviceResource terminal = new DeviceResource();
        terminal.setName("terminal");
        terminal.addTechnology(Technology.H323);
        terminal.addCapability(new TerminalCapability());
        terminal.setAllocatable(true);
        String terminalId = createResource(terminal);

        ReservationRequest terminalReservationRequest = new ReservationRequest();
        terminalReservationRequest.setSlot("2012-06-22T00:00", "PT15H");
        terminalReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        terminalReservationRequest.setSpecification(new ResourceSpecification(terminalId));
        terminalReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String terminalReservationRequestId = allocate(terminalReservationRequest);
        checkAllocated(terminalReservationRequestId);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ResourceSpecification(terminalId));
        reservationRequest.setReusedReservationRequestId(terminalReservationRequestId);

        allocateAndCheckFailed(reservationRequest);
    }

    @Test
    public void testReusedReservationsFromSet() throws Exception
    {
        DeviceResource terminal = new DeviceResource();
        terminal.setName("terminal");
        terminal.addTechnology(Technology.H323);
        terminal.addCapability(new TerminalCapability());
        terminal.setAllocatable(true);
        String terminalId = createResource(terminal);

        ReservationRequest terminalReservationRequest = new ReservationRequest();
        terminalReservationRequest.setSlot("2012-01-01T00:00", "P1D");
        terminalReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        terminalReservationRequest.setSpecification(new ResourceSpecification(terminalId));
        terminalReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String terminalReservationRequestId = allocate(terminalReservationRequest);
        Reservation terminalReservation = checkAllocated(terminalReservationRequestId);

        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.addSlot("2012-01-01T14:00", "PT2H");
        reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSet.setSpecification(new ResourceSpecification(terminalId));
        reservationRequestSet.setReusedReservationRequestId(terminalReservationRequestId);

        Reservation reservation = allocateAndCheck(reservationRequestSet);
        Assert.assertEquals(ExistingReservation.class, reservation.getClass());
        ExistingReservation existingReservation = (ExistingReservation) reservation;
        Assert.assertEquals(terminalReservation.getId(), existingReservation.getReservation().getId());
    }

    @Test
    public void testReusedRoomReservations() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.addCapability(new AliasProviderCapability("950000001", AliasType.H323_E164).withRestrictedToResource());
        mcu.setAllocatable(true);
        createResource(mcu);

        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot("2012-01-01T00:00", "P1D");
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        roomReservationRequest.setSpecification(new RoomSpecification(10, Technology.H323));
        roomReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String roomReservationRequestId = allocate(roomReservationRequest);
        checkAllocated(roomReservationRequestId);

        ReservationRequest compartmentReservationRequest = new ReservationRequest();
        compartmentReservationRequest.setSlot("2012-01-01T14:00", "PT2H");
        compartmentReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addParticipant(new ExternalEndpointSetParticipant(Technology.H323, 3));
        compartmentReservationRequest.setSpecification(compartmentSpecification);
        compartmentReservationRequest.setReusedReservationRequestId(roomReservationRequestId);

        allocateAndCheck(compartmentReservationRequest);
    }

    @Test
    public void testCollision() throws Exception
    {
        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability("950000001", AliasType.H323_E164));
        createResource(aliasProvider);

        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        aliasReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        aliasReservationRequest.setSpecification(new AliasSpecification(AliasType.H323_E164));
        aliasReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String aliasReservationRequestId = allocate(aliasReservationRequest);
        checkAllocated(aliasReservationRequestId);

        ReservationRequest firstReservationRequest = new ReservationRequest();
        firstReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        firstReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        firstReservationRequest.setSpecification(new AliasSpecification(Technology.H323));
        firstReservationRequest.setReusedReservationRequestId(aliasReservationRequestId);
        allocateAndCheck(firstReservationRequest);

        ReservationRequest secondReservationRequest = new ReservationRequest();
        secondReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        secondReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        secondReservationRequest.setSpecification(new AliasSpecification(Technology.H323));
        secondReservationRequest.setReusedReservationRequestId(aliasReservationRequestId);
        allocateAndCheckFailed(secondReservationRequest);
    }

    /**
     * Test allocating {@link AliasReservation} from {@link AliasProviderCapability}
     * with {@link AliasProviderCapability#RESTRICTED_TO_RESOURCE} set to {@code true}.
     * <p/>
     * Then the allocated {@link AliasReservation} is reused by two {@link ReservationRequest} where the first
     * specify different {@link AliasProviderCapability} and it fails and the second specify proper
     * {@link AliasProviderCapability} and it succeeds.
     *
     * @throws Exception
     */
    @Test
    public void testRestrictedAlias() throws Exception
    {
        DeviceResource connectServerFirst = new DeviceResource();
        connectServerFirst.setName("connectServerFirst");
        connectServerFirst.setAllocatable(true);
        connectServerFirst.setAddress("127.0.0.1");
        connectServerFirst.addTechnology(Technology.ADOBE_CONNECT);
        connectServerFirst.addCapability(new RoomProviderCapability(10, new AliasType[]{AliasType.ADOBE_CONNECT_URI}));
        // Generates only single "test" alias for this connect server
        connectServerFirst.addCapability(new AliasProviderCapability(
                "test", AliasType.ADOBE_CONNECT_URI, "{device.address}/{value}").withRestrictedToResource());
        String connectServerFirstId = createResource(connectServerFirst);

        DeviceResource connectServerSecond = new DeviceResource();
        connectServerSecond.setName("connectServerSecond");
        connectServerSecond.setAllocatable(true);
        connectServerSecond.addTechnology(Technology.ADOBE_CONNECT);
        connectServerSecond.addCapability(new RoomProviderCapability(10, new AliasType[]{AliasType.ADOBE_CONNECT_URI}));
        String connectServerSecondId = createResource(connectServerSecond);

        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        aliasReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        aliasReservationRequest.setSpecification(new AliasSpecification(AliasType.ADOBE_CONNECT_URI));
        aliasReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String aliasReservationRequestId = allocate(aliasReservationRequest);
        checkAllocated(aliasReservationRequestId);

        ReservationRequest firstReservationRequest = new ReservationRequest();
        firstReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        firstReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        firstReservationRequest.setSpecification(
                new RoomSpecification(10, Technology.ADOBE_CONNECT, connectServerSecondId));
        firstReservationRequest.setReusedReservationRequestId(aliasReservationRequestId);
        // Should not be allocated because the reusable alias is restricted to the first server
        allocateAndCheckFailed(firstReservationRequest);

        ReservationRequest secondReservationRequest = new ReservationRequest();
        secondReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        secondReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        secondReservationRequest.setSpecification(
                new RoomSpecification(10, Technology.ADOBE_CONNECT, connectServerFirstId));
        secondReservationRequest.setReusedReservationRequestId(aliasReservationRequestId);
        allocateAndCheck(secondReservationRequest);
    }

    /**
     * Test that a reservation request for permanent room with capacity can be modified.
     */
    @Test
    public void testPermanentRoomWithCapacityModifiable() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.setAllocatable(true);
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(
                new RoomProviderCapability(100, new AliasType[]{AliasType.H323_E164, AliasType.SIP_URI}));
        mcu.addCapability(
                new AliasProviderCapability("{digit:3}", AliasType.H323_E164).withRestrictedToResource());
        mcu.addCapability(
                new AliasProviderCapability("{digit:3}@cesnet.cz", AliasType.SIP_URI).withRestrictedToResource());
        createResource(mcu);

        ReservationRequest permanentRoomReservationRequest = new ReservationRequest();
        permanentRoomReservationRequest.setSlot("2013-01-01T00:00", "P1Y");
        permanentRoomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        permanentRoomReservationRequest.setSpecification(
                new RoomSpecification(new AliasType[]{AliasType.H323_E164, AliasType.SIP_URI}));
        permanentRoomReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String permanentRoomReservationRequestId = allocate(permanentRoomReservationRequest);
        Reservation permanentRoomReservation = checkAllocated(permanentRoomReservationRequestId);
        RoomExecutable permanentRoomExecutable = (RoomExecutable) permanentRoomReservation.getExecutable();
        Assert.assertEquals("001", permanentRoomExecutable.getAliasByType(AliasType.H323_E164).getValue());

        ReservationRequest reservationRequest1 = new ReservationRequest();
        reservationRequest1.setSlot("2013-07-01T12:00", "PT2H");
        reservationRequest1.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest1.setReusedReservationRequestId(permanentRoomReservationRequestId, true);
        reservationRequest1.setSpecification(new RoomSpecification(5));
        String reservationRequest1Id = allocate(reservationRequest1);

        ReservationRequest reservationRequest2 = new ReservationRequest();
        reservationRequest2.setSlot("2013-07-02T12:00", "PT2H");
        reservationRequest2.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest2.setReusedReservationRequestId(permanentRoomReservationRequestId, true);
        reservationRequest2.setSpecification(new RoomSpecification(5));
        String reservationRequest2Id = allocate(reservationRequest2);

        // Check allocated capacities for permanent room
        Reservation reservation1 = checkAllocated(reservationRequest1Id);
        Reservation reservation2 = checkAllocated(reservationRequest2Id);
        AbstractRoomExecutable room1 = (AbstractRoomExecutable) reservation1.getExecutable();
        AbstractRoomExecutable room2 = (AbstractRoomExecutable) reservation2.getExecutable();
        Assert.assertEquals("001", room1.getAliasByType(AliasType.H323_E164).getValue());
        Assert.assertEquals("001", room2.getAliasByType(AliasType.H323_E164).getValue());

        // Modify permanent room alias value
        ReservationRequest reservationRequest =
                getReservationRequest(permanentRoomReservationRequestId, ReservationRequest.class);
        RoomSpecification roomSpecification =
                (RoomSpecification) reservationRequest.getSpecification();
        RoomEstablishment establishment = roomSpecification.getEstablishment();
        AliasSpecification aliasSpecification = establishment.getAliasSpecificationByType(AliasType.H323_E164);
        Assert.assertNotNull(aliasSpecification);
        aliasSpecification.setValue("555");
        permanentRoomReservationRequestId = allocate(reservationRequest, new DateTime("2013-07-01T13:00"));

        // Check allocated permanent room alias value
        permanentRoomReservation = checkAllocated(permanentRoomReservationRequestId);
        permanentRoomExecutable = (RoomExecutable) permanentRoomReservation.getExecutable();
        Assert.assertEquals(new DateTime("2013-07-01T14:00"), permanentRoomReservation.getSlot().getStart());
        Assert.assertEquals("555", permanentRoomExecutable.getAliasByType(AliasType.H323_E164).getValue());

        // Check allocated usages of alias to be updated by the reused reservation request modification
        reservation1 = checkAllocated(reservationRequest1Id);
        reservation2 = checkAllocated(reservationRequest2Id);
        room1 = (AbstractRoomExecutable) reservation1.getExecutable();
        room2 = (AbstractRoomExecutable) reservation2.getExecutable();
        Assert.assertEquals("001", room1.getAliasByType(AliasType.H323_E164).getValue());
        Assert.assertEquals("555", room2.getAliasByType(AliasType.H323_E164).getValue());

        try {
            getReservationService().deleteReservationRequest(SECURITY_TOKEN, permanentRoomReservationRequestId);
            Assert.fail("Exception that reservation request cannot be deleted should be thrown");
        }
        catch (ControllerReportSet.ReservationRequestNotDeletableException exception) {
        }

        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.addSlot("2013-07-03T12:00", "PT2H");
        reservationRequestSet.addSlot("2013-07-04T12:00", "PT2H");
        reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSet.setReusedReservationRequestId(permanentRoomReservationRequestId, true);
        reservationRequestSet.setSpecification(new RoomSpecification(5));
        String reservationRequestSetId = allocate(reservationRequestSet);
        checkAllocated(reservationRequestSetId);

        // Delete all reservation requests
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, reservationRequest1Id);
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, reservationRequest2Id);
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, reservationRequestSetId);
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, permanentRoomReservationRequestId);
        runScheduler();
    }

    @Test
    public void testPermanentRoomCapacity() throws Exception
    {
        DeviceResource connectServer = new DeviceResource();
        connectServer.setName("connectServer");
        connectServer.setAllocatable(true);
        connectServer.setAddress("127.0.0.1");
        connectServer.addTechnology(Technology.ADOBE_CONNECT);
        connectServer.addCapability(
                new RoomProviderCapability(10, new AliasType[]{AliasType.ROOM_NAME, AliasType.ADOBE_CONNECT_URI}));
        connectServer.addCapability(
                new AliasProviderCapability("test", AliasType.ADOBE_CONNECT_URI, "{device.address}/{value}"));
        connectServer.addCapability(
                new AliasProviderCapability("test", AliasType.ROOM_NAME));
        createResource(connectServer);

        ReservationRequest permanentRoomReservationRequest = new ReservationRequest();
        permanentRoomReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        permanentRoomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        permanentRoomReservationRequest.setSpecification(
                new RoomSpecification(new AliasType[]{AliasType.ROOM_NAME, AliasType.ADOBE_CONNECT_URI}));
        permanentRoomReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String permanentRoomReservationRequestId = allocate(permanentRoomReservationRequest);
        checkAllocated(permanentRoomReservationRequestId);

        ReservationRequest capacityReservationRequest = new ReservationRequest();
        capacityReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        capacityReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        capacityReservationRequest.setReusedReservationRequestId(permanentRoomReservationRequestId, true);
        capacityReservationRequest.setSpecification(new RoomSpecification(5));
        String capacityReservationRequestId = allocate(capacityReservationRequest);
        checkAllocated(capacityReservationRequestId);

        capacityReservationRequest = getReservationRequest(capacityReservationRequestId, ReservationRequest.class);
        capacityReservationRequestId = allocate(capacityReservationRequest);
        checkAllocated(capacityReservationRequestId);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, capacityReservationRequestId);
        getReservationService().deleteReservationRequest(SECURITY_TOKEN, permanentRoomReservationRequestId);
        runScheduler();
    }

    @Test
    public void testReusementMandatoryAndOptional() throws Exception
    {
        DeviceResource connect = new DeviceResource();
        connect.setName("connect");
        connect.setAllocatable(true);
        connect.addTechnology(Technology.ADOBE_CONNECT);
        connect.addCapability(new RoomProviderCapability(10));
        createResource(connect);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.setAllocatable(true);
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        createResource(mcu);

        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot("2012-01-01T00:00", "P1D");
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        roomReservationRequest.setSpecification(new RoomSpecification(10, Technology.ADOBE_CONNECT));
        roomReservationRequest.setReusement(ReservationRequestReusement.ARBITRARY);
        String roomReservationRequestId = allocate(roomReservationRequest);
        checkAllocated(roomReservationRequestId);

        ReservationRequest usageReservationRequest1 = new ReservationRequest();
        usageReservationRequest1.setSlot("2012-01-01T12:00", "PT2H");
        usageReservationRequest1.setPurpose(ReservationRequestPurpose.SCIENCE);
        usageReservationRequest1.setReusedReservationRequestId(roomReservationRequestId, true);
        usageReservationRequest1.setSpecification(new RoomSpecification(10, Technology.ADOBE_CONNECT));
        String usageReservationRequestId1 = allocate(usageReservationRequest1);
        checkAllocated(usageReservationRequestId1);

        ReservationRequest usageReservationRequest2 = new ReservationRequest();
        usageReservationRequest2.setSlot("2012-01-01T18:00", "PT2H");
        usageReservationRequest2.setPurpose(ReservationRequestPurpose.SCIENCE);
        usageReservationRequest2.setReusedReservationRequestId(roomReservationRequestId, true);
        usageReservationRequest2.setSpecification(new RoomSpecification(10, Technology.H323));
        String usageReservationRequestId2 = allocate(usageReservationRequest2);
        checkAllocationFailed(usageReservationRequestId2);

        ReservationRequest usageReservationRequest3 = new ReservationRequest();
        usageReservationRequest3.setSlot("2012-01-01T18:00", "PT2H");
        usageReservationRequest3.setPurpose(ReservationRequestPurpose.SCIENCE);
        usageReservationRequest3.setReusedReservationRequestId(roomReservationRequestId, false);
        usageReservationRequest3.setSpecification(new RoomSpecification(10, Technology.H323));
        String usageReservationRequestId3 = allocate(usageReservationRequest3);
        checkAllocated(usageReservationRequestId3);
    }
}
