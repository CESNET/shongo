package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.TransactionHelper;
import cz.cesnet.shongo.controller.api.ControllerFault;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.request.Specification;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTaskProvider;
import cz.cesnet.shongo.controller.scheduler.report.DurationLongerThanMaximumReport;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import cz.cesnet.shongo.util.TemporalHelper;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Represents a component of a domain controller that is responsible for allocating {@link ReservationRequest}
 * to the {@link Reservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Scheduler extends Component
{
    private static Logger logger = LoggerFactory.getLogger(Scheduler.class);

    /**
     * @see {@link Cache}
     */
    private Cache cache;

    /**
     * @param cache sets the {@link #cache}
     */
    public void setCache(Cache cache)
    {
        this.cache = cache;
    }

    /**
     * Run scheduler on given entityManagerFactory and interval.
     *
     * @param entityManager
     * @param interval
     */
    public static void createAndRun(Interval interval, EntityManager entityManager, Cache resourceDatabase)
            throws FaultException
    {
        Scheduler scheduler = new Scheduler();
        scheduler.setCache(resourceDatabase);
        scheduler.init();
        scheduler.run(interval, entityManager);
        scheduler.destroy();
    }

    /**
     * Run scheduler for a given interval.
     *
     * @param interval
     */
    public void run(Interval interval, EntityManager entityManager) throws FaultException
    {
        logger.info("Running scheduler for interval '{}'...", TemporalHelper.formatInterval(interval));

        TransactionHelper.Transaction transaction = TransactionHelper.beginTransaction(entityManager);

        // Set current interval as working to the cache (it will reload allocations only when
        // the interval changes)
        cache.setWorkingInterval(interval, entityManager);

        // Delete all reservations which was marked for deletion
        ReservationManager reservationManager = new ReservationManager(entityManager);
        reservationManager.deleteAllNotReferencedByReservationRequest(cache);

        try {
            ReservationRequestManager compartmentRequestManager = new ReservationRequestManager(entityManager);
            List<ReservationRequest> reservationRequests =
                    compartmentRequestManager.listCompletedReservationRequests(interval);

            // TODO: Process permanent first
            // TODO: Apply some other priority to compartment requests

            for (ReservationRequest reservationRequest : reservationRequests) {
                allocateReservationRequest(reservationRequest, entityManager);
            }

            transaction.commit();
        }
        catch (Exception exception) {
            transaction.rollback();
            cache.reset(entityManager);
            throw new FaultException(exception, ControllerFault.SCHEDULER_FAILED);
        }
    }

    /**
     * Allocate given {@code reservationRequest}.
     *
     * @param reservationRequest to be allocated
     */
    private void allocateReservationRequest(ReservationRequest reservationRequest, EntityManager entityManager)
    {
        logger.info("Allocating reservation request '{}'...", reservationRequest.getId());

        reservationRequest.clearReports();

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        ReservationManager reservationManager = new ReservationManager(entityManager);

        // Get existing reservation
        Reservation reservation = reservationManager.getByReservationRequest(reservationRequest);

        // TODO: Try to intelligently reallocate and not delete old reservation
        // Delete old reservation
        if (reservation != null) {
            reservationManager.delete(reservation, cache);
        }

        // Get requested slot and check it's maximum duration
        Interval requestedSlot = reservationRequest.getRequestedSlot();

        // Create new scheduler task
        ReservationTask.Context context = new ReservationTask.Context(requestedSlot, cache);
        ReservationTask reservationTask;
        Specification specification = reservationRequest.getSpecification();
        if (specification instanceof ReservationTaskProvider) {
            ReservationTaskProvider reservationTaskProvider = (ReservationTaskProvider) specification;
            reservationTask = reservationTaskProvider.createReservationTask(context);
        }
        else {
            throw new IllegalStateException(String.format("Cannot allocate reservation request '%s'"
                    + " because the specification of type '%s' is not supposed to be allocated.",
                    reservationRequest.getId(), specification.getClass().getSimpleName()));
        }

        try {
            if (requestedSlot.toDuration().isLongerThan(cache.getAllocatedResourceMaximumDuration())) {
                throw new DurationLongerThanMaximumReport(requestedSlot.toPeriod().normalizedStandard(),
                        cache.getAllocatedResourceMaximumDuration().toPeriod().normalizedStandard()).exception();
            }

            reservation = reservationTask.perform();
            // TODO: Add persons for allocated devices
            reservationManager.create(reservation);

            // Update cache
            for (Reservation childReservation : reservation.getChildReservations()) {
                throw new TodoImplementException();
                //cache.addAllocatedItem(childReservation);
            }

            // Update reservation request
            reservationRequest.setReservation(reservation);
            reservationRequest.setState(ReservationRequest.State.ALLOCATED);
            reservationRequest.setReports(reservationTask.getReports());
            reservationRequestManager.update(reservationRequest);

        }
        catch (ReportException exception) {
            reservationRequest.setState(ReservationRequest.State.ALLOCATION_FAILED);
            reservationRequest.setReports(reservationTask.getReports());
            reservationRequest.addReport(exception.getReport());
        }
    }
}
