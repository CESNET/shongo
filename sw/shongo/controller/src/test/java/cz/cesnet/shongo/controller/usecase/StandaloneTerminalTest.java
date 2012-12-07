package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import org.junit.Test;

/**
 * Tests for allocation of {@link cz.cesnet.shongo.controller.api.Executable.Compartment} without virtual room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class StandaloneTerminalTest extends AbstractControllerTest
{
    /**
     * Test for two standalone terminals with single technology.
     *
     * @throws Exception
     */
    @Test
    public void testSingleTechnology() throws Exception
    {
        DeviceResource firstTerminal = new DeviceResource();
        firstTerminal.setName("firstTerminal");
        firstTerminal.setAddress("127.0.0.1");
        firstTerminal.addTechnology(Technology.H323);
        firstTerminal.addCapability(new StandaloneTerminalCapability());
        firstTerminal.setAllocatable(true);
        String firstTerminalIdentifier = getResourceService().createResource(SECURITY_TOKEN, firstTerminal);

        DeviceResource secondTerminal = new DeviceResource();
        secondTerminal.setName("secondTerminal");
        secondTerminal.addTechnology(Technology.H323);
        secondTerminal.addCapability(new StandaloneTerminalCapability());
        secondTerminal.setAllocatable(true);
        String secondTerminalIdentifier = getResourceService().createResource(SECURITY_TOKEN, secondTerminal);

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
     * Test for two standalone terminals with multiple technology.
     *
     * @throws Exception
     */
    @Test
    public void testMultipleTechnology() throws Exception
    {
        DeviceResource firstTerminal = new DeviceResource();
        firstTerminal.setName("firstTerminal");
        firstTerminal.setAddress("127.0.0.1");
        firstTerminal.addTechnology(Technology.H323);
        firstTerminal.addTechnology(Technology.SIP);
        firstTerminal.addCapability(new StandaloneTerminalCapability());
        firstTerminal.setAllocatable(true);
        String firstTerminalIdentifier = getResourceService().createResource(SECURITY_TOKEN, firstTerminal);

        DeviceResource secondTerminal = new DeviceResource();
        secondTerminal.setName("secondTerminal");
        secondTerminal.addTechnology(Technology.H323);
        secondTerminal.addTechnology(Technology.ADOBE_CONNECT);
        secondTerminal.addCapability(new StandaloneTerminalCapability());
        secondTerminal.setAllocatable(true);
        String secondTerminalIdentifier = getResourceService().createResource(SECURITY_TOKEN, secondTerminal);

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
}
