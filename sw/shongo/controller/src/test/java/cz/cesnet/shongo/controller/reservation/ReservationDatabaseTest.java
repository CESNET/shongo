package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.common.*;
import cz.cesnet.shongo.controller.AbstractDatabaseTest;
import cz.cesnet.shongo.controller.resource.Technology;
import org.junit.Test;

import java.util.List;

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
        ReservationDatabase reservationDatabase = new ReservationDatabase(entityManager);

        // Create request
        ReservationRequest request = new ReservationRequest();
        request.createNewIdentifier("cz.cesnet");
        request.setPurpose(ReservationRequest.Purpose.SCIENCE);
        request.addRequestedSlot(new AbsoluteDateTime("2012-06-01T15:00"), new Period("PT1H"));
        request.addRequestedSlot(new PeriodicDateTime(
                new AbsoluteDateTime("2012-07-01T15:00"), new Period("P1W"), new AbsoluteDateTime("2012-07-31")),
                new Period("PT2H"));
        Compartment requestCompartment = request.addRequestedCompartment();
        requestCompartment.addRequestedResource(new ExternalEndpointSpecification(Technology.H323));
        requestCompartment.addRequestedResource(new ExternalEndpointSpecification(Technology.SIP));

        // Add request to database
        reservationDatabase.addReservationRequest(request);

        // Load stored reservation database and list requests
        reservationDatabase = new ReservationDatabase(entityManager);
        List<ReservationRequest> reservationRequestList = reservationDatabase.listReservationRequests();
        for (ReservationRequest reservationRequest : reservationRequestList) {
            System.err.println(reservationRequest.toString());
        }
    }
}
