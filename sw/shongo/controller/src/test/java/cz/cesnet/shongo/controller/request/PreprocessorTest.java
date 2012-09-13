package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.AbstractDatabaseTest;
import cz.cesnet.shongo.controller.Preprocessor;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestType;
import cz.cesnet.shongo.controller.common.AbsoluteDateTimeSpecification;
import cz.cesnet.shongo.controller.common.PeriodicDateTimeSpecification;
import cz.cesnet.shongo.controller.common.Person;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.Period;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for using {@link Preprocessor} that synchronizes {@link ReservationRequestSet}(s)
 * with {@link ReservationRequest}(s).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PreprocessorTest extends AbstractDatabaseTest
{
    @Test
    public void test() throws Exception
    {
        EntityManager entityManager = getEntityManager();

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

        // Create reservation request
        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.setType(ReservationRequestType.NORMAL);
        reservationRequestSet.setPurpose(ReservationRequestPurpose.SCIENCE);
        reservationRequestSet.addRequestedSlot(new AbsoluteDateTimeSpecification(
                "2012-06-01T15"),
                Period.parse("PT1H"));
        reservationRequestSet.addRequestedSlot(new PeriodicDateTimeSpecification(
                DateTime.parse("2012-07-01T14:00"), Period.parse("P1W"), LocalDate.parse("2012-07-15")),
                Period.parse("PT2H"));
        // First compartment
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSpecification(Technology.SIP));
        EndpointSpecification endpointSpecification = new ExternalEndpointSpecification(Technology.H323);
        endpointSpecification.addPerson(new Person("Martin Srom", "martin.srom@cesnet.cz"));
        compartmentSpecification.addSpecification(endpointSpecification);
        reservationRequestSet.addRequestedSpecification(compartmentSpecification);
        // Second compartment
        compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.addSpecification(new ExternalEndpointSpecification(Technology.ADOBE_CONNECT, 2));
        reservationRequestSet.addRequestedSpecification(compartmentSpecification);

        // Save it
        reservationRequestManager.create(reservationRequestSet);

        // Run preprocessor
        Preprocessor.createAndRun(new Interval(
                DateTime.parse("2012-06-01T00:00:00"), DateTime.parse("2012-06-01T23:59:59")), entityManager);
        assertEquals(2, reservationRequestManager.listReservationRequestsBySet(reservationRequestSet).size());
        Preprocessor.createAndRun(new Interval(
                DateTime.parse("2012-07-02T00:00:00"), DateTime.parse("2012-07-08T23:59:59")), entityManager);
        assertEquals(4, reservationRequestManager.listReservationRequestsBySet(reservationRequestSet).size());
        Preprocessor.createAndRun(new Interval(
                DateTime.parse("2012-06-01T00:00:00"), DateTime.parse("2012-07-08T23:59:59")), entityManager);
        assertEquals(6, reservationRequestManager.listReservationRequestsBySet(reservationRequestSet).size());

        // Check created compartments
        List<ReservationRequest> reservationRequests =
                reservationRequestManager.listReservationRequestsBySet(reservationRequestSet);
        assertEquals(6, reservationRequests.size());
        assertEquals(new Interval(DateTime.parse("2012-06-01T15:00"), Period.parse("PT1H")),
                reservationRequests.get(0).getRequestedSlot());
        assertEquals(new Interval(DateTime.parse("2012-07-01T14:00"), Period.parse("PT2H")),
                reservationRequests.get(2).getRequestedSlot());
        assertEquals(new Interval(DateTime.parse("2012-07-08T14:00"), Period.parse("PT2H")),
                reservationRequests.get(4).getRequestedSlot());

        // Modify reservation request
        reservationRequestSet.setPurpose(ReservationRequestPurpose.EDUCATION);
        reservationRequestSet.removeRequestedSlot(reservationRequestSet.getRequestedSlots().get(0));
        reservationRequestSet.removeRequestedSpecification(reservationRequestSet.getRequestedSpecifications().get(1));

        // Update request
        reservationRequestManager.update(reservationRequestSet);

        // Run preprocessor
        Preprocessor.createAndRun(new Interval(
                DateTime.parse("2012-06-01T00:00:00"), DateTime.parse("2012-07-08T23:59:59")), entityManager);

        // Check modified compartments
        reservationRequests = reservationRequestManager.listReservationRequestsBySet(reservationRequestSet);
        assertEquals(2, reservationRequests.size());
        assertEquals(new Interval(DateTime.parse("2012-07-01T14:00"), Period.parse("PT2H")),
                reservationRequests.get(0).getRequestedSlot());
        assertEquals(new Interval(DateTime.parse("2012-07-08T14:00"), Period.parse("PT2H")),
                reservationRequests.get(1).getRequestedSlot());

        // List all reservation requests and theirs compartment requests
        /*List<ReservationRequest> rrList = reservationRequestManager.list();
        for (ReservationRequest rr : rrList) {
            System.err.println(rr.toString());

            List<ReservationRequest> crList = ReservationRequestManager.listByReservationRequest(rr);
            for (ReservationRequest cr : crList) {
                System.err.println(cr.toString());
            }
        }*/
    }
}
