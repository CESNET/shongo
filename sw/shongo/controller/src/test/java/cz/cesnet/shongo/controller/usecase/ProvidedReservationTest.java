package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ControllerReportSet;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import org.junit.Assert;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * Tests for allocation of single virtual room in a {@link cz.cesnet.shongo.controller.executor.Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ProvidedReservationTest extends AbstractControllerTest
{
    @Test
    public void testTerminal() throws Exception
    {
        DeviceResource terminal = new DeviceResource();
        terminal.setName("terminal");
        terminal.setAllocatable(true);
        terminal.addTechnology(Technology.H323);
        terminal.addCapability(new TerminalCapability());
        String terminalId = getResourceService().createResource(SECURITY_TOKEN, terminal);

        ReservationRequest terminalReservationRequest = new ReservationRequest();
        terminalReservationRequest.setSlot("2012-01-01T00:00", "P1D");
        terminalReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        terminalReservationRequest.setSpecification(new ExistingEndpointSpecification(terminalId));
        Reservation terminalReservation = allocateAndCheck(terminalReservationRequest);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-01-01T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ExistingEndpointSpecification(terminalId));
        reservationRequest.addProvidedReservationId(terminalReservation.getId());

        String id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        runScheduler();
        Reservation reservation = checkAllocated(id);
        assertEquals(ExistingReservation.class, reservation.getClass());
        ExistingReservation existingReservation = (ExistingReservation) reservation;
        assertEquals(terminalReservation.getId(), existingReservation.getReservation().getId());
    }

    @Test
    public void testTerminalWithParent() throws Exception
    {
        Resource lectureRoom = new Resource();
        lectureRoom.setName("lectureRoom");
        lectureRoom.setAllocatable(true);
        String lectureRoomId = getResourceService().createResource(SECURITY_TOKEN, lectureRoom);

        DeviceResource terminal = new DeviceResource();
        terminal.setName("terminal");
        terminal.setParentResourceId(lectureRoomId);
        terminal.setAllocatable(true);
        terminal.addTechnology(Technology.H323);
        terminal.addCapability(new TerminalCapability());
        String terminalId = getResourceService().createResource(SECURITY_TOKEN, terminal);

        ReservationRequest lectureRoomReservationRequest = new ReservationRequest();
        lectureRoomReservationRequest.setSlot("2012-01-01T00:00", "P1D");
        lectureRoomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        lectureRoomReservationRequest.setSpecification(new ResourceSpecification(lectureRoomId));
        Reservation lectureRoomReservation = allocateAndCheck(lectureRoomReservationRequest);

        ReservationRequest request = new ReservationRequest();
        request.setSlot("2012-01-01T14:00", "PT2H");
        request.setSpecification(new ExistingEndpointSpecification(terminalId));
        request.setPurpose(ReservationRequestPurpose.SCIENCE);
        String id = getReservationService().createReservationRequest(SECURITY_TOKEN, request);
        runScheduler();
        checkAllocationFailed(id);

        request = (ReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN, id);
        request.addProvidedReservationId(lectureRoomReservation.getId());

        Reservation reservation = allocateAndCheck(request);
        assertEquals(1, reservation.getChildReservationIds().size());
        Reservation childReservation = getReservationService().getReservation(SECURITY_TOKEN,
                reservation.getChildReservationIds().get(0));
        assertEquals(ExistingReservation.class, childReservation.getClass());
        ExistingReservation childExistingReservation = (ExistingReservation) childReservation;
        assertEquals(lectureRoomReservation.getId(), childExistingReservation.getReservation().getId());
    }

    @Test
    public void testAlias() throws Exception
    {
        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability("95{digit:1}", AliasType.H323_E164));
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        aliasReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        aliasReservationRequest.setSpecification(new AliasSpecification(AliasType.H323_E164));
        Reservation aliasReservation = allocateAndCheck(aliasReservationRequest);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(Technology.H323));
        reservationRequest.addProvidedReservationId(aliasReservation.getId());

        Reservation reservation = allocateAndCheck(reservationRequest);
        assertEquals(ExistingReservation.class, reservation.getClass());
        ExistingReservation existingReservation = (ExistingReservation) reservation;
        assertEquals(aliasReservation.getId(), existingReservation.getReservation().getId());
    }

    @Test
    public void testAliasInCompartment() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.setAllocatable(true);
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(100));
        getResourceService().createResource(SECURITY_TOKEN, mcu);

        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability("950000001", AliasType.H323_E164));
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        aliasReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        aliasReservationRequest.setSpecification(new AliasSpecification(AliasType.H323_E164));
        String aliasReservationRequestId = allocate(aliasReservationRequest);
        AliasReservation aliasReservation = (AliasReservation) checkAllocated(aliasReservationRequestId);
        assertEquals(aliasReservation.getValue(), "950000001");

        ReservationRequest compartmentReservationRequest = new ReservationRequest();
        compartmentReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        compartmentReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 3));
        compartmentReservationRequest.setSpecification(compartmentSpecification);
        compartmentReservationRequest.addProvidedReservationId(aliasReservation.getId());

        allocateAndCheck(compartmentReservationRequest);
        try {
            getReservationService().deleteReservationRequest(SECURITY_TOKEN, aliasReservationRequestId);
            fail("Exception that reservation request cannot be deleted should be thrown");
        }
        catch (ControllerReportSet.ReservationRequestNotModifiableException exception) {
        }
    }

    @Test
    public void testUseOnlyValidProvidedReservations() throws Exception
    {
        DeviceResource terminal = new DeviceResource();
        terminal.setName("terminal");
        terminal.addTechnology(Technology.H323);
        terminal.addCapability(new TerminalCapability());
        terminal.setAllocatable(true);
        String terminalId = getResourceService().createResource(SECURITY_TOKEN, terminal);

        ReservationRequest terminalReservationRequest = new ReservationRequest();
        terminalReservationRequest.setSlot("2012-06-22T00:00", "PT15H");
        terminalReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        terminalReservationRequest.setSpecification(new ExistingEndpointSpecification(terminalId));
        Reservation terminalReservation = allocateAndCheck(terminalReservationRequest);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ExistingEndpointSpecification(terminalId));
        reservationRequest.addProvidedReservationId(terminalReservation.getId());

        allocateAndCheckFailed(reservationRequest);
    }

    @Test
    public void testProvidedReservationsFromSet() throws Exception
    {
        DeviceResource terminal = new DeviceResource();
        terminal.setName("terminal");
        terminal.addTechnology(Technology.H323);
        terminal.addCapability(new TerminalCapability());
        terminal.setAllocatable(true);
        String terminalId = getResourceService().createResource(SECURITY_TOKEN, terminal);

        ReservationRequest terminalReservationRequest = new ReservationRequest();
        terminalReservationRequest.setSlot("2012-01-01T00:00", "P1D");
        terminalReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        terminalReservationRequest.setSpecification(new ExistingEndpointSpecification(terminalId));
        Reservation terminalReservation = allocateAndCheck(terminalReservationRequest);

        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.addSlot("2012-01-01T14:00", "PT2H");
        reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSet.setSpecification(new ExistingEndpointSpecification(terminalId));
        reservationRequestSet.addProvidedReservationId(terminalReservation.getId());

        Reservation reservation = allocateAndCheck(reservationRequestSet);
        assertEquals(ExistingReservation.class, reservation.getClass());
        ExistingReservation existingReservation = (ExistingReservation) reservation;
        assertEquals(terminalReservation.getId(), existingReservation.getReservation().getId());
    }

    @Test
    public void testProvidedRoomReservations() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.addCapability(new AliasProviderCapability("950000001", AliasType.H323_E164).withRestrictedToResource());
        mcu.setAllocatable(true);
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest roomReservationRequest = new ReservationRequest();
        roomReservationRequest.setSlot("2012-01-01T00:00", "P1D");
        roomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.setParticipantCount(10);
        roomReservationRequest.setSpecification(roomSpecification);

        Reservation roomReservation = allocateAndCheck(roomReservationRequest);

        ReservationRequest compartmentReservationRequest = new ReservationRequest();
        compartmentReservationRequest.setSlot("2012-01-01T14:00", "PT2H");
        compartmentReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 3));
        compartmentReservationRequest.setSpecification(compartmentSpecification);
        compartmentReservationRequest.addProvidedReservationId(roomReservation.getId());

        allocateAndCheck(compartmentReservationRequest);
    }

    @Test
    public void testCollision() throws Exception
    {
        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability("950000001", AliasType.H323_E164));
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        aliasReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        aliasReservationRequest.setSpecification(new AliasSpecification(AliasType.H323_E164));
        Reservation aliasReservation = allocateAndCheck(aliasReservationRequest);

        ReservationRequest firstReservationRequest = new ReservationRequest();
        firstReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        firstReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        firstReservationRequest.setSpecification(new AliasSpecification(Technology.H323));
        firstReservationRequest.addProvidedReservationId(aliasReservation.getId());
        allocateAndCheck(firstReservationRequest);

        ReservationRequest secondReservationRequest = new ReservationRequest();
        secondReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        secondReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        secondReservationRequest.setSpecification(new AliasSpecification(Technology.H323));
        secondReservationRequest.addProvidedReservationId(aliasReservation.getId());
        allocateAndCheckFailed(secondReservationRequest);
    }

    /**
     * Test allocating {@link AliasReservation} from {@link AliasProviderCapability}
     * with {@link AliasProviderCapability#RESTRICTED_TO_RESOURCE} set to {@code true}.
     * <p/>
     * Then the allocated {@link AliasReservation} is provided to two {@link ReservationRequest} where the first
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
        String connectServerFirstId = getResourceService().createResource(SECURITY_TOKEN, connectServerFirst);

        DeviceResource connectServerSecond = new DeviceResource();
        connectServerSecond.setName("connectServerSecond");
        connectServerSecond.setAllocatable(true);
        connectServerSecond.addTechnology(Technology.ADOBE_CONNECT);
        connectServerSecond.addCapability(new RoomProviderCapability(10, new AliasType[]{AliasType.ADOBE_CONNECT_URI}));
        String connectServerSecondId = getResourceService().createResource(SECURITY_TOKEN, connectServerSecond);

        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        aliasReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        aliasReservationRequest.setSpecification(new AliasSpecification(AliasType.ADOBE_CONNECT_URI));
        Reservation aliasReservation = allocateAndCheck(aliasReservationRequest);

        ReservationRequest firstReservationRequest = new ReservationRequest();
        firstReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        firstReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        firstReservationRequest.setSpecification(
                new RoomSpecification(10, Technology.ADOBE_CONNECT, connectServerSecondId));
        firstReservationRequest.addProvidedReservationId(aliasReservation.getId());
        // Should not be allocated because the provided alias is restricted to the first server
        allocateAndCheckFailed(firstReservationRequest);

        ReservationRequest secondReservationRequest = new ReservationRequest();
        secondReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        secondReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        secondReservationRequest.setSpecification(new RoomSpecification(10, Technology.ADOBE_CONNECT,
                connectServerFirstId));
        secondReservationRequest.addProvidedReservationId(aliasReservation.getId());
        allocateAndCheck(secondReservationRequest);
    }

    /**
     * Test that a reservation request with {@link cz.cesnet.shongo.controller.api.AliasSetSpecification} cannot be modified when the allocated
     * {@link AliasReservation}s are reused in other reservation request (e.g., {@link RoomReservation}).
     *
     * @throws Exception
     */
    @Test
    public void testProvidedAliasSetNotModifiable() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.setAllocatable(true);
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(new RoomProviderCapability(100, new AliasType[]{AliasType.H323_E164, AliasType.SIP_URI}));
        mcu.addCapability(new AliasProviderCapability("001", AliasType.H323_E164).withRestrictedToResource());
        mcu.addCapability(new AliasProviderCapability("001@cesnet.cz", AliasType.SIP_URI).withRestrictedToResource());
        getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        aliasReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        aliasReservationRequest.setSpecification(
                new AliasSetSpecification(new AliasType[]{AliasType.H323_E164, AliasType.SIP_URI}));
        String aliasReservationRequestId = allocate(aliasReservationRequest);
        Reservation aliasReservation = checkAllocated(aliasReservationRequestId);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(
                new RoomSpecification(5, new Technology[]{Technology.H323, Technology.SIP}));
        reservationRequest.addProvidedReservationId(aliasReservation.getId());
        String reservationRequestId = allocate(reservationRequest);
        checkAllocated(reservationRequestId);

        try {
            getReservationService().modifyReservationRequest(SECURITY_TOKEN,
                    getReservationService().getReservationRequest(SECURITY_TOKEN, aliasReservationRequestId));
            fail("Exception that reservation request cannot be modified should be thrown");
        }
        catch (ControllerReportSet.ReservationRequestNotModifiableException exception) {
        }

        try {
            getReservationService().deleteReservationRequest(SECURITY_TOKEN, aliasReservationRequestId);
            fail("Exception that reservation request cannot be deleted should be thrown");
        }
        catch (ControllerReportSet.ReservationRequestNotModifiableException exception) {
        }
    }

    @Test
    public void testAliasRoomCapacity() throws Exception
    {
        DeviceResource connectServer = new DeviceResource();
        connectServer.setName("connectServer");
        connectServer.setAllocatable(true);
        connectServer.setAddress("127.0.0.1");
        connectServer.addTechnology(Technology.ADOBE_CONNECT);
        connectServer.addCapability(new RoomProviderCapability(10,
                new AliasType[]{AliasType.ROOM_NAME, AliasType.ADOBE_CONNECT_URI}));
        connectServer.addCapability(new AliasProviderCapability(
                "test", AliasType.ADOBE_CONNECT_URI, "{device.address}/{value}").withPermanentRoom());
        connectServer.addCapability(new AliasProviderCapability(
                "test", AliasType.ROOM_NAME).withPermanentRoom());
        getResourceService().createResource(SECURITY_TOKEN, connectServer);

        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        aliasReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        AliasSetSpecification aliasSetSpecification = new AliasSetSpecification();
        aliasSetSpecification.setSharedExecutable(true);
        aliasSetSpecification.addAlias(new AliasSpecification(AliasType.ADOBE_CONNECT_URI));
        aliasSetSpecification.addAlias(new AliasSpecification(AliasType.ROOM_NAME));
        aliasReservationRequest.setSpecification(aliasSetSpecification);
        String aliasReservationRequestId = allocate(aliasReservationRequest);
        Reservation aliasReservation = checkAllocated(aliasReservationRequestId);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(
                new RoomSpecification(10, Technology.ADOBE_CONNECT));
        reservationRequest.addProvidedReservationId(aliasReservation.getId());
        String reservationRequestId = allocate(reservationRequest);
        checkAllocated(reservationRequestId);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, reservationRequestId);

        runScheduler();

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, aliasReservationRequestId);
    }
}
