package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for allocation of {@link VirtualRoomReservation} by {@link VirtualRoomSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class VirtualRoomTest extends AbstractControllerTest
{
    /**
     * Test allocation of virtual room.
     *
     * @throws Exception
     */
    @Test
    public void test() throws Exception
    {
        DeviceResource firstMcu = new DeviceResource();
        firstMcu.setName("firstMcu");
        firstMcu.addTechnology(Technology.H323);
        firstMcu.addTechnology(Technology.SIP);
        firstMcu.addCapability(new VirtualRoomsCapability(10));
        firstMcu.setAllocatable(true);
        String firstMcuIdentifier = getResourceService().createResource(SECURITY_TOKEN, firstMcu);

        DeviceResource secondMcu = new DeviceResource();
        secondMcu.setName("secondMcu");
        secondMcu.addTechnology(Technology.H323);
        secondMcu.addTechnology(Technology.SIP);
        secondMcu.addCapability(new VirtualRoomsCapability(10));
        secondMcu.setAllocatable(true);
        String secondMcuIdentifier = getResourceService().createResource(SECURITY_TOKEN, secondMcu);

        ReservationRequest firstReservationRequest = new ReservationRequest();
        firstReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        firstReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        VirtualRoomSpecification virtualRoomSpecification = new VirtualRoomSpecification();
        virtualRoomSpecification.addTechnology(Technology.H323);
        virtualRoomSpecification.addTechnology(Technology.SIP);
        virtualRoomSpecification.setPortCount(5);
        virtualRoomSpecification.setResourceIdentifier(secondMcuIdentifier);
        firstReservationRequest.setSpecification(virtualRoomSpecification);

        VirtualRoomReservation firstReservation = (VirtualRoomReservation) allocateAndCheck(firstReservationRequest);
        assertEquals("Virtual room should be allocated on second mcu, because it was specified as preferred",
                secondMcuIdentifier, firstReservation.getResourceIdentifier());

        ReservationRequest secondReservationRequest = new ReservationRequest();
        secondReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        secondReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        virtualRoomSpecification = new VirtualRoomSpecification();
        virtualRoomSpecification.addTechnology(Technology.H323);
        virtualRoomSpecification.addTechnology(Technology.SIP);
        virtualRoomSpecification.setPortCount(3);
        virtualRoomSpecification.setResourceIdentifier(firstMcuIdentifier);
        secondReservationRequest.setSpecification(virtualRoomSpecification);

        VirtualRoomReservation secondReservation = (VirtualRoomReservation) allocateAndCheck(secondReservationRequest);
        assertEquals("Virtual room should be allocated on first mcu, because it was specified as preferred",
                firstMcuIdentifier, secondReservation.getResourceIdentifier());

        ReservationRequest thirdReservationRequest = new ReservationRequest();
        thirdReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        thirdReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        virtualRoomSpecification = new VirtualRoomSpecification();
        virtualRoomSpecification.addTechnology(Technology.H323);
        virtualRoomSpecification.addTechnology(Technology.SIP);
        virtualRoomSpecification.setPortCount(5);
        thirdReservationRequest.setSpecification(virtualRoomSpecification);

        VirtualRoomReservation thirdReservation = (VirtualRoomReservation) allocateAndCheck(thirdReservationRequest);
        assertEquals("Virtual room should be allocated on second mcu, because it was the most filled",
                secondMcuIdentifier, thirdReservation.getResourceIdentifier());
    }
}
