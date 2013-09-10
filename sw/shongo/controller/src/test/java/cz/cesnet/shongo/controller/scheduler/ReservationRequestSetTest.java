package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.common.OtherPerson;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.request.*;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.resource.AliasProviderCapability;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.RoomProviderCapability;
import org.joda.time.Interval;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Test for processing {@link cz.cesnet.shongo.controller.request.ReservationRequestSet} by {@link Preprocessor} and {@link Scheduler}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestSetTest extends AbstractSchedulerTest
{
    @Test
    public void test() throws Exception
    {
        // Authorization
        Authorization authorization;
        // Preprocessor
        Preprocessor preprocessor = null;
        // Scheduler
        Scheduler scheduler = null;
        // Interval for which a preprocessor and a scheduler runs and
        // in which the reservation request compartment takes place
        Interval interval = Interval.parse("2012-06-01/2012-06-30T23:59:59");
        // Id for reservation request set which is created
        Long reservationRequestSetId = null;
        // Id for reservation request which is created from the reservation request set
        Long reservationRequestId = null;
        // Ids for persons which are requested to participate in the compartment
        Long personId1 = null;
        Long personId2 = null;

        // ------------
        // Setup cache
        // ------------
        {
            authorization = new DummyAuthorization(getEntityManagerFactory());

            preprocessor = new Preprocessor();
            preprocessor.setCache(getCache());
            preprocessor.setAuthorization(authorization);
            preprocessor.init();

            scheduler = new Scheduler();
            scheduler.setCache(getCache());
            scheduler.setAuthorization(authorization);
            scheduler.init();

            DeviceResource deviceResource = new DeviceResource();
            deviceResource.addTechnology(Technology.H323);
            deviceResource.addCapability(new RoomProviderCapability(100));
            deviceResource.addCapability(new AliasProviderCapability("950000001", AliasType.H323_E164, true));
            deviceResource.setAllocatable(true);
            createResource(deviceResource);
        }

        // ---------------------------
        // Create reservation request
        // ---------------------------
        {
            EntityManager entityManager = createEntityManager();
            entityManager.getTransaction().begin();

            ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
            reservationRequestSet.setCreatedBy(Authorization.ROOT_USER_ID);
            reservationRequestSet.setUpdatedBy(Authorization.ROOT_USER_ID);
            reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
            reservationRequestSet.addSlot("2012-06-22T14:00", "PT2H");
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
            reservationRequestSet.setSpecification(compartmentSpecification);

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            reservationRequestManager.create(reservationRequestSet);

            reservationRequestSetId = reservationRequestSet.getId();
            Assert.assertNotNull("The reservation request set should have assigned identifier", reservationRequestSetId);

            entityManager.getTransaction().commit();
            entityManager.close();
        }

        // -----------------------------------------------------------
        // Create reservation request(s) from reservation request set
        // -----------------------------------------------------------
        {
            EntityManager entityManager = createEntityManager();

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            Assert.assertNotNull("The reservation request set should be stored in database",
                    reservationRequestManager.getReservationRequestSet(reservationRequestSetId));

            preprocessor.run(interval, entityManager);

            ReservationRequestSet reservationRequestSet =
                    reservationRequestManager.getReservationRequestSet(reservationRequestSetId);
            List<ReservationRequest> reservationRequests =
                    reservationRequestManager.listChildReservationRequests(reservationRequestSet);
            Assert.assertEquals("One reservation request should be created for the reservation request set", 1,
                    reservationRequests.size());
            Assert.assertEquals("No complete reservation requests should be present", 0,
                    reservationRequestManager.listCompletedReservationRequests(interval).size());

            ReservationRequest reservationRequest = reservationRequests.get(0);
            reservationRequestId = reservationRequest.getId();
            Assert.assertNotNull("The compartment request should have assigned identifier", reservationRequestId);

            CompartmentSpecification specification = (CompartmentSpecification) reservationRequest.getSpecification();
            for (ParticipantSpecification childSpecification : specification.getParticipantSpecifications()) {
                if (childSpecification instanceof PersonSpecification) {
                    PersonSpecification personSpecification = (PersonSpecification) childSpecification;
                    if (personSpecification.getEndpointSpecification() != null) {
                        personId1 = personSpecification.getPerson().getId();
                    }
                    else {
                        personId2 = personSpecification.getPerson().getId();
                    }
                }
            }

            entityManager.close();
        }

        // ------------------------------------------
        // Persons accepts or rejects the invitation
        // ------------------------------------------
        {
            EntityManager entityManager = createEntityManager();
            entityManager.getTransaction().begin();

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

            // First person rejects
            reservationRequestManager.rejectPersonRequest(reservationRequestId, personId1);
            Assert.assertEquals("No complete reservation requests should be present", 0,
                    reservationRequestManager.listCompletedReservationRequests(interval).size());

            // Second person accepts and fails because he must select an endpoint first
            try {
                reservationRequestManager.acceptPersonRequest(reservationRequestId, personId2);
                Assert.fail("Person shouldn't accept the invitation because he should have selected an endpoint first!");
            }
            catch (RuntimeException exception) {
            }

            // Second person accepts
            reservationRequestManager.selectEndpointForPersonSpecification(reservationRequestId, personId2,
                    new ExternalEndpointSpecification(Technology.H323, new Alias(AliasType.H323_E164, "950080086")));
            reservationRequestManager.acceptPersonRequest(reservationRequestId, personId2);
            Assert.assertEquals("One complete reservation request should be present", 1,
                    reservationRequestManager.listCompletedReservationRequests(interval).size());

            entityManager.getTransaction().commit();
            entityManager.close();
        }

        // -----------------------------------------
        // Schedule complete reservation request(s)
        // -----------------------------------------
        {
            EntityManager entityManager = createEntityManager();

            scheduler.run(interval, entityManager);

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            ReservationManager reservationManager = new ReservationManager(entityManager);

            ReservationRequest reservationRequest =
                    reservationRequestManager.getReservationRequest(reservationRequestId);
            Assert.assertEquals("Reservation request should be in ALLOCATED state.",
                    ReservationRequest.AllocationState.ALLOCATED, reservationRequest.getAllocationState());

            Assert.assertTrue("Reservation should be created for the reservation request",
                    reservationRequest.getAllocation().getReservations().size() > 0);

            entityManager.close();
        }

        // ---------------------------
        // Modify compartment request
        // ---------------------------
        // TODO: keep state of StatefulSpecifications when modification is applied (e.g., invitation state)
        /*{
            EntityManager entityManager = createEntityManager();

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            ReservationManager reservationManager = new ReservationManager(entityManager);

            // Add new requested resource to exceed the maximum number of ports
            entityManager.getTransaction().begin();
            ReservationRequestSet reservationRequestSet = reservationRequestManager.getReservationRequestSet(
                    reservationRequestSetId);
            CompartmentSpecification compartmentSpecification =
                    (CompartmentSpecification) reservationRequestSet.getSpecification();
            compartmentSpecification.addChildSpecification(new ExternalEndpointSetSpecification(Technology.H323, 100));
            reservationRequestManager.update(reservationRequestSet);
            entityManager.getTransaction().commit();

            // Pre-process and schedule compartment request
            preprocessor.run(interval, entityManager);

            scheduler.run(interval, entityManager);

            // Checks allocation failed
            ReservationRequest reservationRequest = reservationRequestManager
                    .getReservationRequest(reservationRequestId);
            Assert.assertEquals("Reservation request should be in ALLOCATION_FAILED state.",
                    ReservationRequest.State.ALLOCATION_FAILED, reservationRequest.getAllocationState());
            Reservation reservation = reservationRequest.getAllocation().getCurrentReservation();
            Assert.assertEquals("Old reservation should be kept for the reservation request",
                    reservationId, reservation.getId());

            // Modify specification to not exceed the maximum number of ports
            entityManager.getTransaction().begin();
            ExternalEndpointSetSpecification externalEndpointSpecification =
                    (ExternalEndpointSetSpecification) compartmentSpecification.getCompartmentSpecifications().get(3);
            externalEndpointSpecification.setCount(96);
            reservationRequestManager.update(reservationRequestSet);
            entityManager.getTransaction().commit();

            // Pre-process and schedule compartment request
            preprocessor.run(interval, entityManager);
            scheduler.run(interval, entityManager);

            // Checks allocated
            entityManager.refresh(reservationRequest);
            Assert.assertEquals("Reservation request should be in ALLOCATED state.",
                    ReservationRequest.State.ALLOCATED, reservationRequest.getAllocationState());

            entityManager.close();
        }*/

        // ---------------------------
        // Delete reservation request
        // ---------------------------
        {
            EntityManager entityManager = createEntityManager();

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);

            // Delete reservation request
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();
            ReservationRequestSet reservationRequestSet =
                    reservationRequestManager.getReservationRequestSet(reservationRequestSetId);
            reservationRequestManager.softDelete(reservationRequestSet, authorizationManager);
            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();

            // Pre-process and schedule
            preprocessor.run(interval, entityManager);
            scheduler.run(interval, entityManager);

            entityManager.close();
        }
    }
}
