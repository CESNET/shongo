package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.common.AbsoluteDateTimeSpecification;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.resource.*;
import cz.cesnet.shongo.controller.ResourceDatabase;
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
        //
        ResourceDatabase resourceDatabase = null;
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

        // ------------------------
        // Setup resource database
        // ------------------------
        {
            resourceDatabase = new ResourceDatabase();
            resourceDatabase.setEntityManagerFactory(getEntityManagerFactory());
            resourceDatabase.init();

            DeviceResource deviceResource = new DeviceResource();
            deviceResource.setName("MCU");
            deviceResource.addTechnology(Technology.H323);
            deviceResource.addCapability(new VirtualRoomsCapability(100));
            resourceDatabase.addResource(deviceResource, getEntityManager());
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

            Preprocessor.run(getEntityManagerFactory(), interval);

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
            Scheduler.run(getEntityManagerFactory(), resourceDatabase, interval);

            // TODO: Check created allocation
        }

        // ------------------------
        // Clean-up
        // ------------------------
        {
            resourceDatabase.destroy();
        }
    }
}
