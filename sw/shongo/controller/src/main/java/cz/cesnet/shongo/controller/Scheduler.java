package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.executor.ExecutableManager;
import cz.cesnet.shongo.controller.notification.NotificationManager;
import cz.cesnet.shongo.controller.notification.ReservationNotification;
import cz.cesnet.shongo.controller.request.*;
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

                    // Allocate reservation request
                    SchedulerContext schedulerContext = new SchedulerContext(
                            interval.getStart(), cache, entityManager, authorizationManager);
                    allocateReservationRequest(reservationRequest, schedulerContext, notifications);

                    // Reallocate dependent reservation requests
                    Iterator<ReservationRequest> iterator = schedulerContext.getReservationRequestsToReallocate();
                    while (iterator.hasNext()) {
                        ReservationRequest reservationRequestToReallocate = iterator.next();
                        allocateReservationRequest(reservationRequestToReallocate, schedulerContext, notifications);
                    }

                    // Finalize (delete old reservations, etc)
                    schedulerContext.finish();

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
     * Allocate given {@code reservationRequest}.
     *
     * @param reservationRequest to be allocated
     * @param schedulerContext
     * @param notifications
     */
    private static void allocateReservationRequest(ReservationRequest reservationRequest,
            SchedulerContext schedulerContext, List<ReservationNotification> notifications) throws SchedulerException
    {
        // Initialize scheduler context
        schedulerContext.setReservationRequest(reservationRequest);

        logger.info("Allocating reservation request '{}'...", reservationRequest.getId());

        DateTime minimumDateTime = schedulerContext.getMinimumDateTime();
        Interval requestedSlot = schedulerContext.getRequestedSlot();
        DateTime requestedSlotStart = requestedSlot.getStart();

        EntityManager entityManager = schedulerContext.getEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        ReservationManager reservationManager = new ReservationManager(entityManager);
        AuthorizationManager authorizationManager = schedulerContext.getAuthorizationManager();

        // Find reservation requests which should be reallocated
        List<ReservationRequest> reservationRequestUsages = reservationRequestManager.listAllocationUsages(
                reservationRequest.getAllocation(), requestedSlot);
        for (ReservationRequest reservationRequestUsage : reservationRequestUsages) {
            Interval usageSlot = reservationRequestUsage.getSlot();
            DateTime usageSlotEnd = usageSlot.getEnd();
            // If usage is active (it starts before currently being allocate requested slot)
            if (usageSlot.getStart().isBefore(requestedSlotStart) && requestedSlotStart.isBefore(usageSlotEnd)) {
                // Move currently being allocated requested slot after the usage
                requestedSlotStart = usageSlotEnd;
            }
            else {
                schedulerContext.addReservationRequestToReallocate(reservationRequestUsage);
            }
        }
        // Update requested slot start to be after active usages
        schedulerContext.setRequestedSlotStart(requestedSlotStart);

        // Fill already allocated reservations as reallocatable
        Allocation allocation = reservationRequest.getAllocation();
        for (Reservation allocatedReservation : allocation.getReservations()) {
            if (!requestedSlot.overlaps(allocatedReservation.getSlot())) {
                continue;
            }
            schedulerContext.addAvailableReservation(allocatedReservation, AvailableReservation.Type.REALLOCATABLE);
        }

        // Fill allocated reservation from provided reservation request as reusable
        Allocation providedAllocation = reservationRequest.getProvidedAllocation();
        if (providedAllocation != null) {
            Reservation providedReservation = schedulerContext.getProvidedReservation(providedAllocation);
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
                    oldReservation.setSlotEnd(requestedSlot.getStart());
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
            schedulerContext.addReservationToDelete(oldReservation);
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
