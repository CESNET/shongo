package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.util.DatabaseHelper;
import junitx.framework.Assert;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.List;

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
    public void testMultipleTechnology() throws Exception
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
        String mcuId = getResourceService().createResource(SECURITY_TOKEN, mcu);

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

        EntityManager entityManager = getEntityManager();
        List<cz.cesnet.shongo.controller.executor.Executable> executables = entityManager.createQuery(
                "SELECT executable FROM Executable executable",
                cz.cesnet.shongo.controller.executor.Executable.class).getResultList();
        Assert.assertEquals(0, executables.size());
        entityManager.close();
    }
}
