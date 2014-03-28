package cz.cesnet.shongo.controller.booking.compartment;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.CompartmentSpecification;
import cz.cesnet.shongo.controller.api.MultiCompartmentSpecification;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Tests for allocation of a {@link cz.cesnet.shongo.controller.api.CompartmentExecutable}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CompartmentTest extends AbstractControllerTest
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
        String firstTerminalId = createResource(firstTerminal);

        DeviceResource secondTerminal = new DeviceResource();
        secondTerminal.setName("secondTerminal");
        secondTerminal.addTechnology(Technology.H323);
        secondTerminal.addCapability(new StandaloneTerminalCapability());
        secondTerminal.setAllocatable(true);
        String secondTerminalId = createResource(secondTerminal);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        cz.cesnet.shongo.controller.api.CompartmentSpecification compartmentSpecification = new cz.cesnet.shongo.controller.api.CompartmentSpecification();
        compartmentSpecification.addParticipant(new ExistingEndpointParticipant(firstTerminalId));
        compartmentSpecification.addParticipant(new ExistingEndpointParticipant(secondTerminalId));
        reservationRequest.setSpecification(compartmentSpecification);

        allocateAndCheck(reservationRequest);

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
        String firstTerminalId = createResource(firstTerminal);

        DeviceResource secondTerminal = new DeviceResource();
        secondTerminal.setName("secondTerminal");
        secondTerminal.addTechnology(Technology.H323);
        secondTerminal.addTechnology(Technology.ADOBE_CONNECT);
        secondTerminal.addCapability(new StandaloneTerminalCapability());
        secondTerminal.setAllocatable(true);
        String secondTerminalId = createResource(secondTerminal);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        cz.cesnet.shongo.controller.api.CompartmentSpecification compartmentSpecification = new cz.cesnet.shongo.controller.api.CompartmentSpecification();
        compartmentSpecification.addParticipant(new ExistingEndpointParticipant(firstTerminalId));
        compartmentSpecification.addParticipant(new ExistingEndpointParticipant(secondTerminalId));
        reservationRequest.setSpecification(compartmentSpecification);

        allocateAndCheck(reservationRequest);
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
        String terminalId = createResource(terminal);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.addCapability(new AliasProviderCapability("95{digit:1}", AliasType.H323_E164).withRestrictedToResource());
        mcu.setAllocatable(true);
        String mcuId = createResource(mcu);

        ReservationRequest firstReservationRequest = new ReservationRequest();
        firstReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        firstReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        cz.cesnet.shongo.controller.api.CompartmentSpecification compartmentSpecification = new cz.cesnet.shongo.controller.api.CompartmentSpecification();
        compartmentSpecification.addParticipant(new ExistingEndpointParticipant(terminalId));
        compartmentSpecification.addParticipant(new ExternalEndpointSetParticipant(Technology.H323, 1));
        firstReservationRequest.setSpecification(compartmentSpecification);

        allocateAndCheck(firstReservationRequest);

        ReservationRequest secondReservationRequest = new ReservationRequest();
        secondReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        secondReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        compartmentSpecification = new cz.cesnet.shongo.controller.api.CompartmentSpecification();
        compartmentSpecification.addParticipant(new ExternalEndpointSetParticipant(Technology.H323, 8));
        secondReservationRequest.setSpecification(compartmentSpecification);

        allocateAndCheck(secondReservationRequest);

        ReservationRequest thirddReservationRequest = new ReservationRequest();
        thirddReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        thirddReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        compartmentSpecification = new cz.cesnet.shongo.controller.api.CompartmentSpecification();
        compartmentSpecification.addParticipant(new ExternalEndpointSetParticipant(Technology.H323, 2));
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
        String firstTerminalId = createResource(firstTerminal);

        DeviceResource secondTerminal = new DeviceResource();
        secondTerminal.setName("secondTerminal");
        secondTerminal.addTechnology(Technology.SIP);
        secondTerminal.addCapability(new TerminalCapability());
        secondTerminal.setAllocatable(true);
        String secondTerminalId = createResource(secondTerminal);

        DeviceResource mcu = new DeviceResource();
        mcu.setName("mcu");
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(new RoomProviderCapability(10));
        mcu.addCapability(new AliasProviderCapability("001", AliasType.H323_E164).withRestrictedToResource());
        mcu.addCapability(new AliasProviderCapability("001@cesnet.cz", AliasType.SIP_URI).withRestrictedToResource());
        mcu.setAllocatable(true);
        String mcuId = createResource(mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        cz.cesnet.shongo.controller.api.CompartmentSpecification compartmentSpecification = new cz.cesnet.shongo.controller.api.CompartmentSpecification();
        compartmentSpecification.addParticipant(new ExistingEndpointParticipant(firstTerminalId));
        compartmentSpecification.addParticipant(new ExistingEndpointParticipant(secondTerminalId));
        reservationRequest.setSpecification(compartmentSpecification);

        allocateAndCheck(reservationRequest);
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
        String firstMcuId = createResource(firstMcu);

        DeviceResource secondMcu = new DeviceResource();
        secondMcu.setName("secondMcu");
        secondMcu.addTechnology(Technology.H323);
        secondMcu.addCapability(new RoomProviderCapability(6));
        String secondMcuId = createResource(secondMcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        cz.cesnet.shongo.controller.api.CompartmentSpecification compartmentSpecification = new cz.cesnet.shongo.controller.api.CompartmentSpecification();
        compartmentSpecification.addParticipant(new ExternalEndpointSetParticipant(Technology.H323, 10));
        reservationRequest.setSpecification(compartmentSpecification);

        allocateAndCheck(reservationRequest);
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
        String mcuId = createResource(mcu);

        ReservationRequest firstReservationRequest = new ReservationRequest();
        firstReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        firstReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        firstReservationRequest.setSpecification(new ResourceSpecification(mcuId));

        String firstReservationRequestId = allocate(firstReservationRequest);
        checkAllocated(firstReservationRequestId);

        ReservationRequest secondReservationRequest = new ReservationRequest();
        secondReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        secondReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        cz.cesnet.shongo.controller.api.CompartmentSpecification compartmentSpecification = new cz.cesnet.shongo.controller.api.CompartmentSpecification();
        compartmentSpecification.addParticipant(new ExternalEndpointSetParticipant(Technology.H323, 10));
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
        createResource(mcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        cz.cesnet.shongo.controller.api.CompartmentSpecification compartmentSpecification = new cz.cesnet.shongo.controller.api.CompartmentSpecification();
        compartmentSpecification.addParticipant(new ExternalEndpointSetParticipant(Technology.H323, 10));
        reservationRequest.setSpecification(compartmentSpecification);
        String reservationRequestId = allocate(reservationRequest);
        checkAllocationFailed(reservationRequestId);

        getReservationService().deleteReservationRequest(SECURITY_TOKEN, reservationRequestId);

        runScheduler();

        // Verify that all executables created for scheduler reports has been deleted
        EntityManager entityManager = createEntityManager();
        List<cz.cesnet.shongo.controller.booking.executable.Executable> executables = entityManager.createQuery(
                "SELECT executable FROM Executable executable",
                Executable.class).getResultList();
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
        mcu.addTechnology(Technology.SIP);
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new RoomProviderCapability(10));
        AliasProviderCapability aliasProviderCapability = new AliasProviderCapability("{digit:1}");
        aliasProviderCapability.addAlias(new Alias(AliasType.H323_E164, "{value}"));
        aliasProviderCapability.addAlias(new Alias(AliasType.SIP_URI, "{value}@cesnet.cz"));
        mcu.addCapability(aliasProviderCapability);
        mcu.setAllocatable(true);
        createResource(mcu);

        DeviceResource connect = new DeviceResource();
        connect.setName("connect");
        connect.addTechnology(Technology.ADOBE_CONNECT);
        connect.addCapability(new RoomProviderCapability(10));
        connect.addCapability(new AliasProviderCapability("{hash}", AliasType.ADOBE_CONNECT_URI));
        connect.setAllocatable(true);
        createResource(connect);

        // Create reservation request
        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.setUserId(Authorization.ROOT_USER_ID);
        reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSet.addSlot("2012-06-01T15", "PT1H");
        reservationRequestSet.addSlot(new PeriodicDateTimeSlot("2012-07-01T14:00", "PT2H", "P1W", "2012-07-15"));
        cz.cesnet.shongo.controller.api.MultiCompartmentSpecification multiCompartmentSpecification = new MultiCompartmentSpecification();
        reservationRequestSet.setSpecification(multiCompartmentSpecification);
        // First compartment
        cz.cesnet.shongo.controller.api.CompartmentSpecification compartmentSpecification = new cz.cesnet.shongo.controller.api.CompartmentSpecification();
        compartmentSpecification.addParticipant(new ExternalEndpointParticipant(Technology.SIP));
        compartmentSpecification.addParticipant(new ExternalEndpointParticipant(Technology.H323));
        multiCompartmentSpecification.addSpecification(compartmentSpecification);
        // Second compartment
        compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addParticipant(new ExternalEndpointSetParticipant(Technology.ADOBE_CONNECT, 2));
        multiCompartmentSpecification.addSpecification(compartmentSpecification);

        allocateAndCheck(reservationRequestSet);
    }
}
