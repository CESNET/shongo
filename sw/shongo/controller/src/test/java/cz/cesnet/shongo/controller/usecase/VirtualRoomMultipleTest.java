package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.ReservationRequestType;
import cz.cesnet.shongo.controller.compartment.Compartment;
import cz.cesnet.shongo.controller.request.CompartmentSpecification;
import cz.cesnet.shongo.controller.request.ExistingEndpointSpecification;
import cz.cesnet.shongo.controller.request.ExternalEndpointSetSpecification;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.resource.Address;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.TerminalCapability;
import cz.cesnet.shongo.controller.resource.VirtualRoomsCapability;
import org.junit.Test;

import javax.persistence.EntityManager;

/**
 * Tests for allocation of multiple virtual room in a {@link Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class VirtualRoomMultipleTest extends AbstractTest
{
    @Test
    public void test() throws Exception
    {
        if (true) {
            // TODO: Implement scheduling of multiple virtual rooms
            System.out.println("TODO: Implement scheduling of multiple virtual rooms.");
            return;
        }

        Cache cache = new Cache();
        cache.setEntityManagerFactory(getEntityManagerFactory());
        cache.init();

        EntityManager entityManager = getEntityManager();

        DeviceResource mcu1 = new DeviceResource();
        mcu1.addTechnology(Technology.H323);
        mcu1.addCapability(new VirtualRoomsCapability(6));
        cache.addResource(mcu1, entityManager);

        DeviceResource mcu2 = new DeviceResource();
        mcu2.addTechnology(Technology.H323);
        mcu2.addCapability(new VirtualRoomsCapability(6));
        cache.addResource(mcu2, entityManager);

        entityManager.close();

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setType(ReservationRequestType.NORMAL);
        reservationRequest.setRequestedSlot("2012-06-22T14:00", "PT2H");
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addChildSpecification(new ExternalEndpointSetSpecification(Technology.H323, 10));
        reservationRequest.setSpecification(compartmentSpecification);

        checkSuccessfulAllocation(reservationRequest, cache, entityManager);
    }
}
