package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestType;
import cz.cesnet.shongo.controller.api.*;
import org.junit.Test;

/**
 * Tests for allocation of multiple virtual rooms in a {@link Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class VirtualRoomMultipleTest extends AbstractControllerTest
{
    /**
     * Test multiple virtual room.
     *
     * @throws Exception
     */
    @Test
    public void test() throws Exception
    {
        if (true) {
            // TODO: Implement scheduling of multiple virtual rooms
            System.out.println("TODO: Implement scheduling of multiple virtual rooms.");
            return;
        }

        DeviceResource firstMcu = new DeviceResource();
        firstMcu.setName("firstMcu");
        firstMcu.addTechnology(Technology.H323);
        firstMcu.addCapability(new VirtualRoomsCapability(6));
        String firstMcuIdentifier = getResourceService().createResource(SECURITY_TOKEN, firstMcu);

        DeviceResource secondMcu = new DeviceResource();
        secondMcu.setName("secondMcu");
        secondMcu.addTechnology(Technology.H323);
        secondMcu.addCapability(new VirtualRoomsCapability(6));
        String secondMcuIdentifier = getResourceService().createResource(SECURITY_TOKEN, secondMcu);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setType(ReservationRequestType.NORMAL);
        reservationRequest.setSlot("2012-06-22T14:00", "PT2H");
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSetSpecification(Technology.H323, 10));
        reservationRequest.setSpecification(compartmentSpecification);

        String identifier = getReservationService().createReservationRequest(SECURITY_TOKEN, reservationRequest);
        runScheduler();
        checkAllocated(identifier);
    }
}
