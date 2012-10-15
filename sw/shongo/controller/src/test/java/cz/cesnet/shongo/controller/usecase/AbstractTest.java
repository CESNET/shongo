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
import cz.cesnet.shongo.fault.FaultException;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * Tests for allocation of {@link ReservationRequest} or {@link ReservationRequestSet}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractTest extends AbstractDatabaseTest
{
    /**
     * Create {@link ReservationRequest} in the database, allocate it to {@link Reservation}.
     *
     * @param reservationRequest
     * @param cache
     * @throws Exception
     */
    private void process(ReservationRequest reservationRequest, Cache cache, EntityManager entityManager)
            throws FaultException
    {
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        if (!reservationRequest.isPersisted()) {
            reservationRequestManager.create(reservationRequest);
        }
        else {
            reservationRequest.clearState();
            reservationRequest.updateStateBySpecifications();
            reservationRequestManager.update(reservationRequest);
        }

        Interval interval = Interval.parse("0/9999");

        EntityManager entityManagerForScheduler = getEntityManager();
        Scheduler.createAndRun(interval, entityManagerForScheduler, cache);
        entityManagerForScheduler.close();
    }

    /**
     * Create {@link ReservationRequest} in the database, allocate it to {@link Reservation} and check if it succeeded.
     *
     * @param reservationRequest
     * @param cache
     * @return {@link Reservation}
     * @throws Exception
     */
    protected Reservation checkSuccessfulAllocation(ReservationRequest reservationRequest, Cache cache,
            EntityManager entityManager) throws Exception
    {
        process(reservationRequest, cache, entityManager);

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

        return reservations.get(0);
    }


    /**
     * Create {@link ReservationRequest} in the database, try allocated it and check that the allocation has failed.
     *
     * @param reservationRequest
     * @param cache
     * @throws Exception
     */
    protected void checkFailedAllocation(ReservationRequest reservationRequest, Cache cache,
            EntityManager entityManager) throws Exception
    {
        process(reservationRequest, cache, entityManager);

        entityManager.refresh(reservationRequest);

        ReservationManager reservationManager = new ReservationManager(entityManager);
        List<Reservation> reservations = reservationManager.listByReservationRequest(reservationRequest);
        assertEquals("Reservation request should be in ALLOCATION_FAILED state.",
                ReservationRequest.State.ALLOCATION_FAILED, reservationRequest.getState());
        assertEquals("No reservation should be allocated for the reservation request.", 0, reservations.size());
    }

    /**
     * Create {@link ReservationRequestSet} in the database, allocate it to {@link Reservation} and
     * check if it succeeded.
     *
     * @param reservationRequestSet
     * @param cache
     * @return {@link Reservation}
     * @throws Exception
     */
    protected Reservation checkSuccessfulAllocation(ReservationRequestSet reservationRequestSet, Cache cache,
            EntityManager entityManager) throws Exception
    {
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

        return reservations.get(0);
    }
}
