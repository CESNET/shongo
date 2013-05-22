package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.executor.ExecutableManager;
import cz.cesnet.shongo.controller.notification.NotificationManager;
import cz.cesnet.shongo.controller.notification.ReservationNotification;
import cz.cesnet.shongo.controller.request.Allocation;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.request.Specification;
import cz.cesnet.shongo.controller.reservation.Reservation;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import cz.cesnet.shongo.controller.scheduler.*;
import cz.cesnet.shongo.report.Report;
import org.joda.time.DateTime;
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
     * @see cz.cesnet.shongo.controller.cache.Cache
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

        // Date/time which represents now
        DateTime dateTimeNow = DateTime.now();

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        ReservationManager reservationManager = new ReservationManager(entityManager);
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager);
        try {
            // Storage for reservation notifications
            List<ReservationNotification> notifications = new LinkedList<ReservationNotification>();

            authorizationManager.beginTransaction(authorization);
            entityManager.getTransaction().begin();

            // Delete all reservations which should be deleted
            for (Reservation reservation : reservationManager.getReservationsForDeletion()) {
                notifications.add(new ReservationNotification(
                        ReservationNotification.Type.DELETED, reservation, authorizationManager));
                // Delete the reservation
                reservationManager.delete(reservation, authorizationManager);
            }

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();

            // Get all reservation requests which should be allocated
            List<ReservationRequest> reservationRequests = new ArrayList<ReservationRequest>();
            reservationRequests.addAll(reservationRequestManager.listCompletedReservationRequests(interval));

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

            // Allocate all reservation requests
            for (ReservationRequest reservationRequest : reservationRequests) {
                try {
                    authorizationManager.beginTransaction(authorization);
                    entityManager.getTransaction().begin();

                    // Reload the request (rollback may happened)
                    reservationRequest = reservationRequestManager.getReservationRequest(
                            reservationRequest.getId());

                    Reservation oldReservation = reservationRequest.getAllocation().getCurrentReservation();
                    Reservation newReservation = allocateReservationRequest(
                            reservationRequest, dateTimeNow, entityManager, authorizationManager);
                    if (oldReservation != null && oldReservation != newReservation) {
                        notifications.add(new ReservationNotification(
                                ReservationNotification.Type.DELETED, oldReservation, authorizationManager));
                    }
                    if (newReservation != null) {
                        if (newReservation == oldReservation) {
                            notifications.add(new ReservationNotification(
                                    ReservationNotification.Type.MODIFIED, newReservation, authorizationManager));
                            // Update ACL records for modified reservation
                            authorizationManager.updateAclRecordsForChildEntities(newReservation);
                        }
                        else {
                            notifications.add(new ReservationNotification(
                                    ReservationNotification.Type.NEW, newReservation, authorizationManager));
                            // Create ACL records for new reservation
                            authorizationManager.createAclRecordsForChildEntity(reservationRequest, newReservation);
                        }
                    }
                    entityManager.getTransaction().commit();
                    authorizationManager.commitTransaction();
                }
                catch (Exception exception) {
                    // Allocation of reservation request has failed and thus rollback transaction
                    if (authorizationManager.isTransactionActive()) {
                        authorizationManager.rollbackTransaction();
                    }
                    if (entityManager.getTransaction().isActive()) {
                        entityManager.getTransaction().rollback();
                    }

                    entityManager.getTransaction().begin();

                    // Because rollback has happened we must reload the entity
                    reservationRequest = reservationRequestManager.getReservationRequest(
                            reservationRequest.getId());

                    // Update reservation request state to failed
                    reservationRequest.setState(ReservationRequest.State.ALLOCATION_FAILED);
                    reservationRequest.clearReports();
                    if (exception instanceof SchedulerException) {
                        SchedulerException schedulerException = (SchedulerException) exception;
                        SchedulerReport report = schedulerException.getTopReport();
                        reservationRequest.addReport(report);
                    }

                    entityManager.getTransaction().commit();

                    if (exception instanceof SchedulerException) {
                        // Report allocation failure to domain admin
                        SchedulerException schedulerException = (SchedulerException) exception;
                        SchedulerReport report = schedulerException.getTopReport();
                        Reporter.reportAllocationFailed(reservationRequest,
                                report.getMessageRecursive(Report.MessageType.DOMAIN_ADMIN));
                    }
                    else {
                        // Report allocation failure internal error
                        Reporter.reportInternalError(Reporter.SCHEDULER, exception);
                    }
                }
            }

            authorizationManager.beginTransaction(authorization);
            entityManager.getTransaction().begin();

            // Delete all executables which should be deleted
            executableManager.deleteAllNotReferenced(authorizationManager);

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();

            // Notify about reservations
            if (notificationManager != null && notifications.size() > 0) {
                if (notificationManager.hasExecutors()) {
                    logger.debug("Notifying about changes in reservations...");
                    for (ReservationNotification reservationNotification : notifications) {
                        notificationManager.executeNotification(reservationNotification);
                    }
                }
            }
        }
        catch (Exception exception) {
            if (authorizationManager.isTransactionActive()) {
                authorizationManager.rollbackTransaction();
            }
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            Reporter.reportInternalError(Reporter.SCHEDULER, exception);
        }
    }

    /**
     * Allocate given {@code reservationRequest}.
     *
     * @param reservationRequest to be allocated
     * @param dateTimeNow
     * @param entityManager
     */
    private Reservation allocateReservationRequest(ReservationRequest reservationRequest, DateTime dateTimeNow,
            EntityManager entityManager, AuthorizationManager authorizationManager) throws SchedulerException
    {
        // Create scheduler task context
        SchedulerContext schedulerContext = new SchedulerContext(reservationRequest, cache, dateTimeNow, entityManager);
        Interval requestedSlot = schedulerContext.getRequestedSlot();

        logger.info("Allocating reservation request '{}'...", reservationRequest.getId());

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        ReservationManager reservationManager = new ReservationManager(entityManager);

        // Fill provided reservations as reusable
        for (Reservation providedReservation : reservationRequest.getProvidedReservations()) {
            if (!schedulerContext.isReservationAvailable(providedReservation)) {
                throw new SchedulerReportSet.ReservationNotAvailableException(providedReservation);
            }
            if (!providedReservation.getSlot().contains(requestedSlot)) {
                throw new SchedulerReportSet.ReservationNotUsableException(providedReservation);
            }
            schedulerContext.addAvailableReservation(providedReservation, AvailableReservation.Type.REUSABLE);
        }

        // Fill already allocated reservations as reallocatable
        Allocation allocation = reservationRequest.getAllocation();
        for (Reservation allocatedReservation : allocation.getReservations()) {
            if (!requestedSlot.overlaps(allocatedReservation.getSlot())) {
                continue;
            }
            schedulerContext.addAvailableReservation(allocatedReservation, AvailableReservation.Type.REALLOCATABLE);
        }

        // Get reservation task
        Specification specification = reservationRequest.getSpecification();
        ReservationTask reservationTask;
        if (specification instanceof ReservationTaskProvider) {
            ReservationTaskProvider reservationTaskProvider = (ReservationTaskProvider) specification;
            reservationTask = reservationTaskProvider.createReservationTask(schedulerContext);
        }
        else {
            throw new SchedulerReportSet.SpecificationNotAllocatableException(specification);
        }

        // Allocate reservation
        Reservation reservation = reservationTask.perform();
        if (!reservation.isPersisted()) {
            reservationManager.create(reservation);
        }

        // Add new allocated reservation
        allocation.addReservation(reservation);

        // Update/delete old allocated reservations
        for (AvailableReservation availableReservation : schedulerContext.getParentAvailableReservations()) {
            if (!availableReservation.isType(AvailableReservation.Type.REALLOCATABLE)) {
                continue;
            }
            Reservation allocatedReservation = availableReservation.getOriginalReservation();
            // Keep only reservations which takes place before new reservation
            if (allocatedReservation.getSlotStart().isBefore(requestedSlot.getStart())) {
                // Reservation time slot should be updated to not intersect the new reservation time slot
                if (allocatedReservation.getSlotEnd().isAfter(requestedSlot.getStart())) {
                    // Shorten the reservation time slot
                    reservationManager.updateReservationSlotEnd(allocatedReservation, requestedSlot.getStart());
                }
                // Reservation should not be deleted
                continue;
            }

            // Remove the reservation from allocation
            allocation.removeReservation(allocatedReservation);
            // Delete the reservation
            reservationManager.delete(allocatedReservation, authorizationManager);
        }

        // Update reservation request
        reservationRequest.setState(ReservationRequest.State.ALLOCATED);
        reservationRequest.setReports(reservationTask.getReports());
        reservationRequestManager.update(reservationRequest);

        return reservation;
    }
}
