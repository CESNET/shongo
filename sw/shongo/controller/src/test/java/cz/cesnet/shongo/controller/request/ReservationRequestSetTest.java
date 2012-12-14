package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.common.AbsoluteDateTimeSpecification;
import cz.cesnet.shongo.controller.common.OtherPerson;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
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
        // Ids for persons which are requested to participate in the compartment
        Long personId1 = null;
        Long personId2 = null;
        // Id for reservation request set which is created
        Long reservationRequestSetId = null;
        // Id for reservation request which is created from the reservation request set
        Long reservationRequestId = null;

        // ------------
        // Setup cache
        // ------------
        {
            cache = new Cache();
            cache.setEntityManagerFactory(getEntityManagerFactory());
            cache.init();

            EntityManager entityManager = getEntityManager();

            DeviceResource deviceResource = new DeviceResource();
            deviceResource.setUserId(Authorization.ROOT_USER_ID);
            deviceResource.setName("MCU");
            deviceResource.addTechnology(Technology.H323);
            deviceResource.addCapability(new RoomProviderCapability(100));
            deviceResource.addCapability(new AliasProviderCapability(AliasType.H323_E164, "950000001", true));
            deviceResource.setAllocatable(true);
            cache.addResource(deviceResource, entityManager);

            entityManager.close();
        }

        // ---------------------------
        // Create reservation request
        // ---------------------------
        {
            EntityManager entityManager = getEntityManager();
            entityManager.getTransaction().begin();

            ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
            reservationRequestSet.setUserId(Authorization.ROOT_USER_ID);
            reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
            reservationRequestSet.addSlot(new AbsoluteDateTimeSpecification("2012-06-22T14:00"), "PT2H");
            CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
            // Requests 3 guests
            compartmentSpecification.addChildSpecification(new ExternalEndpointSetSpecification(Technology.H323, 3));
            // Request specific persons, the first will use specified H.323 endpoint and
            // the second must select an endpoint then
            Person person1 = new OtherPerson("Martin Srom", "srom@cesnet.cz");
            Person person2 = new OtherPerson("Ondrej Bouda", "bouda@cesnet.cz");
            compartmentSpecification.addChildSpecification(new PersonSpecification(person1,
                    new ExternalEndpointSpecification(Technology.H323, new Alias(AliasType.H323_E164, "950080085"))));
            compartmentSpecification.addChildSpecification(new PersonSpecification(person2));
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
                    new ExternalEndpointSpecification(Technology.H323, new Alias(AliasType.H323_E164, "950080086")));
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

            Scheduler.createAndRun(interval, entityManager, cache, null, null);

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
            compartmentSpecification.addChildSpecification(new ExternalEndpointSetSpecification(Technology.H323, 100));
            reservationRequestManager.update(reservationRequestSet);
            entityManager.getTransaction().commit();

            // Pre-process and schedule compartment request
            Preprocessor.createAndRun(interval, entityManager);

            Scheduler.createAndRun(interval, entityManager, cache, null, null);

            // Checks allocation failed
            ReservationRequest reservationRequest = reservationRequestManager
                    .getReservationRequest(reservationRequestId);
            assertEquals("Reservation request should be in ALLOCATION_FAILED state.",
                    ReservationRequest.State.ALLOCATION_FAILED, reservationRequest.getState());
            Reservation reservation = reservationManager.getByReservationRequest(reservationRequestId);
            assertNull("Reservation should not be created for the reservation request", reservation);

            // Modify specification to not exceed the maximum number of ports
            entityManager.getTransaction().begin();
            ExternalEndpointSetSpecification externalEndpointSpecification =
                    (ExternalEndpointSetSpecification) compartmentSpecification.getSpecifications().get(3);
            externalEndpointSpecification.setCount(96);
            reservationRequestManager.update(reservationRequestSet);
            entityManager.getTransaction().commit();

            // Pre-process and schedule compartment request
            Preprocessor.createAndRun(interval, entityManager);
            Scheduler.createAndRun(interval, entityManager, cache, null, null);

            // Checks allocated
            entityManager.refresh(reservationRequest);
            assertEquals("Reservation request should be in ALLOCATED state.",
                    ReservationRequest.State.ALLOCATED, reservationRequest.getState());

            entityManager.close();
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
            Scheduler.createAndRun(interval, entityManager, cache, null, null);

            entityManager.close();
        }

        // ------------------------
        // Clean-up
        // ------------------------
        {
            cache.destroy();
        }
    }
}
