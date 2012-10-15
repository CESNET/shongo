package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.ReservationRequestType;
import cz.cesnet.shongo.controller.compartment.Compartment;
import cz.cesnet.shongo.controller.request.*;
import cz.cesnet.shongo.controller.resource.*;
import org.junit.Test;

import javax.persistence.EntityManager;

/**
 * Tests for allocation of {@link Compartment} with no virtual room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class StandaloneTerminalTest extends AbstractTest
{
    @Test
    public void testSingleTechnology() throws Exception
    {
        Cache cache = new Cache();
        cache.setEntityManagerFactory(getEntityManagerFactory());
        cache.init();

        EntityManager entityManager = getEntityManager();

        DeviceResource terminal1 = new DeviceResource();
        terminal1.setAddress(Address.LOCALHOST);
        terminal1.addTechnology(Technology.H323);
        terminal1.addCapability(new StandaloneTerminalCapability());
        terminal1.setAllocatable(true);
        cache.addResource(terminal1, entityManager);

        DeviceResource terminal2 = new DeviceResource();
        terminal2.addTechnology(Technology.H323);
        terminal2.addCapability(new StandaloneTerminalCapability());
        terminal2.setAllocatable(true);
        cache.addResource(terminal2, entityManager);

        entityManager.close();

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setType(ReservationRequestType.NORMAL);
        reservationRequest.setRequestedSlot("2012-06-22T14:00", "PT2H");
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addChildSpecification(new ExistingEndpointSpecification(terminal1));
        compartmentSpecification.addChildSpecification(new ExistingEndpointSpecification(terminal2));
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
        terminal1.setAddress(Address.LOCALHOST);
        terminal1.addTechnology(Technology.H323);
        terminal1.addTechnology(Technology.SIP);
        terminal1.addCapability(new StandaloneTerminalCapability());
        terminal1.setAllocatable(true);
        cache.addResource(terminal1, entityManager);

        DeviceResource terminal2 = new DeviceResource();
        terminal2.addTechnology(Technology.H323);
        terminal2.addTechnology(Technology.ADOBE_CONNECT);
        terminal2.addCapability(new StandaloneTerminalCapability());
        terminal2.setAllocatable(true);
        cache.addResource(terminal2, entityManager);

        entityManager.close();

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
