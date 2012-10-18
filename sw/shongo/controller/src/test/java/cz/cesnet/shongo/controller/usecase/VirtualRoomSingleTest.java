package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestType;
import cz.cesnet.shongo.controller.api.*;
import org.junit.Test;

/**
 * Tests for allocation of single virtual room in a {@link Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class VirtualRoomSingleTest extends AbstractControllerTest
{
    /**
     * Test single technology virtual room.
     *
     * @throws Exception
     */
    @Test
    public void testSingleTechnology() throws Exception
    {
        DeviceResource terminal = new DeviceResource();
        terminal.setName("terminal");
        terminal.addTechnology(Technology.H323);
        terminal.addCapability(new TerminalCapability());
        terminal.setAllocatable(true);
        String terminalIdentifier = getResourceService().createResource(SECURITY_TOKEN, terminal);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.setAddress("127.0.0.1");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new VirtualRoomsCapability(10));
        mcu.setAllocatable(true);
        String mcuIdentifier = getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setType(ReservationRequestType.NORMAL);
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExistingEndpointSpecification(terminalIdentifier));
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 1));
        reservationRequest.setSpecification(compartmentSpecification);

        String identifier = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        runScheduler();
        checkAllocated(identifier);
    }

    /**
     * Test multiple technology virtual room
     *
     * @throws Exception
     */
    @Test
    public void testMultipleTechnology() throws Exception
    {
        DeviceResource firstTerminal = new DeviceResource();
        firstTerminal.setName("firstTerminal");
        firstTerminal.addTechnology(Technology.H323);
        firstTerminal.addCapability(new TerminalCapability());
        firstTerminal.setAllocatable(true);
        String firstTerminalIdentifier = getResourceService().createResource(SECURITY_TOKEN, firstTerminal);

        DeviceResource secondTerminal = new DeviceResource();
        secondTerminal.setName("secondTerminal");
        secondTerminal.addTechnology(Technology.SIP);
        secondTerminal.addCapability(new TerminalCapability());
        secondTerminal.setAllocatable(true);
        String secondTerminalIdentifier = getResourceService().createResource(SECURITY_TOKEN, secondTerminal);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.setAddress("127.0.0.1");
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(new VirtualRoomsCapability(10));
        mcu.setAllocatable(true);
        String mcuIdentifier = getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setType(ReservationRequestType.NORMAL);
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExistingEndpointSpecification(firstTerminalIdentifier));
        compartmentSpecification.addSpecification(new ExistingEndpointSpecification(secondTerminalIdentifier));
        reservationRequest.setSpecification(compartmentSpecification);

        String identifier = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        runScheduler();
        checkAllocated(identifier);
    }
}
