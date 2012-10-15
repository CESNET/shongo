package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.ReservationRequestType;
import cz.cesnet.shongo.controller.compartment.Compartment;
import cz.cesnet.shongo.controller.request.CompartmentSpecification;
import cz.cesnet.shongo.controller.request.ExistingEndpointSpecification;
import cz.cesnet.shongo.controller.request.ExternalEndpointSetSpecification;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.resource.*;
import org.junit.Test;

import javax.persistence.EntityManager;

/**
 * Tests for allocation of single virtual room in a {@link Compartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class VirtualRoomSingleTest extends AbstractTest
{
    @Test
    public void testSingleTechnology() throws Exception
    {
        Cache cache = new Cache();
        cache.setEntityManagerFactory(getEntityManagerFactory());
        cache.init();

        EntityManager entityManager = getEntityManager();

        DeviceResource terminal = new DeviceResource();
        terminal.addTechnology(Technology.H323);
        terminal.addCapability(new TerminalCapability());
        terminal.setAllocatable(true);
        cache.addResource(terminal, entityManager);

        DeviceResource mcu = new DeviceResource();
        mcu.setAddress(Address.LOCALHOST);
        mcu.addTechnology(Technology.H323);
        mcu.addCapability(new VirtualRoomsCapability(10));
        mcu.setAllocatable(true);
        cache.addResource(mcu, entityManager);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setType(ReservationRequestType.NORMAL);
        reservationRequest.setRequestedSlot("2012-06-22T14:00", "PT2H");
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addChildSpecification(new ExistingEndpointSpecification(terminal));
        compartmentSpecification.addChildSpecification(new ExternalEndpointSetSpecification(Technology.H323, 1));
        reservationRequest.setSpecification(compartmentSpecification);

        checkSuccessfulAllocation(reservationRequest, cache);
    }

    @Test
    public void testMultipleTechnology() throws Exception
    {
        Cache cache = new Cache();
        cache.setEntityManagerFactory(getEntityManagerFactory());
        cache.init();

        EntityManager entityManager = getEntityManager();

        DeviceResource terminal1 = new DeviceResource();
        terminal1.addTechnology(Technology.H323);
        terminal1.addCapability(new TerminalCapability());
        terminal1.setAllocatable(true);
        cache.addResource(terminal1, entityManager);

        DeviceResource terminal2 = new DeviceResource();
        terminal2.addTechnology(Technology.SIP);
        terminal2.addCapability(new TerminalCapability());
        terminal2.setAllocatable(true);
        cache.addResource(terminal2, entityManager);

        DeviceResource mcu = new DeviceResource();
        mcu.setAddress(Address.LOCALHOST);
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(new VirtualRoomsCapability(10));
        mcu.setAllocatable(true);
        cache.addResource(mcu, entityManager);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setType(ReservationRequestType.NORMAL);
        reservationRequest.setRequestedSlot("2012-06-22T14:00", "PT2H");
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addChildSpecification(new ExistingEndpointSpecification(terminal1));
        compartmentSpecification.addChildSpecification(new ExistingEndpointSpecification(terminal2));
        reservationRequest.setSpecification(compartmentSpecification);

        checkSuccessfulAllocation(reservationRequest, cache);
    }
}
