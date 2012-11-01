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
        String terminalIdentifier = getResourceService().createResource(SECURITY_TOKEN, terminal);

        ReservationRequest terminalReservationRequest = new ReservationRequest();
        terminalReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        terminalReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        terminalReservationRequest.setSpecification(new ExistingEndpointSpecification(terminalIdentifier));
        Reservation terminalReservation = allocateAndCheck(terminalReservationRequest);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ExistingEndpointSpecification(terminalIdentifier));
        reservationRequest.addProvidedReservationIdentifier(terminalReservation.getIdentifier());

        String identifier = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        runScheduler();
        Reservation reservation = checkAllocated(identifier);
        assertEquals(ExistingReservation.class, reservation.getClass());
        ExistingReservation existingReservation = (ExistingReservation) reservation;
        assertEquals(terminalReservation.getIdentifier(), existingReservation.getReservation().getIdentifier());
    }

    @Test
    public void testTerminalWithParent() throws Exception
    {
        Resource lectureRoom = new Resource();
        lectureRoom.setName("lectureRoom");
        lectureRoom.setAllocatable(true);
        String lectureRoomIdentifier = getResourceService().createResource(SECURITY_TOKEN, lectureRoom);

        DeviceResource terminal = new DeviceResource();
        terminal.setName("terminal");
        terminal.setParentIdentifier(lectureRoomIdentifier);
        terminal.setAllocatable(true);
        terminal.addTechnology(Technology.H323);
        terminal.addCapability(new TerminalCapability());
        String terminalIdentifier = getResourceService().createResource(SECURITY_TOKEN, terminal);

        ReservationRequest lectureRoomReservationRequest = new ReservationRequest();
        lectureRoomReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        lectureRoomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        lectureRoomReservationRequest.setSpecification(new ResourceSpecification(lectureRoomIdentifier));
        Reservation lectureRoomReservation = allocateAndCheck(lectureRoomReservationRequest);

        ReservationRequest request = new ReservationRequest();
        request.setSlot("2012-06-22T14:00", "PT2H");
        request.setSpecification(new ExistingEndpointSpecification(terminalIdentifier));
        request.setPurpose(ReservationRequestPurpose.SCIENCE);
        String identifier = getReservationService().createReservationRequest(SECURITY_TOKEN, request);
        runScheduler();
        checkAllocationFailed(identifier);

        request = (ReservationRequest) getReservationService().getReservationRequest(SECURITY_TOKEN, identifier);
        request.addProvidedReservationIdentifier(lectureRoomReservation.getIdentifier());

        Reservation reservation = allocateAndCheck(request);
        assertEquals(1, reservation.getChildReservationIdentifiers().size());
        Reservation childReservation = getReservationService().getReservation(SECURITY_TOKEN,
                reservation.getChildReservationIdentifiers().get(0));
        assertEquals(ExistingReservation.class, childReservation.getClass());
        ExistingReservation childExistingReservation = (ExistingReservation) childReservation;
        assertEquals(lectureRoomReservation.getIdentifier(), childExistingReservation.getReservation().getIdentifier());
    }

    @Test
    public void testAlias() throws Exception
    {
        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability(Technology.H323, AliasType.E164, "95000000[d]"));
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        aliasReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        aliasReservationRequest.setSpecification(new AliasSpecification(Technology.H323, AliasType.E164));
        Reservation aliasReservation = allocateAndCheck(aliasReservationRequest);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new AliasSpecification(Technology.H323));
        reservationRequest.addProvidedReservationIdentifier(aliasReservation.getIdentifier());

        Reservation reservation = allocateAndCheck(reservationRequest);
        assertEquals(ExistingReservation.class, reservation.getClass());
        ExistingReservation existingReservation = (ExistingReservation) reservation;
        assertEquals(aliasReservation.getIdentifier(), existingReservation.getReservation().getIdentifier());
    }

    @Test
    public void testAliasInCompartment() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.setAllocatable(true);
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new VirtualRoomsCapability(100));
        getResourceService().createResource(SECURITY_TOKEN, mcu);

        Resource aliasProvider = new Resource();
        aliasProvider.setName("aliasProvider");
        aliasProvider.setAllocatable(true);
        aliasProvider.addCapability(new AliasProviderCapability(Technology.H323, AliasType.E164, "950000001"));
        getResourceService().createResource(SECURITY_TOKEN, aliasProvider);

        ReservationRequest aliasReservationRequest = new ReservationRequest();
        aliasReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        aliasReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        aliasReservationRequest.setSpecification(new AliasSpecification(Technology.H323, AliasType.E164));
        String aliasReservationRequestIdentifier = allocate(aliasReservationRequest);
        AliasReservation aliasReservation = (AliasReservation) checkAllocated(aliasReservationRequestIdentifier);
        assertEquals(aliasReservation.getAlias().getValue(), "950000001");

        ReservationRequest compartmentReservationRequest = new ReservationRequest();
        compartmentReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        compartmentReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 3));
        compartmentReservationRequest.setSpecification(compartmentSpecification);
        compartmentReservationRequest.addProvidedReservationIdentifier(aliasReservation.getIdentifier());

        allocateAndCheck(compartmentReservationRequest);
        try {
            getReservationService().deleteReservationRequest(SECURITY_TOKEN, aliasReservationRequestIdentifier);
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
        String terminalIdentifier = getResourceService().createResource(SECURITY_TOKEN, terminal);

        ReservationRequest terminalReservationRequest = new ReservationRequest();
        terminalReservationRequest.setSlot("2012-06-22T00:00", "PT15H");
        terminalReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        terminalReservationRequest.setSpecification(new ExistingEndpointSpecification(terminalIdentifier));
        Reservation terminalReservation = allocateAndCheck(terminalReservationRequest);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSpecification(new ExistingEndpointSpecification(terminalIdentifier));
        reservationRequest.addProvidedReservationIdentifier(terminalReservation.getIdentifier());

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
        String terminalIdentifier = getResourceService().createResource(SECURITY_TOKEN, terminal);

        ReservationRequest terminalReservationRequest = new ReservationRequest();
        terminalReservationRequest.setSlot("2012-01-01T00:00", "P1Y");
        terminalReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        terminalReservationRequest.setSpecification(new ExistingEndpointSpecification(terminalIdentifier));
        Reservation terminalReservation = allocateAndCheck(terminalReservationRequest);

        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.addSlot(new DateTimeSlot("2012-06-22T14:00", "PT2H"));
        reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSet.addSpecification(new ExistingEndpointSpecification(terminalIdentifier));
        reservationRequestSet.addProvidedReservationIdentifier(terminalReservation.getIdentifier());

        Reservation reservation = allocateAndCheck(reservationRequestSet);
        assertEquals(ExistingReservation.class, reservation.getClass());
        ExistingReservation existingReservation = (ExistingReservation) reservation;
        assertEquals(terminalReservation.getIdentifier(), existingReservation.getReservation().getIdentifier());
    }

    @Test
    public void testProvidedVirtualRoomReservations() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.setAddress("127.0.0.1");
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(new VirtualRoomsCapability(10));
        mcu.setAllocatable(true);
        String mcuIdentifier = getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest virtualRoomReservationRequest = new ReservationRequest();
        virtualRoomReservationRequest.setSlot("2012-01-01T00:00", "P1D");
        virtualRoomReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        VirtualRoomSpecification virtualRoomSpecification = new VirtualRoomSpecification();
        virtualRoomSpecification.addTechnology(Technology.H323);
        virtualRoomSpecification.setPortCount(10);
        virtualRoomReservationRequest.setSpecification(virtualRoomSpecification);

        Reservation virtualRoomReservation = allocateAndCheck(virtualRoomReservationRequest);

        ReservationRequest compartmentReservationRequest = new ReservationRequest();
        compartmentReservationRequest.setSlot("2012-01-01T14:00", "PT2H");
        compartmentReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 3));
        compartmentReservationRequest.setSpecification(compartmentSpecification);
        compartmentReservationRequest.addProvidedReservationIdentifier(virtualRoomReservation.getIdentifier());

        allocateAndCheck(compartmentReservationRequest);
    }
}
