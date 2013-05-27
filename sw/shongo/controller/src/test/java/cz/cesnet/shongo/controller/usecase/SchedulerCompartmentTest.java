package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.authorization.Authorization;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Tests for allocation of a {@link cz.cesnet.shongo.controller.api.Executable.Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class SchedulerCompartmentTest extends AbstractControllerTest
{
    /**
     * Test for two standalone terminals with single technology.
     *
     * @throws Exception
     */
    @Test
    public void testNoRoomSingleTechnology() throws Exception
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
    public void testNoRoomMultipleTechnology() throws Exception
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

    /**
     * Test single technology virtual room.
     *
     * @throws Exception
     */
    @Test
    public void testRoomSingleTechnology() throws Exception
    {
        DeviceResource terminal = new DeviceResource();
        terminal.setName("terminal");
        terminal.addTechnology(Technology.H323);
        terminal.addCapability(new TerminalCapability());
        terminal.setAllocatable(true);
        String terminalId = getResourceService().createResource(SECURITY_TOKEN, terminal);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.addCapability(new AliasProviderCapability("95{digit:1}", AliasType.H323_E164).withRestrictedToResource());
        mcu.setAllocatable(true);
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest firstReservationRequest = new ReservationRequest();
        firstReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        firstReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExistingEndpointSpecification(terminalId));
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
    public void testRoomMultipleTechnology() throws Exception
    {
        DeviceResource firstTerminal = new DeviceResource();
        firstTerminal.setName("firstTerminal");
        firstTerminal.addTechnology(Technology.H323);
        firstTerminal.addCapability(new TerminalCapability());
        firstTerminal.setAllocatable(true);
        String firstTerminalId = getResourceService().createResource(SECURITY_TOKEN, firstTerminal);

        DeviceResource secondTerminal = new DeviceResource();
        secondTerminal.setName("secondTerminal");
        secondTerminal.addTechnology(Technology.SIP);
        secondTerminal.addCapability(new TerminalCapability());
        secondTerminal.setAllocatable(true);
        String secondTerminalId = getResourceService().createResource(SECURITY_TOKEN, secondTerminal);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.addCapability(new AliasProviderCapability("001", AliasType.H323_E164).withRestrictedToResource());
        mcu.addCapability(new AliasProviderCapability("001@cesnet.cz", AliasType.SIP_URI).withRestrictedToResource());
        mcu.setAllocatable(true);
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

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
     * Test multiple virtual room.
     *
     * @throws Exception
     */
    @Test
    public void testMultipleRooms() throws Exception
    {
        if (true) {
            // TODO: Implement scheduling of multiple virtual rooms
            System.out.println("TODO: Implement scheduling of multiple virtual rooms.");
            return;
        }

        DeviceResource firstMcu = new DeviceResource();
        firstMcu.setName("firstMcu");
        firstMcu.addTechnology(Technology.H323);
        firstMcu.addCapability(new RoomProviderCapability(6));
        String firstMcuId = getResourceService().createResource(SECURITY_TOKEN, firstMcu);

        DeviceResource secondMcu = new DeviceResource();
        secondMcu.setName("secondMcu");
        secondMcu.addTechnology(Technology.H323);
        secondMcu.addCapability(new RoomProviderCapability(6));
        String secondMcuId = getResourceService().createResource(SECURITY_TOKEN, secondMcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 10));
        reservationRequest.setSpecification(compartmentSpecification);

        String id = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        runScheduler();
        checkAllocated(id);
    }

    /**
     * Test disabling whole MCU by reservation request of the MCU resource directly (not though virtual room).
     *
     * @throws Exception
     */
    @Test
    public void testDisabledRoomProvider() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.addCapability(new AliasProviderCapability("950000001", AliasType.H323_E164).withRestrictedToResource());
        mcu.setAllocatable(true);
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest firstReservationRequest = new ReservationRequest();
        firstReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        firstReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        firstReservationRequest.setSpecification(new ResourceSpecification(mcuId));

        String firstReservationRequestId = allocate(firstReservationRequest);
        checkAllocated(firstReservationRequestId);

        ReservationRequest secondReservationRequest = new ReservationRequest();
        secondReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        secondReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 10));
        secondReservationRequest.setSpecification(compartmentSpecification);

        String secondReservationRequestId = allocate(secondReservationRequest);
        checkAllocationFailed(secondReservationRequestId);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, firstReservationRequestId);

        reallocate(secondReservationRequestId);
        checkAllocated(secondReservationRequestId);
    }

    /**
     * Test allocation failure for compartment.
     *
     * @throws Exception
     */
    @Test
    public void testFailure() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        getResourceService().createResource(SECURITY_TOKEN, mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 10));
        reservationRequest.setSpecification(compartmentSpecification);
        String reservationRequestId = allocate(reservationRequest);
        checkAllocationFailed(reservationRequestId);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, reservationRequestId);

        runScheduler();

        EntityManager entityManager = createEntityManager();
        List<cz.cesnet.shongo.controller.executor.Executable> executables = entityManager.createQuery(
                "SELECT executable FROM Executable executable",
                cz.cesnet.shongo.controller.executor.Executable.class).getResultList();
        Assert.assertEquals(0, executables.size());
        entityManager.close();
    }

    /**
     * Test multi-compartment.
     *
     * @throws Exception
     */
    @Test
    public void testMultiCompartment() throws Exception
    {
        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.setAllocatable(true);
        getResourceService().createResource(SECURITY_TOKEN, mcu);

        // Create reservation request
        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.setUserId(Authorization.ROOT_USER_ID);
        reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSet.addSlot("2012-06-01T15", "PT1H");
        reservationRequestSet.addSlot(new PeriodicDateTimeSlot("2012-07-01T14:00", "PT2H", "P1W", "2012-07-15"));
        MultiCompartmentSpecification multiCompartmentSpecification = new MultiCompartmentSpecification();
        reservationRequestSet.setSpecification(multiCompartmentSpecification);
        // First compartment
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSpecification(Technology.SIP));
        compartmentSpecification.addSpecification(new ExternalEndpointSpecification(Technology.H323));
        multiCompartmentSpecification.addSpecification(compartmentSpecification);
        // Second compartment
        compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.ADOBE_CONNECT, 2));
        multiCompartmentSpecification.addSpecification(compartmentSpecification);

        allocateAndCheck(reservationRequestSet);
    }
}
