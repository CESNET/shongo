package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.TransactionHelper;
import cz.cesnet.shongo.controller.executor.ExecutableManager;
import cz.cesnet.shongo.controller.notification.NotificationManager;
import cz.cesnet.shongo.controller.report.ReportException;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.request.Specification;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTaskProvider;
import cz.cesnet.shongo.controller.scheduler.report.ProvidedReservationNotAvailableReport;
import cz.cesnet.shongo.controller.scheduler.report.ProvidedReservationNotUsableReport;
import cz.cesnet.shongo.controller.scheduler.report.SpecificationNotAllocatableReport;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import cz.cesnet.shongo.util.TemporalHelper;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * Represents a component of a domain controller that is responsible for allocating {@link ReservationRequest}
 * to the {@link Reservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Scheduler extends Component implements Component.NotificationManagerAware
{
    private static Logger logger = LoggerFactory.getLogger(Scheduler.class);

    /**
     * @see Cache
     */
    private Cache cache;

    /**
     * @see NotificationManager
     */
    private NotificationManager notificationManager;

    /**
     * @param cache sets the {@link #cache}
     */
    public void setCache(Cache cache)
    {
        this.cache = cache;
    }

    @Override
    public void setNotificationManager(NotificationManager notificationManager)
    {
        this.notificationManager = notificationManager;
    }

    @Override
    public void init(Configuration configuration)
    {
        checkDependency(cache, Cache.class);
        super.init(configuration);
    }

    /**
     * Run scheduler for a given interval.
     *
     * @param interval
     */
    public void run(Interval interval, EntityManager entityManager)
    {
        logger.info("Running scheduler for interval '{}'...", TemporalHelper.formatInterval(interval));

        // Set current interval as working to the cache (it will reload allocations only when
        // the interval changes)
        cache.setWorkingInterval(interval, entityManager);

        ReservationManager reservationManager = new ReservationManager(entityManager);
        ExecutableManager executableManager = new ExecutableManager(entityManager);

        TransactionHelper.Transaction transaction = TransactionHelper.beginTransaction(entityManager);

        try {
            Set<Reservation> newReservations = new HashSet<Reservation>();
            Set<Reservation> modifiedReservations = new HashSet<Reservation>();
            Set<Reservation> deletedReservations = new HashSet<Reservation>();

            // Get all reservations which should be deleted, and store theirs reservation request
            Map<Reservation, AbstractReservationRequest> toDeleteReservations =
                    new HashMap<Reservation, AbstractReservationRequest>();
            for (Reservation reservation : reservationManager.getReservationsForDeletion()) {
                toDeleteReservations.put(reservation, reservation.getReservationRequest());
            }

            // Get all reservation requests which should be allocated
            ReservationRequestManager compartmentRequestManager = new ReservationRequestManager(entityManager);
            List<ReservationRequest> reservationRequests = new ArrayList<ReservationRequest>();
            reservationRequests.addAll(compartmentRequestManager.listCompletedReservationRequests(interval));

            // Sort reservation requests by theirs priority, purpose and created date/time
            Collections.sort(reservationRequests, new Comparator<ReservationRequest>()
            {
                @Override
                public int compare(ReservationRequest reservationRequest1, ReservationRequest reservationRequest2)
                {
                    int result = -reservationRequest1.getPriority().compareTo(reservationRequest2.getPriority());
                    if (result == 0) {
                        result = reservationRequest1.getPurpose().priorityCompareTo(reservationRequest2.getPurpose());
                        if (result == 0) {
                            return reservationRequest1.getCreated().compareTo(reservationRequest2.getCreated());
                        }
                    }
                    return result;
                }
            });

            // Keep track of old reservations for reservation requests (for determination of modified reservations)
            Map<AbstractReservationRequest, Reservation> oldReservations =
                    new HashMap<AbstractReservationRequest, Reservation>();

            // Delete all reservations which should be deleted
            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            for (Reservation reservation : toDeleteReservations.keySet()) {
                AbstractReservationRequest reservationRequest = toDeleteReservations.get(reservation);
                if (reservationRequest != null) {
                    reservationRequest.removeReservation(reservation);
                    reservationRequestManager.update(reservationRequest);
                }
                reservationManager.delete(reservation, cache);

                oldReservations.put(reservationRequest, reservation);
            }

            // Allocate all reservation requests
            for (ReservationRequest reservationRequest : reservationRequests) {
                Reservation oldReservation = oldReservations.get(reservationRequest);
                Reservation newReservation = allocateReservationRequest(reservationRequest, entityManager);
                if (oldReservation != null) {
                    oldReservations.remove(reservationRequest);
                    if (newReservation != null) {
                        modifiedReservations.add(newReservation);
                    }
                    else {
                        deletedReservations.add(oldReservation);
                    }
                }
                else if (newReservation != null) {
                    newReservations.add(newReservation);
                }
            }

            // All remaining old reservation must be considered as deleted
            for (Reservation reservation : oldReservations.values()) {
                deletedReservations.add(reservation);
            }

            // Notify about new reservations
            if (notificationManager != null) {
                notificationManager.notifyReservations(newReservations, modifiedReservations, deletedReservations,
                        entityManager);
            }

            // Delete all executables which should be deleted
            executableManager.deleteAllNotReferenced();

            transaction.commit();
        }
        catch (Exception exception) {
            transaction.rollback();
            try {
                cache.reset();
            }
            catch (Exception resettingException) {
                logger.error("Cache resetting failed", resettingException);
            }
            throw new IllegalStateException("Scheduler failed", exception);
        }
    }

    /**
     * Allocate given {@code reservationRequest}.
     *
     * @param reservationRequest to be allocated
     */
    private Reservation allocateReservationRequest(ReservationRequest reservationRequest, EntityManager entityManager)
    {
        logger.info("Allocating reservation request '{}'...", reservationRequest.getId());

        reservationRequest.clearReports();

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        ReservationManager reservationManager = new ReservationManager(entityManager);

        // Get existing reservation
        Reservation reservation = reservationRequest.getReservation();

        // Old reservation exists
        if (reservation != null) {
            // TODO: Try to intelligently reallocate and not delete old reservation
            throw new TodoImplementException("Reallocate reservation");
        }

        // Get requested slot and check it's maximum duration
        Interval slot = reservationRequest.getSlot();

        // Create new scheduler task
        ReservationTask.Context context = new ReservationTask.Context(reservationRequest, cache, slot);
        ReservationTask reservationTask = null;

        try {
            // Fill provided reservations to transaction
            for (Reservation providedReservation : reservationRequest.getProvidedReservations()) {
                if (!context.getCache().isProvidedReservationAvailable(providedReservation, slot)) {
                    throw new ProvidedReservationNotAvailableReport(providedReservation).exception();
                }
                if (!providedReservation.getSlot().contains(slot)) {
                    throw new ProvidedReservationNotUsableReport(providedReservation).exception();
                }
                context.getCacheTransaction().addProvidedReservation(providedReservation);
            }

            // Get reservation task
            Specification specification = reservationRequest.getSpecification();
            if (specification instanceof ReservationTaskProvider) {
                ReservationTaskProvider reservationTaskProvider = (ReservationTaskProvider) specification;
                reservationTask = reservationTaskProvider.createReservationTask(context);
            }
            else {
                throw new SpecificationNotAllocatableReport(specification).exception();
            }

            reservation = reservationTask.perform();
            reservationManager.create(reservation);

            // Update cache
            cache.addReservation(reservation);

            // Update reservation request
            reservationRequest.setReservation(reservation);
            reservationRequest.setState(ReservationRequest.State.ALLOCATED);
            reservationRequest.setReports(reservationTask.getReports());
            reservationRequestManager.update(reservationRequest);
        }
        catch (ReportException exception) {
            reservationRequest.setState(ReservationRequest.State.ALLOCATION_FAILED);
            reservationRequest.addReport(exception.getTopReport());
        }

        return reservation;
    }

    /**
     * Run scheduler on given entityManagerFactory and interval.
     *
     * @param entityManager
     * @param interval
     */
    public static void createAndRun(Interval interval, EntityManager entityManager, Cache cache,
            NotificationManager notificationManager) throws FaultException
    {
        Scheduler scheduler = new Scheduler();
        scheduler.setCache(cache);
        scheduler.setNotificationManager(notificationManager);
        scheduler.init();
        scheduler.run(interval, entityManager);
        scheduler.destroy();
    }
}
