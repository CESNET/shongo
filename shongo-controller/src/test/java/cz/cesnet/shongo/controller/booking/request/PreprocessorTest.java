package cz.cesnet.shongo.controller.booking.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.booking.compartment.CompartmentSpecification;
import cz.cesnet.shongo.controller.booking.compartment.MultiCompartmentSpecification;
import cz.cesnet.shongo.controller.booking.participant.ExternalEndpointSetParticipant;
import cz.cesnet.shongo.controller.booking.participant.InvitedPersonParticipant;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.booking.person.AnonymousPerson;
import cz.cesnet.shongo.controller.booking.datetime.PeriodicDateTime;
import cz.cesnet.shongo.controller.scheduler.Preprocessor;
import cz.cesnet.shongo.report.Report;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.junit.Assert;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

/**
 * Tests for using {@link cz.cesnet.shongo.controller.scheduler.Preprocessor} that synchronizes {@link cz.cesnet.shongo.controller.booking.request.ReservationRequestSet}(s)
 * with {@link cz.cesnet.shongo.controller.booking.request.ReservationRequest}(s).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PreprocessorTest extends AbstractDatabaseTest
{
    @Override
    public void before() throws Exception
    {
        super.before();

        LocalDomain.setLocalDomain(new LocalDomain("cz.cesnet"));
    }

    @Override
    public void after() throws Exception
    {
        LocalDomain.setLocalDomain(null);

        super.after();
    }

    @Test
    public void test() throws Exception
    {
        EntityManager entityManager = createEntityManager();

        Preprocessor preprocessor = new Preprocessor();
        preprocessor.setCache(new Cache());
        preprocessor.setAuthorization(new DummyAuthorization(getEntityManagerFactory()));
        preprocessor.init();

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

        // Create reservation request
        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.setCreatedBy(Authorization.ROOT_USER_ID);
        reservationRequestSet.setUpdatedBy(Authorization.ROOT_USER_ID);
        reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSet.addSlot("2012-06-01T15", "PT1H");
        reservationRequestSet.addSlot(new PeriodicDateTime(
                DateTime.parse("2012-07-01T14:00"), Period.parse("P1W"), LocalDate.parse("2012-07-15")), "PT2H");
        reservationRequestSet.setSpecification(new CompartmentSpecification());

        // Save it
        entityManager.getTransaction().begin();
        reservationRequestManager.create(reservationRequestSet);
        entityManager.getTransaction().commit();

        // Run preprocessor
        preprocessor.run(new Interval(
                DateTime.parse("2012-06-01T00:00:00"), DateTime.parse("2012-06-01T23:59:59")), entityManager);
        Assert.assertEquals(1, reservationRequestManager.listChildReservationRequests(reservationRequestSet).size());
        preprocessor.run(new Interval(
                DateTime.parse("2012-07-02T00:00:00"), DateTime.parse("2012-07-08T23:59:59")), entityManager);
        Assert.assertEquals(2, reservationRequestManager.listChildReservationRequests(reservationRequestSet).size());
        preprocessor.run(new Interval(
                DateTime.parse("2012-06-01T00:00:00"), DateTime.parse("2012-07-08T23:59:59")), entityManager);
        Assert.assertEquals(3, reservationRequestManager.listChildReservationRequests(reservationRequestSet).size());

        // Check created reservation requests
        List<ReservationRequest> reservationRequests =
                reservationRequestManager.listChildReservationRequests(reservationRequestSet);
        Assert.assertEquals(3, reservationRequests.size());
        Assert.assertEquals(new Interval(DateTime.parse("2012-06-01T15:00"), Period.parse("PT1H")),
                reservationRequests.get(0).getSlot());
        Assert.assertEquals(new Interval(DateTime.parse("2012-07-01T14:00"), Period.parse("PT2H")),
                reservationRequests.get(1).getSlot());
        Assert.assertEquals(new Interval(DateTime.parse("2012-07-08T14:00"), Period.parse("PT2H")),
                reservationRequests.get(2).getSlot());

        // Modify reservation request
        reservationRequestSet.setPurpose(ReservationRequestPurpose.EDUCATION);
        reservationRequestSet.removeSlot(reservationRequestSet.getSlots().get(0));

        // Update request
        reservationRequestManager.update(reservationRequestSet);

        // Run preprocessor
        preprocessor.run(new Interval(
                DateTime.parse("2012-06-01T00:00:00"), DateTime.parse("2012-07-08T23:59:59")), entityManager);

        // Check modified requests
        reservationRequests = reservationRequestManager.listChildReservationRequests(reservationRequestSet);
        Assert.assertEquals(2, reservationRequests.size());
        Assert.assertEquals(new Interval(DateTime.parse("2012-07-01T14:00"), Period.parse("PT2H")),
                reservationRequests.get(0).getSlot());
        Assert.assertEquals(new Interval(DateTime.parse("2012-07-08T14:00"), Period.parse("PT2H")),
                reservationRequests.get(1).getSlot());

        entityManager.close();
    }

    @Test
    public void testClonedSpecifications() throws Exception
    {
        Preprocessor preprocessor = new Preprocessor();
        preprocessor.setCache(new Cache());
        preprocessor.setAuthorization(new DummyAuthorization(getEntityManagerFactory()));
        preprocessor.init();

        EntityManager entityManager = createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.setCreatedBy(Authorization.ROOT_USER_ID);
        reservationRequestSet.setUpdatedBy(Authorization.ROOT_USER_ID);
        reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSet.addSlot("2012-01-01", "PT1H");
        reservationRequestSet.addSlot("2012-01-02", "PT1H");
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addParticipant(new ExternalEndpointSetParticipant(Technology.H323, 2));
        compartmentSpecification.addParticipant(
                new InvitedPersonParticipant(new AnonymousPerson("Martin Srom", "srom@cesnet.cz")));
        reservationRequestSet.setSpecification(compartmentSpecification);

        entityManager.getTransaction().begin();
        reservationRequestManager.create(reservationRequestSet);
        entityManager.getTransaction().commit();

        preprocessor.run(new Interval(
                DateTime.parse("2012-01-01"), DateTime.parse("2012-01-03")), entityManager);

        List<ReservationRequest> reservationRequests =
                reservationRequestManager.listChildReservationRequests(reservationRequestSet);
        Assert.assertEquals(2, reservationRequests.size());
        Assert.assertThat("Compartment specifications in reservation requests created from single reservation request set"
                + " should be different database instances.",
                reservationRequests.get(0).getSpecification().getId(),
                is(not(reservationRequests.get(1).getSpecification().getId())));
        CompartmentSpecification compartmentSpecification1 =
                (CompartmentSpecification) reservationRequests.get(0).getSpecification();
        CompartmentSpecification compartmentSpecification2 =
                (CompartmentSpecification) reservationRequests.get(1).getSpecification();
        Assert.assertThat("External endpoint specifications in reservation requests created from single"
                + " reservation request set should be different database instances.",
                compartmentSpecification1.getParticipants().get(0).getId(),
                is(not(compartmentSpecification2.getParticipants().get(0).getId())));
        Assert.assertThat("Person specifications in reservation requests created from single reservation request set"
                + " should be different database instances.",
                compartmentSpecification1.getParticipants().get(1).getId(),
                is(not(compartmentSpecification2.getParticipants().get(1).getId())));

        entityManager.close();
    }

    @Test
    public void testModification() throws Exception
    {
        Preprocessor preprocessor = new Preprocessor();
        preprocessor.setCache(new Cache());
        preprocessor.setAuthorization(new DummyAuthorization(getEntityManagerFactory()));
        preprocessor.init();

        EntityManager entityManager = createEntityManager();

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        try {
            // Create reservation request set
            ReservationRequestSet oldReservationRequest = new ReservationRequestSet();
            oldReservationRequest.setCreatedBy(Authorization.ROOT_USER_ID);
            oldReservationRequest.setUpdatedBy(Authorization.ROOT_USER_ID);
            oldReservationRequest.setPurpose(ReservationRequestPurpose.SCIENCE);
            oldReservationRequest.addSlot("2012-01-01", "PT1H");
            oldReservationRequest.setSpecification(new CompartmentSpecification());

            entityManager.getTransaction().begin();
            reservationRequestManager.create(oldReservationRequest);
            entityManager.getTransaction().commit();

            preprocessor.run(Interval.parse("2012-01-01/2012-01-02"), entityManager);

            List<ReservationRequest> reservationRequests1 =
                    reservationRequestManager.listChildReservationRequests(oldReservationRequest);
            Assert.assertEquals(1, reservationRequests1.size());

            // Modify reservation request set
            ReservationRequestSet newReservationRequest = oldReservationRequest.clone(entityManager);
            newReservationRequest.addSlot("2012-02-01", "PT1H");
            newReservationRequest.setSpecification(new MultiCompartmentSpecification());

            entityManager.getTransaction().begin();
            reservationRequestManager.modify(oldReservationRequest, newReservationRequest);
            entityManager.getTransaction().commit();

            preprocessor.run(Interval.parse("2012-02-01/2012-02-02"), entityManager);

            List<ReservationRequest> reservationRequests2 =
                    reservationRequestManager.listChildReservationRequests(newReservationRequest);
            Assert.assertEquals(2, reservationRequests2.size());
            Assert.assertEquals("First reservation request should have old specification",
                    CompartmentSpecification.class, reservationRequests2.get(0).getSpecification().getClass());
            Assert.assertEquals("Second reservation request should have new specification",
                    MultiCompartmentSpecification.class, reservationRequests2.get(1).getSpecification().getClass());

            oldReservationRequest = newReservationRequest;

            // Modify reservation request set
            newReservationRequest = oldReservationRequest.clone(entityManager);
            newReservationRequest.addSlot("2012-03-01", "PT1H");

            entityManager.getTransaction().begin();
            reservationRequestManager.modify(oldReservationRequest, newReservationRequest);
            entityManager.getTransaction().commit();

            preprocessor.run(Interval.parse("2012-01-01/2012-03-02"), entityManager);

            List<ReservationRequest> reservationRequests3 =
                    reservationRequestManager.listChildReservationRequests(newReservationRequest);
            Assert.assertEquals(3, reservationRequests3.size());
            Assert.assertEquals("All reservation requests should have new specification",
                    MultiCompartmentSpecification.class, reservationRequests3.get(0).getSpecification().getClass());
            Assert.assertEquals("All reservation requests should have new specification",
                    MultiCompartmentSpecification.class, reservationRequests3.get(1).getSpecification().getClass());
            Assert.assertEquals("All reservation requests should have new specification",
                    MultiCompartmentSpecification.class, reservationRequests3.get(2).getSpecification().getClass());
        }
        finally {
            entityManager.close();
        }
    }
}
