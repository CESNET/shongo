package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.common.*;
import cz.cesnet.shongo.controller.AbstractDatabaseTest;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.resource.Technology;
import cz.cesnet.shongo.controller.scheduler.Scheduler;
import org.junit.Test;

import java.nio.charset.CoderMalfunctionError;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * Reservation database test.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationDatabaseTest extends AbstractDatabaseTest
{
    @Test
    public void test() throws Exception
    {
        Scheduler scheduler = new Scheduler(entityManager);

        // Load reservation database
        ReservationDatabase reservationDatabase = new ReservationDatabase(entityManager, new Domain("cz.cesnet"),
                scheduler);

        // Create reservation request
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setPurpose(ReservationRequest.Purpose.SCIENCE);
        reservationRequest.addRequestedSlot(new AbsoluteDateTime("2012-06-01T15:00"), new Period("PT1H"));
        reservationRequest.addRequestedSlot(new PeriodicDateTime(
                new AbsoluteDateTime("2012-07-01T14:00"), new Period("P1W"), new AbsoluteDateTime("2012-07-15")),
                new Period("PT2H"));
        // First compartment
        Compartment requestCompartment = reservationRequest.addRequestedCompartment();
        requestCompartment.addRequestedResource(new ExternalEndpointSpecification(Technology.SIP));
        ResourceSpecification resourceSpecification = new ExternalEndpointSpecification(Technology.H323);
        resourceSpecification.addRequestedPerson(new Person("Martin Srom", "martin.srom@cesnet.cz"));
        requestCompartment.addRequestedResource(resourceSpecification);
        // Second compartment
        requestCompartment = reservationRequest.addRequestedCompartment();
        requestCompartment.addRequestedResource(new ExternalEndpointSpecification(Technology.ADOBE_CONNECT, 2));

        // Add request to database
        reservationDatabase.addReservationRequest(reservationRequest);

        // Check created compartments
        CompartmentRequestManager compartmentRequestManager = new CompartmentRequestManager(entityManager, scheduler);
        List<CompartmentRequest> compartmentRequestList =
                compartmentRequestManager.list(reservationRequest);
        assertEquals(8, compartmentRequestList.size());
        assertEquals(new AbsoluteDateTimeSlot(new AbsoluteDateTime("2012-06-01T15:00"), new Period("PT1H")),
                compartmentRequestList.get(0).getRequestedSlot());
        assertEquals(new AbsoluteDateTimeSlot(new AbsoluteDateTime("2012-07-15T14:00"), new Period("PT2H")),
                compartmentRequestList.get(3).getRequestedSlot());

        // Modify reservation request
        reservationRequest.setPurpose(ReservationRequest.Purpose.EDUCATION);
        reservationRequest.removeRequestedSlot(reservationRequest.getRequestedSlots().get(0));
        reservationRequest.removeRequestedCompartment(reservationRequest.getRequestedCompartments().get(1));

        // Update request
        reservationDatabase.updateReservationRequest(reservationRequest);

        // Check modified compartments
        compartmentRequestList = compartmentRequestManager.list(reservationRequest);
        assertEquals(3, compartmentRequestList.size());
        assertEquals(new AbsoluteDateTimeSlot(new AbsoluteDateTime("2012-07-01T14:00"), new Period("PT2H")),
                compartmentRequestList.get(0).getRequestedSlot());
        assertEquals(new AbsoluteDateTimeSlot(new AbsoluteDateTime("2012-07-15T14:00"), new Period("PT2H")),
                compartmentRequestList.get(2).getRequestedSlot());

        // List all reservation requests and theirs compartment requests
        List<ReservationRequest> rrList = reservationDatabase.listReservationRequests();
        for (ReservationRequest rr : rrList) {
            System.err.println(rr.toString());

            List<CompartmentRequest> crList = compartmentRequestManager.list(rr);
            for (CompartmentRequest cr : crList ) {
                System.err.println(cr.toString());
            }
        }
    }
}
