package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractControllerTest;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.*;
import org.junit.Test;

/**
 * Tests for allocation of multiple virtual rooms in a {@link cz.cesnet.shongo.controller.api.Executable.Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CompartmentMultipleRoomTest extends AbstractControllerTest
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
}
