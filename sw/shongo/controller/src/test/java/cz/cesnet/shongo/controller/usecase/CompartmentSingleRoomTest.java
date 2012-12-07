package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import org.junit.Test;

/**
 * Tests for allocation of single virtual room in a {@link cz.cesnet.shongo.controller.api.Executable.Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CompartmentSingleRoomTest extends AbstractControllerTest
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
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.addCapability(new AliasProviderCapability(AliasType.H323_E164, "95000000[d]", true));
        mcu.setAllocatable(true);
        String mcuIdentifier = getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest firstReservationRequest = new ReservationRequest();
        firstReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        firstReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExistingEndpointSpecification(terminalIdentifier));
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 1));
        firstReservationRequest.setSpecification(compartmentSpecification);

        allocateAndCheck(firstReservationRequest);

        ReservationRequest secondReservationRequest = new ReservationRequest();
        secondReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        secondReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 8));
        secondReservationRequest.setSpecification(compartmentSpecification);

        allocateAndCheck(secondReservationRequest);

        ReservationRequest thirddReservationRequest = new ReservationRequest();
        thirddReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        thirddReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 2));
        thirddReservationRequest.setSpecification(compartmentSpecification);

        allocateAndCheckFailed(thirddReservationRequest);
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
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.addCapability(new AliasProviderCapability(AliasType.H323_E164, "950000001", true));
        mcu.addCapability(new AliasProviderCapability(AliasType.SIP_URI, "950000001@cesnet.cz", true));
        mcu.setAllocatable(true);
        String mcuIdentifier = getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
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

    /**
     * Test disabling whole MCU by reservation request of the MCU resource directly (not though virtual room).
     *
     * @throws Exception
     */
    @Test
    public void testDisabledReservation() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.addCapability(new AliasProviderCapability(AliasType.H323_E164, "950000001", true));
        mcu.setAllocatable(true);
        String mcuIdentifier = getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest firstReservationRequest = new ReservationRequest();
        firstReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        firstReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        firstReservationRequest.setSpecification(new ResourceSpecification(mcuIdentifier));

        String firstReservationRequestIdentifier = allocate(firstReservationRequest);
        checkAllocated(firstReservationRequestIdentifier);

        ReservationRequest secondReservationRequest = new ReservationRequest();
        secondReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        secondReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 10));
        secondReservationRequest.setSpecification(compartmentSpecification);

        String secondReservationRequestIdentifier = allocate(secondReservationRequest);
        checkAllocationFailed(secondReservationRequestIdentifier);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, firstReservationRequestIdentifier);

        reallocate(secondReservationRequestIdentifier);
        checkAllocated(secondReservationRequestIdentifier);
    }
}
