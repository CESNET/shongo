package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.common.AbsoluteDateTime;
import cz.cesnet.shongo.common.DateTime;
import cz.cesnet.shongo.common.DateTimeSlot;
import cz.cesnet.shongo.common.Period;
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
        // Fill reservation database
        ReservationDatabase reservationDatabase = new ReservationDatabase(entityManager);

        ReservationRequest request = new ReservationRequest();
        request.createNewIdentifier("cz.cesnet");
        request.setType(ReservationRequest.Type.DEFAULT);
        request.addRequestedSlot(new DateTimeSlot(
                new AbsoluteDateTime("2012-06-01T15:00"), new Period("PT1H")));
        reservationDatabase.addReservationRequest(request);
        Compartment requestCompartment = request.addRequestedCompartment();
        requestCompartment.addRequestedResource(new ExternalEndpointSpecification(Technology.H323));

        // Load stored reservaiton database and list requests
        reservationDatabase = new ReservationDatabase(entityManager);
        List<ReservationRequest> reservationRequestList = reservationDatabase.listReservationRequests();
        for (ReservationRequest reservationRequest : reservationRequestList) {
            System.err.println(reservationRequest.toString());
        }
    }
}
