package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.executor.ExecutableManager;
import cz.cesnet.shongo.controller.notification.NotificationManager;
import cz.cesnet.shongo.controller.notification.ReservationNotification;
import cz.cesnet.shongo.controller.request.*;
import cz.cesnet.shongo.controller.reservation.ExistingReservation;
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
     * Allocate reservation requests which intersects given {@code interval}. Reservations are allocated in given
     * {@code interval} or more in future (and thus not before given {@code interval}).
     *
     * @param interval      only reservation requests which intersects this interval should be allocated
     * @param entityManager to be used
     */
    public void run(Interval interval, EntityManager entityManager)
    {
        logger.debug("Running scheduler for interval '{}'...", Temporal.formatInterval(interval));

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        ReservationManager reservationManager = new ReservationManager(entityManager);
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        SchedulerContext schedulerContext = new SchedulerContext(cache, entityManager, interval.getStart());
        AuthorizationManager authorizationManager = schedulerContext.getAuthorizationManager();
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
                            return reservationRequest1.getCreatedAt().compareTo(reservationRequest2.getCreatedAt());
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

                    allocateReservationRequest(reservationRequest, schedulerContext, notifications);

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
                    reservationRequest.setAllocationState(ReservationRequest.AllocationState.ALLOCATION_FAILED);
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
     * @param interval
     * @param providedReservationRequest
     * @param entityManager
     * @return {@link Reservation} to be provided from given {@code providedReservationRequest} for given {@code interval}
     * @throws SchedulerException
     */
    public static Reservation getProvidedReservation(Interval interval,
            AbstractReservationRequest providedReservationRequest, EntityManager entityManager, SchedulerContext schedulerContext)
            throws SchedulerException
    {
        // Only reservation request can be provided
        if (!(providedReservationRequest instanceof ReservationRequest)) {
            throw new SchedulerReportSet.ReservationRequestNotUsableException(providedReservationRequest);
        }
        ReservationRequest reservationRequest = (ReservationRequest) providedReservationRequest;

        // Find provided reservation
        Reservation providedReservation = null;
        for (Reservation reservation : reservationRequest.getAllocation().getReservations()) {
            if (reservation.getSlot().contains(interval)) {
                providedReservation = reservation;
                break;
            }
        }
        if (providedReservation == null) {
            throw new SchedulerReportSet.ReservationRequestNotUsableException(reservationRequest);
        }

        // Check the provided reservation
        ReservationManager reservationManager = new ReservationManager(entityManager);
        List<ExistingReservation> existingReservations =
                reservationManager.getExistingReservations(providedReservation, interval);
        if (schedulerContext != null) {
            schedulerContext.applyAvailableReservations(existingReservations);
        }
        if (existingReservations.size() > 0) {
            throw new SchedulerReportSet.ReservationNotAvailableException(
                    providedReservation, reservationRequest);
        }
        return providedReservation;
    }

    /**
     * Allocate given {@code reservationRequest}.
     *
     * @param reservationRequest to be allocated
     * @param schedulerContext
     * @param notifications
     */
    private void allocateReservationRequest(ReservationRequest reservationRequest,
            SchedulerContext schedulerContext, List<ReservationNotification> notifications) throws SchedulerException
    {
        // Create scheduler task context
        schedulerContext.clear();
        schedulerContext.setReservationRequest(reservationRequest);

        logger.info("Allocating reservation request '{}'...", reservationRequest.getId());

        DateTime minimumDateTime = schedulerContext.getMinimumDateTime();
        Interval requestedSlot = schedulerContext.getRequestedSlot();

        EntityManager entityManager = schedulerContext.getEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        ReservationManager reservationManager = new ReservationManager(entityManager);
        AuthorizationManager authorizationManager = schedulerContext.getAuthorizationManager();

        // Fill already allocated reservations as reallocatable
        Allocation allocation = reservationRequest.getAllocation();
        for (Reservation allocatedReservation : allocation.getReservations()) {
            if (!requestedSlot.overlaps(allocatedReservation.getSlot())) {
                continue;
            }
            schedulerContext.addAvailableReservation(allocatedReservation, AvailableReservation.Type.REALLOCATABLE);
        }

        // Fill allocated reservation from provided reservation request as reusable
        ReservationRequest providedReservationRequest = reservationRequest.getProvidedReservationRequest();
        if (providedReservationRequest != null) {
            Reservation providedReservation = getProvidedReservation(
                    requestedSlot, providedReservationRequest, entityManager, schedulerContext);
            schedulerContext.addAvailableReservation(providedReservation, AvailableReservation.Type.REUSABLE);
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
        Reservation allocatedReservation = reservationTask.perform();
        allocatedReservation.setUserId(reservationRequest.getUpdatedBy());

        // Create allocated reservation
        boolean isNew = !allocatedReservation.isPersisted();
        if (isNew) {
            // Persist reservation
            reservationManager.create(allocatedReservation);

            // Create ACL records for new reservation
            authorizationManager.createAclRecordsForChildEntity(reservationRequest, allocatedReservation);
        }
        else {
            // Update ACL records for modified reservation
            authorizationManager.updateAclRecordsForChildEntities(allocatedReservation);
        }

        // Get set of all new reservations
        Set<Reservation> newReservations = allocatedReservation.getSetOfAllReservations();

        // Get list of old allocated reservations
        Collection<Reservation> oldReservations = new LinkedList<Reservation>();
        oldReservations.addAll(allocation.getReservations());

        // Remove/update/delete old allocated reservations
        Reservation precedingReservation = null;
        for (Reservation oldReservation : oldReservations) {
            // If old reservation has been reallocated to a new reservation
            if (newReservations.contains(oldReservation)) {
                // Remove reallocated reservation from allocation (it will be re-added be new reservation)
                allocation.removeReservation(oldReservation);
                // Reallocated reservation should not be deleted
                continue;
            }

            // If old reservation takes place before minimum date/time slot (i.e., in the past and before the new reservation)
            if (oldReservation.getSlotStart().isBefore(minimumDateTime)) {
                // If old reservation time slot intersects the new reservation time slot
                if (oldReservation.getSlotEnd().isAfter(requestedSlot.getStart())) {
                    // Set preceding reservation
                    if (precedingReservation != null) {
                        throw new RuntimeException("Only one preceding reservation can exist in old reservations.");
                    }
                    precedingReservation = oldReservation;

                    // Shorten the old reservation time slot to not intersect the new reservation time slot
                    reservationManager.updateReservationSlotEnd(oldReservation, requestedSlot.getStart());
                }
                // Old reservation which takes place in the past should not be deleted
                continue;
            }

            // Create notification
            notifications.add(new ReservationNotification(
                    ReservationNotification.Type.DELETED, oldReservation, authorizationManager));

            // Remove the old reservation from allocation
            allocation.removeReservation(oldReservation);
            // Delete the old reservation
            reservationManager.delete(oldReservation, authorizationManager);
        }

        // Add new allocated reservation
        allocation.addReservation(allocatedReservation);

        // Allocate migration
        if (precedingReservation != null && precedingReservation.getClass().equals(allocatedReservation.getClass())) {
            reservationTask.migrateReservation(precedingReservation, allocatedReservation);
        }

        // Create notification
        notifications.add(new ReservationNotification(
                (isNew ? ReservationNotification.Type.NEW : ReservationNotification.Type.MODIFIED),
                allocatedReservation, authorizationManager));

        // Update reservation request
        reservationRequest.setAllocationState(ReservationRequest.AllocationState.ALLOCATED);
        reservationRequest.setReports(reservationTask.getReports());
        reservationRequestManager.update(reservationRequest);
    }
}
