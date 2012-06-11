package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.common.*;
import cz.cesnet.shongo.controller.AbstractDatabaseTest;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.resource.Technology;
import org.junit.Test;

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
        // Load reservation database
        ReservationDatabase reservationDatabase = new ReservationDatabase(new Domain("cz.cesnet"), entityManager);

        // Create request
        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setPurpose(ReservationRequest.Purpose.SCIENCE);
        reservationRequest.addRequestedSlot(new AbsoluteDateTime("2012-06-01T15:00"), new Period("PT1H"));
        reservationRequest.addRequestedSlot(new PeriodicDateTime(
                new AbsoluteDateTime("2012-07-01T14:00"), new Period("P1W"), new AbsoluteDateTime("2012-07-31")),
                new Period("PT2H"));
        Compartment requestCompartment = reservationRequest.addRequestedCompartment();
        requestCompartment.addRequestedResource(new ExternalEndpointSpecification(Technology.SIP));
        ResourceSpecification resourceSpecification = new ExternalEndpointSpecification(Technology.H323);
        resourceSpecification.addRequestedPerson(new Person("Martin Srom", "martin.srom@cesnet.cz"));
        requestCompartment.addRequestedResource(resourceSpecification);

        // Add request to database
        reservationDatabase.addReservationRequest(reservationRequest);

        // Check created compartments
        List<CompartmentRequest> compartmentRequestList =
                reservationDatabase.listCompartmentRequests(reservationRequest.getIdentifier());
        assertEquals(6, compartmentRequestList.size());
        assertEquals(new AbsoluteDateTimeSlot(new AbsoluteDateTime("2012-06-01T15:00"), new Period("PT1H")),
                compartmentRequestList.get(0).getRequestedSlot());
        assertEquals(new AbsoluteDateTimeSlot(new AbsoluteDateTime("2012-07-22T14:00"), new Period("PT2H")),
                compartmentRequestList.get(4).getRequestedSlot());

        // List all reservation requests and theirs compartment requests
        List<ReservationRequest> rrList = reservationDatabase.listReservationRequests();
        for (ReservationRequest rr : rrList) {
            System.err.println(rr.toString());

            List<CompartmentRequest> crList = reservationDatabase.listCompartmentRequests(
                    rr.getIdentifier());
            for (CompartmentRequest cr : crList ) {
                System.err.println(cr.toString());
            }
        }
    }
}
