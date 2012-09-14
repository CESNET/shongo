package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.TransactionHelper;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import cz.cesnet.shongo.controller.api.ControllerFault;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
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
        reservationManager.deleteAllMarked(cache);

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
        Task task = new Task(requestedSlot, cache);

        throw new TodoImplementException();
        /*try {

            if (requestedSlot.toDuration().isLongerThan(cache.getAllocatedResourceMaximumDuration())) {
                throw new DurationLongerThanMaximumReport(requestedSlot.toPeriod().normalizedStandard(),
                        cache.getAllocatedResourceMaximumDuration().toPeriod().normalizedStandard()).exception();
            }

            // Get list of requested resources
            List<CompartmentRequest.RequestedResource> requestedResources =
                    compartmentRequest.getRequestedResourcesForScheduler();

            // Initialize scheduler task (by adding all requested resources to it)

            CallInitiation callInitiation = compartmentRequest.getCompartment().getCallInitiation();
            if (callInitiation != null) {
                task.setCallInitiation(callInitiation);
            }
            for (CompartmentRequest.RequestedResource requestedResource : requestedResources) {
                task.addResource(requestedResource.getResourceSpecification());
            }

            // Create new allocated compartment
            reservation = task.createReservation();
            reservation.setCompartmentRequest(compartmentRequest);

            // TODO: Add persons for allocated devices

            // Create allocated compartment
            reservationManager.create(reservation);

            // Add allocated items to the cache
            for (AllocatedItem allocatedItem : reservation.getChildReservations()) {
                cache.addAllocatedItem(allocatedItem);
            }

            // Set compartment state to allocated
            compartmentRequest.setState(CompartmentRequest.State.ALLOCATED);
            compartmentRequest.setReports(task.getReports());
            reservationRequestManager.update(compartmentRequest);
        }
        catch (ReportException exception) {
            compartmentRequest.setState(CompartmentRequest.State.ALLOCATION_FAILED);
            compartmentRequest.setReports(task.getReports());
            compartmentRequest.addReport(exception.getReport());
        }*/
    }
}
