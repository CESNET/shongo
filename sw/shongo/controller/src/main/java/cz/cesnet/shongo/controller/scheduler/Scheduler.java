package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.ControllerConfiguration;
import cz.cesnet.shongo.controller.Reporter;
import cz.cesnet.shongo.controller.SwitchableComponent;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.booking.room.UsedRoomEndpoint;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.booking.specification.Specification;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.notification.AllocationFailedNotification;
import cz.cesnet.shongo.controller.notification.Notification;
import cz.cesnet.shongo.controller.notification.ReservationNotification;
import cz.cesnet.shongo.controller.notification.ReservationRequestNotification;
import cz.cesnet.shongo.controller.notification.manager.NotificationManager;
import cz.cesnet.shongo.controller.booking.reservation.ExistingReservation;
import cz.cesnet.shongo.controller.booking.reservation.Reservation;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import cz.cesnet.shongo.util.DateTimeFormatter;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * Represents a component of a domain controller that is responsible for allocating {@link cz.cesnet.shongo.controller.booking.request.ReservationRequest}
 * to the {@link Reservation}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Scheduler extends SwitchableComponent
        implements Component.AuthorizationAware, Component.NotificationManagerAware
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
    public void init(ControllerConfiguration configuration)
    {
        this.
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
    public Result run(Interval interval, EntityManager entityManager)
    {
        Result result = new Result();
        if (!isEnabled()) {
            logger.warn("Skipping scheduler because it is disabled...");
            return result;
        }

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.getInstance(DateTimeFormatter.Type.LONG);
        logger.debug("Running scheduler for interval '{}'...", dateTimeFormatter.formatInterval(interval));

        cz.cesnet.shongo.util.Timer timer = new cz.cesnet.shongo.util.Timer();
        timer.start();

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        ReservationManager reservationManager = new ReservationManager(entityManager);
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        try {
            // Set of notifications
            NotificationSet notifications = new NotificationSet();

            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            // Delete all reservations which should be deleted
            List<Reservation> reservationsForDeletion = reservationManager.getReservationsForDeletion();
            // Get all referenced reservations from reservations from deletion
            List<Reservation> referencedReservations = new LinkedList<Reservation>();
            for (Reservation reservationForDeletion : reservationsForDeletion) {
                getReferencedReservations(reservationForDeletion, referencedReservations);
            }
            // Move all referenced reservations to the end
            for (Reservation referencedReservation : referencedReservations) {
                Reservation topReferencedReservation = referencedReservation.getTopReservation();
                if (reservationsForDeletion.contains(topReferencedReservation)) {
                    reservationsForDeletion.remove(topReferencedReservation);
                    reservationsForDeletion.add(topReferencedReservation);
                }
            }
            for (Reservation reservation : reservationsForDeletion) {
                notifications.addNotification(reservation, ReservationNotification.Type.DELETED, authorizationManager);
                reservation.setAllocation(null);
                reservationManager.delete(reservation, authorizationManager);
                result.deletedReservations++;
            }

            // Delete all allocations which should be deleted
            for (Allocation allocation : reservationRequestManager.getAllocationsForDeletion()) {
                entityManager.remove(allocation);
            }

            // Delete all reservation requests which should be deleted
            for (ReservationRequest request : reservationRequestManager.getReservationRequestsForDeletion()) {
                reservationRequestManager.hardDelete(request, authorizationManager);
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
                    authorizationManager.beginTransaction();
                    entityManager.getTransaction().begin();

                    // Reload the request (rollback may happened)
                    reservationRequest = reservationRequestManager.getReservationRequest(reservationRequest.getId());

                    // Allocate reservation request
                    SchedulerContext schedulerContext = new SchedulerContext(
                            interval.getStart(), cache, entityManager, authorizationManager);
                    allocateReservationRequest(reservationRequest, schedulerContext, notifications);

                    // Reallocate dependent reservation requests
                    Iterator<ReservationRequest> iterator = schedulerContext.getReservationRequestsToReallocate();
                    while (iterator.hasNext()) {
                        ReservationRequest reservationRequestToReallocate = iterator.next();
                        allocateReservationRequest(reservationRequestToReallocate, schedulerContext, notifications);
                        reservationRequestToReallocate.getSpecification().updateTechnologies(entityManager);
                    }

                    // Finalize (delete old reservations, etc)
                    schedulerContext.finish();

                    entityManager.getTransaction().commit();
                    authorizationManager.commitTransaction();

                    result.allocatedReservationRequests++;
                }
                catch (Exception exception) {
                    result.failedReservationRequests++;

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
                        notifications.addNotificationWithReservationRequest(new AllocationFailedNotification(
                                reservationRequest, authorizationManager, getConfiguration()),
                                reservationRequest, authorizationManager);
                    }
                    else {
                        // Report allocation failure internal error
                        Reporter.reportInternalError(Reporter.SCHEDULER, exception);
                    }
                }
            }

            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            // Delete all executables which should be deleted
            executableManager.deleteAllNotReferenced(authorizationManager);

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();

            // Execute notifications
            if (notificationManager != null) {
                notifications.executeNotifications(notificationManager);
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

        if (!result.isEmpty()) {
            logger.info("Scheduling done in {} ms (failed: {}, allocated: {}, deleted: {}).", new Object[]{
                    timer.stop(), result.failedReservationRequests, result.allocatedReservationRequests,
                    result.deletedReservations
            });
        }
        return result;
    }

    /**
     * Fill {@link Reservation}s which are referenced (e.g., by {@link ExistingReservation})
     * from given {@code reservation} to given {@code referencedReservations}.
     *
     * @param reservation
     * @param referencedReservations
     */
    private void getReferencedReservations(Reservation reservation, List<Reservation> referencedReservations)
    {
        if (reservation instanceof ExistingReservation) {
            ExistingReservation existingReservation = (ExistingReservation) reservation;
            Reservation referencedReservation = existingReservation.getReusedReservation();
            referencedReservations.add(referencedReservation);
        }
        for (Reservation childReservation : reservation.getChildReservations()) {
            getReferencedReservations(childReservation, referencedReservations);
        }
    }

    /**
     * Allocate given {@code reservationRequest}.
     *
     * @param reservationRequest to be allocated
     * @param schedulerContext
     * @param notificationSet
     */
    private static void allocateReservationRequest(ReservationRequest reservationRequest,
            SchedulerContext schedulerContext, NotificationSet notificationSet) throws SchedulerException
    {
        logger.debug("Allocating reservation request '{}'...", reservationRequest.getId());

        EntityManager entityManager = schedulerContext.getEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        ReservationManager reservationManager = new ReservationManager(entityManager);
        AuthorizationManager authorizationManager = schedulerContext.getAuthorizationManager();

        // Initialize scheduler context
        schedulerContext.setReservationRequest(reservationRequest);

        // Find reservation requests which should be reallocated
        DateTime requestedSlotStart = schedulerContext.getRequestedSlotStart();
        List<ReservationRequest> reservationRequestUsages = reservationRequestManager.listAllocationActiveUsages(
                reservationRequest.getAllocation(), schedulerContext.getRequestedSlot());
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

        DateTime minimumDateTime = schedulerContext.getMinimumDateTime();
        Interval requestedSlot = schedulerContext.getRequestedSlot();

        // Fill already allocated reservations as reallocatable
        Allocation allocation = reservationRequest.getAllocation();
        for (Reservation allocatedReservation : allocation.getReservations()) {
            if (!requestedSlot.overlaps(allocatedReservation.getSlot())) {
                continue;
            }
            schedulerContext.addAvailableReservation(allocatedReservation, AvailableReservation.Type.REALLOCATABLE);
        }

        // Fill allocated reservation from reused reservation request as reusable
        Allocation reusedAllocation = reservationRequest.getReusedAllocation();
        Reservation reusableReservation = null;
        if (reusedAllocation != null) {
            reusableReservation = schedulerContext.setReusableAllocation(reusedAllocation);
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

        // Check mandatory reusable reservation
        if (reusableReservation != null && reservationRequest.isReusedAllocationMandatory()) {
            boolean reused = false;
            Executable reusableExecutable = reusableReservation.getExecutable();
            for (Reservation reservation : schedulerContext.getAllocatedReservations()) {
                if (reservation instanceof ExistingReservation) {
                    ExistingReservation existingReservation = (ExistingReservation) reservation;
                    if (existingReservation.getReusedReservation().equals(reusableReservation)) {
                        reused = true;
                        break;
                    }
                }
            }
            if (!reused && reusableExecutable instanceof RoomEndpoint) {
                for (Reservation reservation : schedulerContext.getAllocatedReservations()) {
                    Executable executable = reservation.getExecutable();
                    if (executable instanceof UsedRoomEndpoint) {
                        UsedRoomEndpoint usedRoomEndpoint = (UsedRoomEndpoint) executable;
                        if (usedRoomEndpoint.getReusedRoomEndpoint().equals(reusableExecutable)) {
                            reused = true;
                            break;
                        }
                    }
                }
            }
            if (!reused) {
                throw new SchedulerReportSet.ReservationWithoutMandatoryUsageException(
                        reusedAllocation.getReservationRequest());
            }
        }

        // Create allocated reservation
        boolean isNew = !allocatedReservation.isPersisted();
        if (isNew) {
            // Persist reservation
            reservationManager.create(allocatedReservation);

            // Create ACL entries for new reservation
            authorizationManager.createAclEntriesForChildEntity(reservationRequest, allocatedReservation);
        }
        else {
            // Update ACL entries for modified reservation
            authorizationManager.updateAclEntriesForChildEntities(allocatedReservation);
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
                if (oldReservation.getSlotEnd().isAfter(requestedSlotStart)) {
                    // Set preceding reservation
                    if (precedingReservation != null) {
                        throw new RuntimeException("Only one preceding reservation can exist in old reservations.");
                    }
                    precedingReservation = oldReservation;

                    // Shorten the old reservation time slot to not intersect the new reservation time slot
                    oldReservation.setSlotEnd(requestedSlotStart);
                }
                // Old reservation which takes place in the past should not be deleted
                continue;
            }

            // Create notification
            notificationSet.addNotification(oldReservation, ReservationNotification.Type.DELETED, authorizationManager);

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
        notificationSet.addNotification(allocatedReservation,
                (isNew ? ReservationNotification.Type.NEW : ReservationNotification.Type.MODIFIED),
                authorizationManager);

        // Update reservation request
        reservationRequest.setAllocationState(ReservationRequest.AllocationState.ALLOCATED);
        reservationRequest.setReports(reservationTask.getReports());
        reservationRequestManager.update(reservationRequest);
    }

    /**
     * Set of {@link Notification} for execution.
     */
    private class NotificationSet
    {
        /**
         * List of {@link Notification}.
         */
        List<Notification> notifications = new LinkedList<Notification>();

        /**
         * Map of {@link ReservationRequestNotification} by {@link cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest}.
         */
        Map<Long, ReservationRequestNotification> reservationRequestNotifications =
                new HashMap<Long, ReservationRequestNotification>();

        /**
         * @param notification to be added to the {@link #notifications}
         */
        public void addNotification(Notification notification)
        {
            notifications.add(notification);
        }

        /**
         * Add new {@link ReservationNotification} to the {@link #notifications}.
         *
         * @param reservation
         * @param type
         * @param authorizationManager
         */
        public void addNotification(Reservation reservation, ReservationNotification.Type type,
                AuthorizationManager authorizationManager)
        {
            // Get reservation request for reservation
            Allocation allocation = reservation.getAllocation();
            AbstractReservationRequest abstractReservationRequest =
                    (allocation != null ? allocation.getReservationRequest() : null);

            // Create reservation notification
            ReservationNotification notification = new ReservationNotification(
                    type, reservation, abstractReservationRequest, authorizationManager, getConfiguration());

            // Get reservation request notification
            if (abstractReservationRequest != null) {
                // Add reservation notification as normal and add it also to reservation request notification
                addNotificationWithReservationRequest(notification, abstractReservationRequest, authorizationManager);
            }
            else {
                // Add reservation notification as normal
                addNotification(notification);
            }
        }

        private void addNotificationWithReservationRequest(Notification notification,
                AbstractReservationRequest abstractReservationRequest, AuthorizationManager authorizationManager)
        {
            addNotification(notification);

            // Get top reservation request
            if (abstractReservationRequest instanceof ReservationRequest) {
                ReservationRequest reservationRequest = (ReservationRequest) abstractReservationRequest;
                Allocation parentAllocation = reservationRequest.getParentAllocation();
                if (parentAllocation != null) {
                    AbstractReservationRequest parentReservationRequest = parentAllocation.getReservationRequest();
                    if (parentReservationRequest != null) {
                        abstractReservationRequest = parentReservationRequest;
                    }
                }
            }

            // Create or reuse reservation request notification
            Long abstractReservationRequestId = abstractReservationRequest.getId();
            ReservationRequestNotification reservationRequestNotification =
                    reservationRequestNotifications.get(abstractReservationRequestId);
            if (reservationRequestNotification == null) {
                reservationRequestNotification = new ReservationRequestNotification(
                        abstractReservationRequest, authorizationManager, getConfiguration());
                notifications.add(reservationRequestNotification);
                reservationRequestNotifications.put(abstractReservationRequestId, reservationRequestNotification);
            }

            // Add reservation notification to reservation request notification
            reservationRequestNotification.addNotification(notification);
        }

        /**
         * Execute {@link #notifications}.
         *
         * @param notificationManager to be used
         */
        public void executeNotifications(NotificationManager notificationManager)
        {
            if (notifications.size() > 0) {
                if (notificationManager.hasExecutors()) {
                    logger.debug("Executing notifications...");
                    for (Notification notification : notifications) {
                        notificationManager.executeNotification(notification);
                    }
                }
            }
        }
    }

    public static class Result
    {
        private int failedReservationRequests = 0;
        private int allocatedReservationRequests = 0;
        private int deletedReservations = 0;

        public boolean isEmpty()
        {
            return failedReservationRequests == 0 &&
                    allocatedReservationRequests == 0 &&
                    deletedReservations == 0;
        }

        public int getFailedReservationRequests()
        {
            return failedReservationRequests;
        }

        public int getAllocatedReservationRequests()
        {
            return allocatedReservationRequests;
        }

        public int getDeletedReservations()
        {
            return deletedReservations;
        }
    }

}
