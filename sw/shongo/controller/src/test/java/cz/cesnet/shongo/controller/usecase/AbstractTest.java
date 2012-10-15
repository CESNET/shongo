package cz.cesnet.shongo.controller.usecase;

import cz.cesnet.shongo.controller.AbstractDatabaseTest;
import cz.cesnet.shongo.controller.Cache;
import cz.cesnet.shongo.controller.Preprocessor;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.request.ReservationRequestSet;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for allocation of {@link ReservationRequest} or {@link ReservationRequestSet}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AbstractTest extends AbstractDatabaseTest
{
    /**
     * Create reservation request in the database, allocate it and check if allocation succeeds
     *
     * @param reservationRequest
     * @param cache
     * @throws Exception
     */
    protected void checkSuccessfulAllocation(ReservationRequest reservationRequest, Cache cache)
            throws Exception
    {
        EntityManager entityManager = getEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        reservationRequestManager.create(reservationRequest);

        Interval interval = Interval.parse("0/9999");

        EntityManager entityManagerForPreprocessor = getEntityManager();
        Preprocessor.createAndRun(interval, entityManagerForPreprocessor);
        entityManagerForPreprocessor.close();

        EntityManager entityManagerForScheduler = getEntityManager();
        Scheduler.createAndRun(interval, entityManagerForScheduler, cache);
        entityManagerForScheduler.close();

        entityManager.refresh(reservationRequest);

        ReservationManager reservationManager = new ReservationManager(entityManager);
        List<Reservation> reservations = reservationManager.listByReservationRequest(reservationRequest);
        if (reservations.size() == 0) {
            System.err.println(reservationRequest.getReportText());
            Thread.sleep(100);
        }
        assertEquals("Reservation request should be in ALLOCATED state.",
                ReservationRequest.State.ALLOCATED, reservationRequest.getState());
        assertEquals("Reservation should be allocated for the reservation request.", 1, reservations.size());

        entityManager.close();
    }

    /**
     * Create {@link ReservationRequestSet} in the database, allocate it and check if allocation succeeds.
     *
     * @param reservationRequestSet
     * @param cache
     * @throws Exception
     */
    protected void checkSuccessfulAllocation(ReservationRequestSet reservationRequestSet, Cache cache)
            throws Exception
    {
        EntityManager entityManager = getEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        reservationRequestManager.create(reservationRequestSet);

        Interval interval = Interval.parse("0/9999");

        EntityManager entityManagerForPreprocessor = getEntityManager();
        Preprocessor.createAndRun(interval, entityManagerForPreprocessor);
        entityManagerForPreprocessor.close();

        EntityManager entityManagerForScheduler = getEntityManager();
        Scheduler.createAndRun(interval, entityManagerForScheduler, cache);
        entityManagerForScheduler.close();

        List<ReservationRequest> reservationRequests =
                reservationRequestManager.listReservationRequestsBySet(reservationRequestSet);
        assertEquals(1, reservationRequests.size());
        ReservationRequest reservationRequest = reservationRequests.get(0);

        ReservationManager reservationManager = new ReservationManager(entityManager);
        List<Reservation> reservations = reservationManager.listByReservationRequest(reservationRequestSet);
        if (reservations.size() == 0) {
            System.err.println(reservationRequest.getReportText());
            Thread.sleep(100);
        }
        assertEquals("Reservation request should be in ALLOCATED state.",
                ReservationRequest.State.ALLOCATED, reservationRequest.getState());
        assertEquals("Reservation should be allocated for the reservation request.", 1, reservations.size());

        entityManager.close();
    }
}
