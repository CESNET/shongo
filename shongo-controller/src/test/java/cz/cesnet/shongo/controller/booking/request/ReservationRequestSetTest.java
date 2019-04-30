package cz.cesnet.shongo.controller.booking.request;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractSchedulerTest;
import cz.cesnet.shongo.controller.DummyAuthorization;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.alias.Alias;
import cz.cesnet.shongo.controller.booking.alias.AliasProviderCapability;
import cz.cesnet.shongo.controller.booking.compartment.CompartmentSpecification;
import cz.cesnet.shongo.controller.booking.participant.AbstractParticipant;
import cz.cesnet.shongo.controller.booking.participant.ExternalEndpointParticipant;
import cz.cesnet.shongo.controller.booking.participant.ExternalEndpointSetParticipant;
import cz.cesnet.shongo.controller.booking.participant.InvitedPersonParticipant;
import cz.cesnet.shongo.controller.booking.person.AbstractPerson;
import cz.cesnet.shongo.controller.booking.person.AnonymousPerson;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.booking.resource.ResourceSpecification;
import cz.cesnet.shongo.controller.booking.room.RoomProviderCapability;
import cz.cesnet.shongo.controller.scheduler.Preprocessor;
import cz.cesnet.shongo.controller.scheduler.Scheduler;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Test for processing {@link ReservationRequestSet} by {@link Preprocessor} and {@link Scheduler}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestSetTest extends AbstractSchedulerTest
{
    // Authorization
    private Authorization authorization;
    // Preprocessor
    private Preprocessor preprocessor;
    // Scheduler
    private Scheduler scheduler;

    @Before
    public void init()
    {
        // ------------
        // Setup cache
        // ------------
        {
            authorization = new DummyAuthorization(getEntityManagerFactory());

            preprocessor = new Preprocessor();
            preprocessor.setCache(getCache());
            preprocessor.setAuthorization(authorization);
            preprocessor.init();

            scheduler = new Scheduler(getCache(), null, null);
            scheduler.setAuthorization(authorization);
            scheduler.init();

            DeviceResource deviceResource = new DeviceResource();
            deviceResource.addTechnology(Technology.H323);
            deviceResource.addCapability(new RoomProviderCapability(100));
            deviceResource.addCapability(new AliasProviderCapability("950000001", AliasType.H323_E164, true));
            deviceResource.setAllocatable(true);
            createResource(deviceResource);
        }
    }

    @Test
    public void test() throws Exception
    {
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
            compartmentSpecification.addParticipant(new ExternalEndpointSetParticipant(Technology.H323, 3));
            // Request specific persons, the first will use specified H.323 endpoint and
            // the second must select an endpoint then
            AbstractPerson person1 = new AnonymousPerson("Martin Srom", "srom@cesnet.cz");
            AbstractPerson person2 = new AnonymousPerson("Ondrej Bouda", "bouda@cesnet.cz");
            compartmentSpecification.addParticipant(new InvitedPersonParticipant(person1,
                    new ExternalEndpointParticipant(Technology.H323, new Alias(AliasType.H323_E164, "950080085"))));
            compartmentSpecification.addParticipant(new InvitedPersonParticipant(person2));
            reservationRequestSet.setSpecification(compartmentSpecification);

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            reservationRequestManager.create(reservationRequestSet);

            reservationRequestSetId = reservationRequestSet.getId();
            Assert.assertNotNull("The reservation request set should have assigned identifier",
                    reservationRequestSetId);

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
            for (AbstractParticipant participant : specification.getParticipants()) {
                if (participant instanceof InvitedPersonParticipant) {
                    InvitedPersonParticipant invitedPersonParticipant = (InvitedPersonParticipant) participant;
                    if (invitedPersonParticipant.getEndpointParticipant() != null) {
                        personId1 = invitedPersonParticipant.getPerson().getId();
                    }
                    else {
                        personId2 = invitedPersonParticipant.getPerson().getId();
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
            reservationRequestManager.rejectInvitedPersonParticipant(reservationRequestId, personId1);
            Assert.assertEquals("No complete reservation requests should be present", 0,
                    reservationRequestManager.listCompletedReservationRequests(interval).size());

            // Second person accepts and fails because he must select an endpoint first
            try {
                reservationRequestManager.acceptInvitedPersonParticipant(reservationRequestId, personId2);
                Assert.fail(
                        "Person shouldn't accept the invitation because he should have selected an endpoint first!");
            }
            catch (RuntimeException exception) {
            }

            // Second person accepts
            reservationRequestManager.selectEndpointForInvitedPersonParticipant(reservationRequestId, personId2,
                    new ExternalEndpointParticipant(Technology.H323, new Alias(AliasType.H323_E164, "950080086")));
            reservationRequestManager.acceptInvitedPersonParticipant(reservationRequestId, personId2);
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
            compartmentSpecification.addParticipant(new ExternalEndpointSetParticipant(Technology.H323, 100));
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
            ExternalEndpointSetParticipant externalEndpointSpecification =
                    (ExternalEndpointSetParticipant) compartmentSpecification.getCompartmentSpecifications().get(3);
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
            authorizationManager.commitTransaction(null);

            // Pre-process and schedule
            preprocessor.run(interval, entityManager);
            scheduler.run(interval, entityManager);

            entityManager.close();
        }
    }

    @Test
    public void testResourceReservationRequestModificationTest()
    {
        Resource resource = new Resource();
        resource.setAllocatable(true);
        createResource(resource);

        Interval interval = Interval.parse("2012-06-01/2012-06-30T23:59:59");

        EntityManager entityManager = createEntityManager();
        entityManager.getTransaction().begin();

        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.setCreatedBy(Authorization.ROOT_USER_ID);
        reservationRequestSet.setUpdatedBy(Authorization.ROOT_USER_ID);
        reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSet.addSlot("2012-06-22T14:00", "PT2H");
        reservationRequestSet.setSpecification(new ResourceSpecification(resource));

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        reservationRequestManager.create(reservationRequestSet);

        long reservationRequestSetId = reservationRequestSet.getId();
        Assert.assertNotNull("The reservation request set should have assigned identifier",
                reservationRequestSetId);

        entityManager.getTransaction().commit();
        entityManager.close();

        entityManager = createEntityManager();
        preprocessor.run(interval, entityManager);
        scheduler.run(interval, entityManager);
        entityManager.close();

        /*
         * Modify ReservationRequestSet to ReservationRequest
         */

        entityManager = createEntityManager();
        entityManager.getTransaction().begin();

        reservationRequestManager = new ReservationRequestManager(entityManager);
        ReservationRequestSet oldReservationRequest = reservationRequestManager.getReservationRequestSet(reservationRequestSetId);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setCreatedBy(Authorization.ROOT_USER_ID);
        reservationRequest.setUpdatedBy(Authorization.ROOT_USER_ID);
        reservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequest.setSlot(new DateTime("2012-06-22T14:00"), new Period("PT2H"));
        reservationRequest.setSpecification(new ResourceSpecification(resource));
        reservationRequest.setModifiedReservationRequest(oldReservationRequest);

        reservationRequestManager.modify(oldReservationRequest, reservationRequest);

        entityManager.getTransaction().commit();
        entityManager.close();
        entityManager = createEntityManager();

        preprocessor.run(interval, entityManager);
        scheduler.run(interval, entityManager);
        scheduler.run(interval, entityManager);

        entityManager.close();
        entityManager = createEntityManager();

        reservationRequestManager = new ReservationRequestManager(entityManager);
        reservationRequest = reservationRequestManager.getReservationRequest(reservationRequest.getId());

        Assert.assertEquals(ReservationRequest.AllocationState.ALLOCATED, reservationRequest.getAllocationState());

        entityManager.close();
    }
}
