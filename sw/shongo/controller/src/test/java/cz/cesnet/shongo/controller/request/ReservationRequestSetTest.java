package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.common.AbsoluteDateTimeSpecification;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import cz.cesnet.shongo.controller.resource.*;
import org.joda.time.Interval;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.List;

import static junit.framework.Assert.*;

/**
 * Test for processing {@link ReservationRequestSet} by {@link Preprocessor} and {@link Scheduler}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestSetTest extends AbstractDatabaseTest
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
        // Identifier for reservation request set which is created
        Long reservationRequestSetId = null;
        // Identifier for reservation request which is created from the reservation request set
        Long reservationRequestId = null;

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

            ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
            reservationRequestSet.setType(ReservationRequestType.NORMAL);
            reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
            reservationRequestSet.addRequestedSlot(new AbsoluteDateTimeSpecification("2012-06-22T14:00"), "PT2H");
            CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
            // Requests 3 guests
            compartmentSpecification.addSpecification(new ExternalEndpointSpecification(Technology.H323, 3));
            // Request specific persons, the first will use specified H.323 endpoint and
            // the second must select an endpoint then
            Person person1 = new Person("Martin Srom", "srom@cesnet.cz");
            Person person2 = new Person("Ondrej Bouda", "bouda@cesnet.cz");
            compartmentSpecification.addSpecification(new PersonSpecification(person1,
                    new ExternalEndpointSpecification(Technology.H323, new Alias(AliasType.E164, "950080085"))));
            compartmentSpecification.addSpecification(new PersonSpecification(person2));
            reservationRequestSet.addSpecification(compartmentSpecification);

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            reservationRequestManager.create(reservationRequestSet);

            personId1 = person1.getId();
            assertNotNull("The person should have assigned identifier", personId1);
            personId2 = person2.getId();
            assertNotNull("The person should have assigned identifier", personId2);
            reservationRequestSetId = reservationRequestSet.getId();
            assertNotNull("The reservation request set should have assigned identifier", reservationRequestSetId);

            entityManager.getTransaction().commit();
            entityManager.close();
        }

        // ----------------------------------------------------------
        // Create compartment request(s) from reservation request(s)
        // ----------------------------------------------------------
        {
            EntityManager entityManager = getEntityManager();

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            assertNotNull("The reservation request set should be stored in database",
                    reservationRequestManager.getReservationRequestSet(reservationRequestSetId));

            Preprocessor.createAndRun(interval, entityManager);

            List<ReservationRequest> compartmentRequestList =
                    reservationRequestManager.listReservationRequestsBySet(reservationRequestSetId);
            assertEquals("One reservation request should be created for the reservation request set", 1,
                    compartmentRequestList.size());
            assertEquals("No complete reservation requests should be present", 0,
                    reservationRequestManager.listCompletedReservationRequests(interval).size());

            reservationRequestId = compartmentRequestList.get(0).getId();
            assertNotNull("The compartment request should have assigned identifier", reservationRequestId);

            entityManager.close();
        }

        // ------------------------------------------
        // Persons accepts or rejects the invitation
        // ------------------------------------------
        {
            EntityManager entityManager = getEntityManager();
            entityManager.getTransaction().begin();

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

            // First person rejects
            reservationRequestManager.rejectPersonRequest(reservationRequestId, personId1);
            assertEquals("No complete reservation requests should be present", 0,
                    reservationRequestManager.listCompletedReservationRequests(interval).size());

            // Second person accepts and fails because he must select an endpoint first
            try {
                reservationRequestManager.acceptPersonRequest(reservationRequestId, personId2);
                fail("Person shouldn't accept the invitation because he should have selected an endpoint first!");
            }
            catch (IllegalStateException exception) {
            }

            // Second person accepts
            reservationRequestManager.selectEndpointForPersonSpecification(reservationRequestId, personId2,
                    new ExternalEndpointSpecification(Technology.H323, new Alias(AliasType.E164, "950080086")));
            reservationRequestManager.acceptPersonRequest(reservationRequestId, personId2);
            assertEquals("One complete reservation request should be present", 1,
                    reservationRequestManager.listCompletedReservationRequests(interval).size());

            entityManager.getTransaction().commit();
            entityManager.close();
        }

        // -----------------------------------------
        // Schedule complete compartment request(s)
        // -----------------------------------------
        {
            EntityManager entityManager = getEntityManager();

            Scheduler.createAndRun(interval, entityManager, cache);

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            ReservationManager reservationManager = new ReservationManager(entityManager);

            ReservationRequest reservationRequest =
                    reservationRequestManager.getReservationRequest(reservationRequestId);
            assertEquals("Reservation request should be in ALLOCATED state.",
                    ReservationRequest.State.ALLOCATED, reservationRequest.getState());

            Reservation reservation = reservationManager.getByReservationRequest(reservationRequestId);
            assertNotNull("Reservation should be created for the reservation request", reservation);

            entityManager.close();
        }

        // ---------------------------
        // Modify compartment request
        // ---------------------------
        {
            EntityManager entityManager = getEntityManager();

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            ReservationManager reservationManager = new ReservationManager(entityManager);

            // Add new requested resource to exceed the maximum number of ports
            entityManager.getTransaction().begin();
            ReservationRequestSet reservationRequestSet = reservationRequestManager.getReservationRequestSet(
                    reservationRequestSetId);
            CompartmentSpecification compartmentSpecification =
                    (CompartmentSpecification) reservationRequestSet.getSpecifications().get(0);
            compartmentSpecification.addSpecification(new ExternalEndpointSpecification(Technology.H323, 100));
            reservationRequestManager.update(reservationRequestSet);
            entityManager.getTransaction().commit();

            // Pre-process and schedule compartment request
            Preprocessor.createAndRun(interval, entityManager);

            Scheduler.createAndRun(interval, entityManager, cache);

            // Checks allocation failed
            ReservationRequest reservationRequest = reservationRequestManager.getReservationRequest(reservationRequestId);
            assertEquals("Reservation request should be in ALLOCATION_FAILED state.",
                    ReservationRequest.State.ALLOCATION_FAILED, reservationRequest.getState());
            Reservation reservation = reservationManager.getByReservationRequest(reservationRequestId);
            assertNull("Reservation should not be created for the reservation request", reservation);

            // Modify specification to not exceed the maximum number of ports
            entityManager.getTransaction().begin();
            ExternalEndpointSpecification externalEndpointSpecification =
                    (ExternalEndpointSpecification) compartmentSpecification.getSpecifications().get(3);
            externalEndpointSpecification.setCount(96);
            reservationRequestManager.update(reservationRequestSet);
            entityManager.getTransaction().commit();

            // Pre-process and schedule compartment request
            Preprocessor.createAndRun(interval, entityManager);
            Scheduler.createAndRun(interval, entityManager, cache);

            // Checks allocated
            entityManager.refresh(reservationRequest);
            assertEquals("Reservation request should be in ALLOCATED state.",
                    ReservationRequest.State.ALLOCATED, reservationRequest.getState());
        }

        // ---------------------------
        // Delete reservation request
        // ---------------------------
        {
            EntityManager entityManager = getEntityManager();

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

            // Delete reservation request
            entityManager.getTransaction().begin();
            ReservationRequestSet reservationRequestSet =
                    reservationRequestManager.getReservationRequestSet(reservationRequestSetId);
            reservationRequestManager.delete(reservationRequestSet);
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
     * @param reservationRequestSet
     * @param cache
     * @throws Exception
     */
    private void checkSuccessfulAllocation(ReservationRequestSet reservationRequestSet, Cache cache)
            throws Exception
    {
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(getEntityManager());
        reservationRequestManager.create(reservationRequestSet);

        Interval interval = Interval.parse("0/9999");
        Preprocessor.createAndRun(interval, getEntityManager());
        Scheduler.createAndRun(interval, getEntityManager(), cache);

        List<ReservationRequest> reservationRequests =
                reservationRequestManager.listReservationRequestsBySet(reservationRequestSet);
        assertEquals(1, reservationRequests.size());
        ReservationRequest reservationRequest = reservationRequests.get(0);

        ReservationManager reservationManager = new ReservationManager(getEntityManager());
        List<Reservation> reservations = reservationManager.listByReservationRequest(reservationRequestSet);
        if (reservations.size() == 0) {
            System.err.println(reservationRequest.getReportText());
            Thread.sleep(100);
        }
        assertEquals("Reservation request should be in ALLOCATED state.",
                ReservationRequest.State.ALLOCATED, reservationRequest.getState());
        assertEquals("Reservation should be allocated for the reservation request.", 1, reservations.size());
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

        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.setType(ReservationRequestType.NORMAL);
        reservationRequestSet.addRequestedSlot(new AbsoluteDateTimeSpecification("2012-06-22T14:00"), "PT2H");
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExistingEndpointSpecification(terminal1));
        compartmentSpecification.addSpecification(new ExistingEndpointSpecification(terminal2));
        reservationRequestSet.addSpecification(compartmentSpecification);

        checkSuccessfulAllocation(reservationRequestSet, cache);
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

        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.setType(ReservationRequestType.NORMAL);
        reservationRequestSet.addRequestedSlot(new AbsoluteDateTimeSpecification("2012-06-22T14:00"), "PT2H");
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExistingEndpointSpecification(terminal1));
        compartmentSpecification.addSpecification(new ExistingEndpointSpecification(terminal2));
        reservationRequestSet.addSpecification(compartmentSpecification);

        checkSuccessfulAllocation(reservationRequestSet, cache);
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

        ReservationRequestSet reservationRequest = new ReservationRequestSet();
        reservationRequest.setType(ReservationRequestType.NORMAL);
        reservationRequest.addRequestedSlot(new AbsoluteDateTimeSpecification("2012-06-22T14:00"), "PT2H");
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExistingEndpointSpecification(terminal1));
        compartmentSpecification.addSpecification(new ExistingEndpointSpecification(terminal2));
        reservationRequest.addSpecification(compartmentSpecification);

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

        ReservationRequestSet reservationRequest = new ReservationRequestSet();
        reservationRequest.setType(ReservationRequestType.NORMAL);
        reservationRequest.addRequestedSlot(new AbsoluteDateTimeSpecification("2012-06-22T14:00"), "PT2H");
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSpecification(Technology.H323, 10));
        reservationRequest.addSpecification(compartmentSpecification);

        checkSuccessfulAllocation(reservationRequest, cache);
    }
}
