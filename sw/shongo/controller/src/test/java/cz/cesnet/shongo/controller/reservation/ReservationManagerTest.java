package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.controller.AbstractDatabaseTest;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.request.ReservationRequestSet;
import org.junit.Test;

import javax.persistence.EntityManager;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for {@link ReservationManager}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationManagerTest extends AbstractDatabaseTest
{
    @Test
    public void testQueryNotInIdentifierBug() throws Exception
    {
        EntityManager entityManager = getEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        ReservationManager reservationManager = new ReservationManager(entityManager);

        ReservationRequestSet reservationRequestSet = new ReservationRequestSet();
        reservationRequestSet.setName("test");

        ReservationRequest reservationRequest1 = new ReservationRequest();
        reservationRequestSet.addReservationRequest(reservationRequest1);

        ReservationRequest reservationRequest2 = new ReservationRequest();
        reservationRequestSet.addReservationRequest(reservationRequest2);

        reservationRequestManager.create(reservationRequestSet);

        ReservationRequest reservationRequest3 = new ReservationRequest();
        reservationRequestManager.create(reservationRequest3);

        Reservation reservation1 = new ResourceReservation();
        reservationManager.create(reservation1);

        // Select reservations which aren't referenced by any reservation requests (should be 1)
        List<Reservation> reservations = entityManager.createQuery(
                "SELECT reservation FROM Reservation reservation"
                        + " WHERE reservation.parentReservation IS NULL AND reservation NOT IN ("
                        + " SELECT reservationRequest.reservation FROM ReservationRequest reservationRequest)",
                Reservation.class)
                .getResultList();
        assertEquals(1, reservations.size());

        // The following query is almost same as the previous one but it uses "reservation.id" instead of "reservation"
        // And it causes a bug
        reservations = entityManager.createQuery(
                "SELECT reservation FROM Reservation reservation"
                        + " WHERE reservation.parentReservation IS NULL AND reservation.id NOT IN ("
                        + " SELECT reservationRequest.reservation.id FROM ReservationRequest reservationRequest"
                        + ")",
                Reservation.class)
                .getResultList();
        // Bug, should be 1
        assertEquals(0, reservations.size());

        // So in code we must use entity alias in "NOT IN" clause and not identifiers

        entityManager.close();
    }
}
