package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.allocation.AllocatedCompartment;
import cz.cesnet.shongo.controller.allocation.AllocatedCompartmentManager;
import cz.cesnet.shongo.controller.common.AbsoluteDateTimeSpecification;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.resource.*;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.List;

import static junit.framework.Assert.*;

/**
 * Test for processing {@link ReservationRequest} by controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestTest extends AbstractDatabaseTest
{
    @Test
    public void test() throws Exception
    {
        // Cache
        Cache cache = null;
        // Interval for which a preprocessor and a scheduler runs and
        // in which the reservation request compartment takes place
        Interval interval = Interval.parse("2012-06-01/2012-06-30T23:59:59");
        // Identifiers for persons which are requested to participate in the compartment
        Long personId1 = null;
        Long personId2 = null;
        // Identifier for reservation request which is created
        Long reservationRequestId = null;
        // Identifier for compartment request which is created from reservation request
        Long compartmentRequestId = null;

        // ------------
        // Setup cache
        // ------------
        {
            cache = new Cache();
            cache.setEntityManagerFactory(getEntityManagerFactory());
            cache.init();

            DeviceResource deviceResource = new DeviceResource();
            deviceResource.setName("MCU");
            deviceResource.setAddress(Address.LOCALHOST);
            deviceResource.addTechnology(Technology.H323);
            deviceResource.addCapability(new VirtualRoomsCapability(100));
            deviceResource.setAllocatable(true);
            cache.addResource(deviceResource, getEntityManager());
        }

        // ---------------------------
        // Create reservation request
        // ---------------------------
        {
            EntityManager entityManager = getEntityManager();
            entityManager.getTransaction().begin();

            ReservationRequest reservationRequest = new ReservationRequest();
            reservationRequest.setType(ReservationRequestType.NORMAL);
            reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
            reservationRequest.addRequestedSlot(new AbsoluteDateTimeSpecification("2012-06-22T14:00"),
                    new Period("PT2H"));
            Compartment compartment = reservationRequest.addRequestedCompartment();
            // Requests 3 guests
            compartment.addRequestedResource(new ExternalEndpointSpecification(Technology.H323, 3));
            // Request specific persons, the first will use specified H.323 endpoint and
            // the second must select an endpoint then
            Person person1 = new Person("Martin Srom", "srom@cesnet.cz");
            Person person2 = new Person("Ondrej Bouda", "bouda@cesnet.cz");
            compartment.addRequestedPerson(person1,
                    new ExternalEndpointSpecification(Technology.H323, new Alias(AliasType.E164, "950080085")));
            compartment.addRequestedPerson(person2);

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            reservationRequestManager.create(reservationRequest);

            personId1 = person1.getId();
            assertNotNull("The person should have assigned identifier", personId1);
            personId2 = person2.getId();
            assertNotNull("The person should have assigned identifier", personId2);
            reservationRequestId = reservationRequest.getId();
            assertNotNull("The reservation request should have assigned identifier", reservationRequestId);

            entityManager.getTransaction().commit();
            entityManager.close();
        }

        // ----------------------------------------------------------
        // Create compartment request(s) from reservation request(s)
        // ----------------------------------------------------------
        {
            EntityManager entityManager = getEntityManager();

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            assertNotNull("The reservation request should be stored in database",
                    reservationRequestManager.get(reservationRequestId));

            Preprocessor.createAndRun(interval, entityManager);

            CompartmentRequestManager compartmentRequestManager = new CompartmentRequestManager(entityManager);
            List<CompartmentRequest> compartmentRequestList =
                    compartmentRequestManager.listByReservationRequest(reservationRequestId);
            assertEquals("One compartment request should be created for the reservation request", 1,
                    compartmentRequestList.size());
            assertEquals("No complete compartment requests should be present", 0,
                    compartmentRequestManager.listCompleted(interval).size());

            compartmentRequestId = compartmentRequestList.get(0).getId();
            assertNotNull("The compartment request should have assigned identifier", compartmentRequestId);

            entityManager.close();
        }

        // ------------------------------------------
        // Persons accepts or rejects the invitation
        // ------------------------------------------
        {
            EntityManager entityManager = getEntityManager();
            entityManager.getTransaction().begin();

            CompartmentRequestManager compartmentRequestManager = new CompartmentRequestManager(entityManager);

            // First person rejects
            compartmentRequestManager.rejectPersonRequest(compartmentRequestId, personId1);
            assertEquals("No complete compartment requests should be present", 0,
                    compartmentRequestManager.listCompleted(interval).size());

            // Second person accepts and fails because he must select an endpoint first
            try {
                compartmentRequestManager.acceptPersonRequest(compartmentRequestId, personId2);
                fail("Person shouldn't accept the invitation because he should have selected an endpoint first!");
            }
            catch (IllegalStateException exception) {
            }

            // Second person accepts
            compartmentRequestManager.selectResourceForPersonRequest(compartmentRequestId, personId2,
                    new ExternalEndpointSpecification(Technology.H323, new Alias(AliasType.E164, "950080086")));
            compartmentRequestManager.acceptPersonRequest(compartmentRequestId, personId2);
            assertEquals("One complete compartment request should be present", 1,
                    compartmentRequestManager.listCompleted(interval).size());

            entityManager.getTransaction().commit();
            entityManager.close();
        }

        // -----------------------------------------
        // Schedule complete compartment request(s)
        // -----------------------------------------
        {
            EntityManager entityManager = getEntityManager();

            Scheduler.createAndRun(interval, entityManager, cache);

            CompartmentRequestManager compartmentRequestManager = new CompartmentRequestManager(entityManager);
            AllocatedCompartmentManager allocatedCompartmentManager = new AllocatedCompartmentManager(entityManager);

            CompartmentRequest compartmentRequest = compartmentRequestManager.get(compartmentRequestId);
            assertEquals("Compartment request should be in ALLOCATED state.",
                    CompartmentRequest.State.ALLOCATED, compartmentRequest.getState());

            AllocatedCompartment allocatedCompartment =
                    allocatedCompartmentManager.getByCompartmentRequest(compartmentRequestId);
            assertNotNull("Allocated compartment should be created for the compartment request", allocatedCompartment);

            entityManager.close();
        }

        // ---------------------------
        // Modify compartment request
        // ---------------------------
        {
            EntityManager entityManager = getEntityManager();

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            CompartmentRequestManager compartmentRequestManager = new CompartmentRequestManager(entityManager);
            AllocatedCompartmentManager allocatedCompartmentManager = new AllocatedCompartmentManager(entityManager);

            // Add new requested resource to exceed the maximum number of ports
            entityManager.getTransaction().begin();
            ReservationRequest reservationRequest = reservationRequestManager.get(reservationRequestId);
            Compartment compartment = reservationRequest.getRequestedCompartments().get(0);
            compartment.addRequestedResource(new ExternalEndpointSpecification(Technology.H323, 100));
            reservationRequestManager.update(reservationRequest);
            entityManager.getTransaction().commit();

            // Pre-process and schedule compartment request
            Preprocessor.createAndRun(interval, entityManager);
            Scheduler.createAndRun(interval, entityManager, cache);

            // Checks allocation failed
            CompartmentRequest compartmentRequest = compartmentRequestManager.get(compartmentRequestId);
            assertEquals("Compartment request should be in ALLOCATION_FAILED state.",
                    CompartmentRequest.State.ALLOCATION_FAILED, compartmentRequest.getState());
            AllocatedCompartment allocatedCompartment =
                    allocatedCompartmentManager.getByCompartmentRequest(compartmentRequestId);
            assertNull("Allocated compartment should not be created for the compartment request",
                    allocatedCompartment);

            // Modify requested resources to not exceed the maximum number of ports
            entityManager.getTransaction().begin();
            ExternalEndpointSpecification externalEndpointSpecification =
                    (ExternalEndpointSpecification) compartment.getRequestedResources().get(2);
            externalEndpointSpecification.setCount(96);
            reservationRequestManager.update(reservationRequest);
            entityManager.getTransaction().commit();

            // Pre-process and schedule compartment request
            Preprocessor.createAndRun(interval, entityManager);
            Scheduler.createAndRun(interval, entityManager, cache);

            // Checks allocated
            entityManager.refresh(compartmentRequest);
            assertEquals("Compartment request should be in ALLOCATED state.",
                    CompartmentRequest.State.ALLOCATED, compartmentRequest.getState());
        }

        // ---------------------------
        // Delete reservation request
        // ---------------------------
        {
            EntityManager entityManager = getEntityManager();

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

            // Delete reservation request
            entityManager.getTransaction().begin();
            ReservationRequest reservationRequest = reservationRequestManager.get(reservationRequestId);
            reservationRequestManager.delete(reservationRequest);
            entityManager.getTransaction().commit();

            // Pre-process and schedule
            Preprocessor.createAndRun(interval, entityManager);
            Scheduler.createAndRun(interval, entityManager, cache);
        }

        // ------------------------
        // Clean-up
        // ------------------------
        {
            cache.destroy();
        }
    }

    /**
     * Create reservation request in the database, allocate it and check if allocation succeeds
     *
     * @param reservationRequest
     * @param cache
     * @throws Exception
     */
    private void checkSuccessfulAllocation(ReservationRequest reservationRequest, Cache cache)
            throws Exception
    {
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(getEntityManager());
        reservationRequestManager.create(reservationRequest);

        Interval interval = Interval.parse("0/9999");
        Preprocessor.createAndRun(interval, getEntityManager());
        Scheduler.createAndRun(interval, getEntityManager(), cache);

        CompartmentRequestManager compartmentRequestManager = new CompartmentRequestManager(getEntityManager());
        List<CompartmentRequest> compartmentRequests =
                compartmentRequestManager.listByReservationRequest(reservationRequest);
        assertEquals(1, compartmentRequests.size());
        CompartmentRequest compartmentRequest = compartmentRequests.get(0);

        AllocatedCompartmentManager allocatedCompartmentManager = new AllocatedCompartmentManager(getEntityManager());
        List<AllocatedCompartment> allocatedCompartments = allocatedCompartmentManager.listByReservationRequest(reservationRequest);
        if (allocatedCompartments.size() == 0) {
            System.err.println(compartmentRequest.getStateDescription());
            Thread.sleep(100);
        }
        assertEquals(1, allocatedCompartments.size());
    }

    @Test
    public void testStandaloneTerminals() throws Exception
    {
        Cache cache = new Cache();
        cache.setEntityManagerFactory(getEntityManagerFactory());
        cache.init();

        DeviceResource terminal1 = new DeviceResource();
        terminal1.setAddress(Address.LOCALHOST);
        terminal1.addTechnology(Technology.H323);
        terminal1.addCapability(new StandaloneTerminalCapability());
        terminal1.setAllocatable(true);
        cache.addResource(terminal1, getEntityManager());

        DeviceResource terminal2 = new DeviceResource();
        terminal2.addTechnology(Technology.H323);
        terminal2.addCapability(new StandaloneTerminalCapability());
        terminal2.setAllocatable(true);
        cache.addResource(terminal2, getEntityManager());

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setType(ReservationRequestType.NORMAL);
        reservationRequest.addRequestedSlot(new AbsoluteDateTimeSpecification("2012-06-22T14:00"), new Period("PT2H"));
        Compartment compartment = reservationRequest.addRequestedCompartment();
        compartment.addRequestedResource(new ExistingResourceSpecification(terminal1));
        compartment.addRequestedResource(new ExistingResourceSpecification(terminal2));

        checkSuccessfulAllocation(reservationRequest, cache);
    }

    @Test
    public void testMultipleTechnologyTerminals() throws Exception
    {
        Cache cache = new Cache();
        cache.setEntityManagerFactory(getEntityManagerFactory());
        cache.init();

        DeviceResource terminal1 = new DeviceResource();
        terminal1.setAddress(Address.LOCALHOST);
        terminal1.addTechnology(Technology.H323);
        terminal1.addTechnology(Technology.SIP);
        terminal1.addCapability(new StandaloneTerminalCapability());
        terminal1.setAllocatable(true);
        cache.addResource(terminal1, getEntityManager());

        DeviceResource terminal2 = new DeviceResource();
        terminal2.addTechnology(Technology.H323);
        terminal2.addTechnology(Technology.ADOBE_CONNECT);
        terminal2.addCapability(new StandaloneTerminalCapability());
        terminal2.setAllocatable(true);
        cache.addResource(terminal2, getEntityManager());

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setType(ReservationRequestType.NORMAL);
        reservationRequest.addRequestedSlot(new AbsoluteDateTimeSpecification("2012-06-22T14:00"), new Period("PT2H"));
        Compartment compartment = reservationRequest.addRequestedCompartment();
        compartment.addRequestedResource(new ExistingResourceSpecification(terminal1));
        compartment.addRequestedResource(new ExistingResourceSpecification(terminal2));

        checkSuccessfulAllocation(reservationRequest, cache);
    }

    @Test
    public void testMultipleTechnologyVirtualRoom() throws Exception
    {
        Cache cache = new Cache();
        cache.setEntityManagerFactory(getEntityManagerFactory());
        cache.init();

        DeviceResource terminal1 = new DeviceResource();
        terminal1.addTechnology(Technology.H323);
        terminal1.addCapability(new TerminalCapability());
        terminal1.setAllocatable(true);
        cache.addResource(terminal1, getEntityManager());

        DeviceResource terminal2 = new DeviceResource();
        terminal2.addTechnology(Technology.SIP);
        terminal2.addCapability(new TerminalCapability());
        terminal2.setAllocatable(true);
        cache.addResource(terminal2, getEntityManager());

        DeviceResource mcu = new DeviceResource();
        mcu.setAddress(Address.LOCALHOST);
        mcu.addTechnology(Technology.H323);
        mcu.addTechnology(Technology.SIP);
        mcu.addCapability(new VirtualRoomsCapability(10));
        mcu.setAllocatable(true);
        cache.addResource(mcu, getEntityManager());

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setType(ReservationRequestType.NORMAL);
        reservationRequest.addRequestedSlot(new AbsoluteDateTimeSpecification("2012-06-22T14:00"), new Period("PT2H"));
        Compartment compartment = reservationRequest.addRequestedCompartment();
        compartment.addRequestedResource(new ExistingResourceSpecification(terminal1));
        compartment.addRequestedResource(new ExistingResourceSpecification(terminal2));

        checkSuccessfulAllocation(reservationRequest, cache);
    }

    //@Test
    public void testMultipleVirtualRooms() throws Exception
    {
        // TODO: Implement scheduling of multiple virtual rooms

        Cache cache = new Cache();
        cache.setEntityManagerFactory(getEntityManagerFactory());
        cache.init();

        DeviceResource mcu1 = new DeviceResource();
        mcu1.addTechnology(Technology.H323);
        mcu1.addCapability(new VirtualRoomsCapability(6));
        cache.addResource(mcu1, getEntityManager());

        DeviceResource mcu2 = new DeviceResource();
        mcu2.addTechnology(Technology.H323);
        mcu2.addCapability(new VirtualRoomsCapability(6));
        cache.addResource(mcu2, getEntityManager());

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setType(ReservationRequestType.NORMAL);
        reservationRequest.addRequestedSlot(new AbsoluteDateTimeSpecification("2012-06-22T14:00"), new Period("PT2H"));
        Compartment compartment = reservationRequest.addRequestedCompartment();
        compartment.addRequestedResource(new ExternalEndpointSpecification(Technology.H323, 10));

        checkSuccessfulAllocation(reservationRequest, cache);
    }
}
