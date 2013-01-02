package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
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
        firstTerminal.addTechnology(Technology.H323);
        StandaloneTerminalCapability terminalCapability = new StandaloneTerminalCapability();
        terminalCapability.addAlias(new Alias(AliasType.H323_E164, "950000001"));
        firstTerminal.addCapability(terminalCapability);
        firstTerminal.setAllocatable(true);
        String firstTerminalId = getResourceService().createResource(SECURITY_TOKEN, firstTerminal);

        DeviceResource secondTerminal = new DeviceResource();
        secondTerminal.setName("secondTerminal");
        secondTerminal.addTechnology(Technology.H323);
        secondTerminal.addCapability(new StandaloneTerminalCapability());
        secondTerminal.setAllocatable(true);
        String secondTerminalId = getResourceService().createResource(SECURITY_TOKEN, secondTerminal);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExistingEndpointSpecification(firstTerminalId));
        compartmentSpecification.addSpecification(new ExistingEndpointSpecification(secondTerminalId));
        reservationRequest.setSpecification(compartmentSpecification);

        String id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        runScheduler();
        checkAllocated(id);
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
        firstTerminal.addTechnology(Technology.H323);
        firstTerminal.addTechnology(Technology.SIP);
        StandaloneTerminalCapability terminalCapability = new StandaloneTerminalCapability();
        terminalCapability.addAlias(new Alias(AliasType.H323_E164, "950000001"));
        firstTerminal.addCapability(terminalCapability);
        firstTerminal.setAllocatable(true);
        String firstTerminalId = getResourceService().createResource(SECURITY_TOKEN, firstTerminal);

        DeviceResource secondTerminal = new DeviceResource();
        secondTerminal.setName("secondTerminal");
        secondTerminal.addTechnology(Technology.H323);
        secondTerminal.addTechnology(Technology.ADOBE_CONNECT);
        secondTerminal.addCapability(new StandaloneTerminalCapability());
        secondTerminal.setAllocatable(true);
        String secondTerminalId = getResourceService().createResource(SECURITY_TOKEN, secondTerminal);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExistingEndpointSpecification(firstTerminalId));
        compartmentSpecification.addSpecification(new ExistingEndpointSpecification(secondTerminalId));
        reservationRequest.setSpecification(compartmentSpecification);

        String id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        runScheduler();
        checkAllocated(id);
    }
}
