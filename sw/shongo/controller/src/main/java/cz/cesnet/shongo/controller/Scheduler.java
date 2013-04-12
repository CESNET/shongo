package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.executor.ExecutableManager;
import cz.cesnet.shongo.controller.notification.NotificationManager;
import cz.cesnet.shongo.controller.notification.ReservationNotification;
import cz.cesnet.shongo.controller.report.ReportException;
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
import cz.cesnet.shongo.TodoImplementException;
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
public class Scheduler extends Component implements Component.AuthorizationAware, Component.NotificationManagerAware
{
    private static Logger logger = LoggerFactory.getLogger(Scheduler.class);

    /**
     * @see Cache
     */
    private Cache cache;

    /**
     * @see Authorization
     */
    private Authorization authorization;

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
    public void setAuthorization(Authorization authorization)
    {
        this.authorization = authorization;
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
        logger.debug("Running scheduler for interval '{}'...", Temporal.formatInterval(interval));

        // Set current interval as working to the cache (it will reload allocations only when
        // the interval changes)
        cache.setWorkingInterval(interval, entityManager);

        // Storage for reservation notifications
        Map<Long, ReservationNotification> notificationByReservationId =
                new HashMap<Long, ReservationNotification>();
        // Map from new reservations to old reservations identifiers
        Map<Reservation, Long> newReservations = new HashMap<Reservation, Long>();

        ReservationManager reservationManager = new ReservationManager(entityManager);
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager);
        try {
            authorizationManager.beginTransaction(authorization);
            entityManager.getTransaction().begin();

            // Get all reservations which should be deleted, and store theirs reservation request
            Map<Reservation, ReservationRequest> toDeleteReservations = new HashMap<Reservation, ReservationRequest>();
            for (Reservation reservation : reservationManager.getReservationsForDeletion()) {
                toDeleteReservations.put(reservation, (ReservationRequest) reservation.getReservationRequest());
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
            Map<ReservationRequest, Long> oldReservationIds = new HashMap<ReservationRequest, Long>();

            // Delete all reservations which should be deleted
            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            for (Reservation reservation : toDeleteReservations.keySet()) {
                Long reservationId = reservation.getId();

                // Add notification
                if (notificationManager != null) {
                    notificationByReservationId.put(reservationId, new ReservationNotification(
                            ReservationNotification.Type.DELETED, reservation));
                }

                // Delete the reservation and add ACL records to be deleted in the end
                ReservationRequest reservationRequest = toDeleteReservations.get(reservation);
                if (reservationRequest != null) {
                    reservationRequest.setReservation(null);
                    reservationRequestManager.update(reservationRequest);
                }

                reservationManager.delete(reservation, authorizationManager, cache);

                // Remember the old reservation for the reservation request
                oldReservationIds.put(reservationRequest, reservationId);
            }

            // Allocate all reservation requests
            for (ReservationRequest reservationRequest : reservationRequests) {
                Long oldReservationId = oldReservationIds.get(reservationRequest);
                Reservation newReservation = allocateReservationRequest(reservationRequest, entityManager);
                if (newReservation != null) {
                    newReservations.put(newReservation, oldReservationId);
                }
            }

            // Create ACL records
            for (Reservation reservation : newReservations.keySet()) {
                ReservationRequest reservationRequest = reservation.getReservationRequest();
                authorizationManager.createAclRecordsForChildEntity(reservationRequest, reservation);
            }

            // Delete all executables which should be deleted
            executableManager.deleteAllNotReferenced(authorizationManager);

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();
        }
        catch (Exception exception) {
            if (authorizationManager.isTransactionActive()) {
                authorizationManager.rollbackTransaction();
            }
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            try {
                cache.reset();
            }
            catch (Exception resettingException) {
                Reporter.reportInternalError(Reporter.InternalErrorType.SCHEDULER, "Cache resetting failed", resettingException);
            }
            Reporter.reportInternalError(Reporter.InternalErrorType.SCHEDULER, exception);
            return;
        }

        // Create new/modified reservation notifications
        if (notificationManager != null) {
            for (Map.Entry<Reservation, Long> newReservationEntry : newReservations.entrySet()) {
                Reservation newReservation = newReservationEntry.getKey();
                Long oldReservationId = newReservationEntry.getValue();
                if (oldReservationId == null) {
                    // Add notification about new reservation
                    notificationByReservationId.put(newReservation.getId(), new ReservationNotification(
                            ReservationNotification.Type.NEW, newReservation));
                }
                else {
                    // Remove notification about deleted reservation
                    notificationByReservationId.remove(oldReservationId);
                    // Add notification about modified reservation
                    notificationByReservationId.put(newReservation.getId(), new ReservationNotification(
                            ReservationNotification.Type.MODIFIED, newReservation));
                }
            }
        }

        // Notify about reservations
        if (notificationByReservationId.size() > 0) {
            if (notificationManager.hasExecutors()) {
                logger.debug("Notifying about changes in reservations...");
                for (ReservationNotification reservationNotification : notificationByReservationId.values()) {
                    notificationManager.executeNotification(reservationNotification);
                }
            }
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
}
