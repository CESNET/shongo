package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.fault.EntityToDeleteIsReferencedException;
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
        terminalReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        terminalReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        terminalReservationRequest.setSpecification(new ExistingEndpointSpecification(terminalId));
        Reservation terminalReservation = allocateAndCheck(terminalReservationRequest);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
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
        lectureRoomReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        lectureRoomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        lectureRoomReservationRequest.setSpecification(new ResourceSpecification(lectureRoomId));
        Reservation lectureRoomReservation = allocateAndCheck(lectureRoomReservationRequest);

        ReservationRequest request = new ReservationRequest();
        request.setSlot("2012-06-22T14:00", "PT2H");
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
        aliasProvider.addCapability(new AliasProviderCapability(AliasType.H323_E164, "95000000[d]"));
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
        aliasProvider.addCapability(new AliasProviderCapability(AliasType.H323_E164, "950000001"));
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        aliasReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        aliasReservationRequest.setSpecification(new AliasSpecification(AliasType.H323_E164));
        String aliasReservationRequestId = allocate(aliasReservationRequest);
        AliasReservation aliasReservation = (AliasReservation) checkAllocated(aliasReservationRequestId);
        assertEquals(aliasReservation.getAliasValue(), "950000001");

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
            fail("Exception that reservation request is still referenced should be thrown");
        }
        catch (EntityToDeleteIsReferencedException exception) {
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
        terminalReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        terminalReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        terminalReservationRequest.setSpecification(new ExistingEndpointSpecification(terminalId));
        Reservation terminalReservation = allocateAndCheck(terminalReservationRequest);

        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.addSlot(new DateTimeSlot("2012-06-22T14:00", "PT2H"));
        reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSet.addSpecification(new ExistingEndpointSpecification(terminalId));
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
        mcu.addCapability(new AliasProviderCapability(AliasType.H323_E164, "950000001", true));
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
        aliasProvider.addCapability(new AliasProviderCapability(AliasType.H323_E164, "950000001"));
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
}
