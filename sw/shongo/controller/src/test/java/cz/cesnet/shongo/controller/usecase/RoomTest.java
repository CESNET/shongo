package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for allocation of {@link cz.cesnet.shongo.controller.api.RoomReservation}
 * by {@link cz.cesnet.shongo.controller.api.RoomSpecification}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomTest extends AbstractControllerTest
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
        firstMcu.addCapability(new RoomProviderCapability(10));
        firstMcu.setAllocatable(true);
        String firstMcuId = getResourceService().createResource(SECURITY_TOKEN, firstMcu);

        DeviceResource secondMcu = new DeviceResource();
        secondMcu.setName("secondMcu");
        secondMcu.addTechnology(Technology.H323);
        secondMcu.addTechnology(Technology.SIP);
        secondMcu.addCapability(new RoomProviderCapability(10));
        secondMcu.setAllocatable(true);
        String secondMcuId = getResourceService().createResource(SECURITY_TOKEN, secondMcu);

        ReservationRequest firstReservationRequest = new ReservationRequest();
        firstReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        firstReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        RoomSpecification roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.addTechnology(Technology.SIP);
        roomSpecification.setParticipantCount(5);
        roomSpecification.setResourceId(secondMcuId);
        firstReservationRequest.setSpecification(roomSpecification);

        RoomReservation firstReservation = (RoomReservation) allocateAndCheck(firstReservationRequest);
        assertEquals("Virtual room should be allocated on second mcu, because it was specified as preferred",
                secondMcuId, firstReservation.getResourceId());

        ReservationRequest secondReservationRequest = new ReservationRequest();
        secondReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        secondReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.addTechnology(Technology.SIP);
        roomSpecification.setParticipantCount(3);
        roomSpecification.setResourceId(firstMcuId);
        secondReservationRequest.setSpecification(roomSpecification);

        RoomReservation secondReservation = (RoomReservation) allocateAndCheck(secondReservationRequest);
        assertEquals("Virtual room should be allocated on first mcu, because it was specified as preferred",
                firstMcuId, secondReservation.getResourceId());

        ReservationRequest thirdReservationRequest = new ReservationRequest();
        thirdReservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        thirdReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        roomSpecification = new RoomSpecification();
        roomSpecification.addTechnology(Technology.H323);
        roomSpecification.addTechnology(Technology.SIP);
        roomSpecification.setParticipantCount(5);
        thirdReservationRequest.setSpecification(roomSpecification);

        RoomReservation thirdReservation = (RoomReservation) allocateAndCheck(thirdReservationRequest);
        assertEquals("Virtual room should be allocated on second mcu, because it was the most filled",
                secondMcuId, thirdReservation.getResourceId());
    }
}
