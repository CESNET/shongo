package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.domain.Domain;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.executable.ExecutableManager;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.booking.reservation.*;
import cz.cesnet.shongo.controller.booking.resource.ResourceManager;
import cz.cesnet.shongo.controller.booking.resource.ResourceReservation;
import cz.cesnet.shongo.controller.booking.room.RoomEndpoint;
import cz.cesnet.shongo.controller.booking.room.UsedRoomEndpoint;
import cz.cesnet.shongo.controller.booking.specification.Specification;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.calendar.CalendarManager;
import cz.cesnet.shongo.controller.calendar.ReservationCalendar;
import cz.cesnet.shongo.controller.domains.InterDomainAgent;
import cz.cesnet.shongo.controller.notification.*;
import cz.cesnet.shongo.util.DateTimeFormatter;
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
public class Scheduler extends SwitchableComponent implements Component.AuthorizationAware
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
     * @see CalendarManager
     */
    private CalendarManager calendarManager;

    /**
     * @see Authorization
     */
    private Authorization authorization;

    private Set<String> modifiedResources = new HashSet<>();

    /**
     * Constructor.
     *
     * @param cache               sets the {@link #cache}
     * @param notificationManager sets the {@link #notificationManager}
     */
    public Scheduler(Cache cache, NotificationManager notificationManager, CalendarManager calendarManager)
    {
        this.cache = cache;
        this.notificationManager = notificationManager;
        this.calendarManager = calendarManager;
    }

    @Override
    public void setAuthorization(Authorization authorization)
    {
        this.authorization = authorization;
    }

    @Override
    public void init(ControllerConfiguration configuration)
    {
        this.checkDependency(cache, Cache.class);
        super.init(configuration);
    }

    /**
     * Allocate reservation without bypassEntityManager.
     */
    public Result run(Interval interval, EntityManager entityManager)
    {
        return run(interval, entityManager, null);
    }

    /**
     * Allocate reservation requests which intersects given {@code interval}. Reservations are allocated in given
     * {@code interval} or more in future (and thus not before given {@code interval}).
     *
     * @param interval      only reservation requests which intersects this interval should be allocated
     * @param entityManager to be used
     * @param bypassEntityManager to be used when persisting entity while error
     */
    public Result run(Interval interval, EntityManager entityManager, EntityManager bypassEntityManager)
    {
        Result result = new Result();
        if (!isEnabled()) {
            logger.warn("Skipping scheduler because it is disabled...");
            return result;
        }

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.getInstance(DateTimeFormatter.Type.LONG);
        logger.debug("Running scheduler for interval '{}'...", dateTimeFormatter.formatInterval(interval));
        logger.debug("Start of scheduler in time: " + DateTime.now());

        cz.cesnet.shongo.util.Timer timer = new cz.cesnet.shongo.util.Timer();
        timer.start();

        DateTime start = interval.getStart();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        ReservationManager reservationManager = new ReservationManager(entityManager);
        ExecutableManager executableManager = new ExecutableManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            List<AbstractNotification> reservationNotifications = new ArrayList<>();
            List<Allocation> allocationForDeletion = reservationManager.getAllocationsReservationsForDeletion();

            List<Allocation> referencedAllocations = new LinkedList<>();
            for (Allocation allocation : allocationForDeletion) {
                getReferencedAllocations(allocation, referencedAllocations);
            }
            // Move all referenced allocations to the end
            for (Allocation allocation : referencedAllocations) {
                if (allocationForDeletion.contains(allocation)) {
                    allocationForDeletion.remove(allocation);
                    allocationForDeletion.add(allocation);
                }
            }
            for (Allocation allocation : allocationForDeletion) {
                List<Reservation> reservations = new LinkedList<>(allocation.getReservations());
                for (Reservation reservation : reservations) {
                    DeallocateReservationTask deallocateTask = DeallocateReservationTaskProvider.create(reservation);
                    try {
                        List<AbstractNotification> notifications = deallocateTask.perform(interval, result, entityManager, reservationManager, authorizationManager);
                        reservationNotifications.addAll(notifications);

                        // Notify foreign domain, that created this reservation about the deletion (if unexpected).
                        //TODO: specify domain in the reservation directly
                        if (false && !UserInformation.isLocal(reservation.getUserId())) {
                            String reservationRequestId = ObjectIdentifier.formatId(reservation.getReservationRequest());
                            Long domainId = UserInformation.parseDomainId(reservation.getUserId());
                            ResourceManager resourceManager = new ResourceManager(entityManager);
                            Domain domain = resourceManager.getDomain(domainId);

                            String message = "TODO: Reservation deleted unexpectedly (possible maintenance reasons).";
                            InterDomainAgent.getInstance().getConnector().notifyDomain(domain.toApi(), reservationRequestId, message);
                        }
                        //Add deleted reservation to calendarManager
                        if (calendarManager != null) {
                            if (reservation instanceof ResourceReservation) {
                                calendarManager.addCalendar(new ReservationCalendar.Deleted(reservation), entityManager);
                            }
                        }
                        recordModifiedReservationId(reservation);
                    } catch (ForeignDomainConnectException e) {
                        // When deallocate of foreign reservation fails, try again next time
                        //TODO: delay for some time
                        logger.error("Deallocation of foreign reservation has failed", e);
                    } catch (TodoImplementException e) {
                        // Skip deletion for now
                    }

                }
                // Delete allocation if it has no {@link ReservationRequest} linked (for foreign reservations for example).
                if (Allocation.State.DELETED.equals(allocation.getState()) && allocation.getReservationRequest() == null) {
                    if (!allocation.getReservations().isEmpty() || !allocation.getChildReservationRequests().isEmpty()) {
                        throw new TodoImplementException();
                    }
                    entityManager.remove(allocation);
                }
            }

            for (Reservation reservation : reservationManager.getOrphanReservationsForDeletion()) {
                DeallocateReservationTask deallocateTask = DeallocateReservationTaskProvider.create(reservation);
                deallocateTask.perform(interval, result, entityManager, reservationManager, authorizationManager);
                recordModifiedReservationId(reservation);
                if (calendarManager != null) {
                    if (reservation instanceof ResourceReservation) {
                        calendarManager.addCalendar(new ReservationCalendar.Deleted(reservation), entityManager);
                    }
                }
            }


            // Delete all reservation requests which should be deleted
            for (ReservationRequest request : reservationRequestManager.getOrphanReservationRequestsForDeletion()) {
                List<Reservation> detachedReservations = reservationRequestManager.hardDelete(request, authorizationManager);
                Allocation parentAllocation = request.getParentAllocation();
                // Add detached reservations to parent allocation, when the allocation is not deleted
                // (e.g. when modifying from ReservationRequestSet to ReservationRequest)
                if (parentAllocation != null && Allocation.State.ACTIVE_WITHOUT_CHILD_RESERVATION_REQUESTS.equals(parentAllocation.getState())) {
                    for (Reservation reservation : detachedReservations) {
                        parentAllocation.addReservation(reservation);
                    }
                }
            }

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction(null);
            removeModifiedReservationsFromCache();

            // Add reservation notifications
            if (notificationManager != null) {
                notificationManager.addNotifications(reservationNotifications, entityManager);
            }


            // Get all reservation requests which should be allocated
            ReservationRequestQueue reservationRequestQueue = new ReservationRequestQueue();
            reservationRequestQueue.add(reservationRequestManager.listCompletedReservationRequests(interval));

            // Allocate all reservation requests
            while (!reservationRequestQueue.isEmpty()) {
                ReservationRequest reservationRequest = reservationRequestQueue.pop();
                SchedulerReport reallocationReport = null;
                try {
                    authorizationManager.beginTransaction();
                    entityManager.getTransaction().begin();

                    // Reload the request (rollback may happened)
                    reservationRequest = reservationRequestManager.getReservationRequest(reservationRequest.getId());

                    // Allocate reservation request
                    SchedulerContext context = new SchedulerContext(start, cache, entityManager, authorizationManager, bypassEntityManager);
                    SchedulerContextState contextState = context.getState();
                    allocateReservationRequest(reservationRequest, context);

                    // Try to reallocate reservation requests
                    Iterator<ReservationRequest> tryReallocateIterator = contextState.getTryReallocationIterator();
                    contextState.enableNotifications(false);
                    while (tryReallocateIterator.hasNext()) {
                        ReservationRequest reservationRequestToReallocate = tryReallocateIterator.next();
                        reallocationReport = reservationRequest.addReport(
                                new SchedulerReportSet.ReallocatingReservationRequestReport(
                                        ObjectIdentifier.formatId(reservationRequestToReallocate)));
                        allocateReservationRequest(reservationRequestToReallocate, context);
                        reservationRequestToReallocate.getSpecification().updateTechnologies(entityManager);

                        reservationRequestToReallocate.getSpecification().updateSpecificationSummary(entityManager, false);
                    }
                    contextState.enableNotifications(true);

                    // Force to reallocate reservation requests
                    List<ReservationRequest> forceReallocation = contextState.getForceReallocation();
                    for (ReservationRequest reservationRequestToReallocate : forceReallocation) {
                        Allocation allocation = reservationRequestToReallocate.getAllocation();
                        Reservation reservation = allocation.getCurrentReservation();
                        while (reservation != null) {
                            deleteReservation(reservation, context);
                            recordModifiedReservationId(reservation);
                            reservation = allocation.getCurrentReservation();
                        }
                    }
                    reservationRequestQueue.add(forceReallocation);

                    // Finalize (delete old reservations, etc)
                    List<AbstractNotification> contextNotifications = context.finish(result);

                    entityManager.getTransaction().commit();
                    authorizationManager.commitTransaction(null);

                    removeModifiedReservationsFromCache();


                    // Add context notifications
                    if (notificationManager != null) {
                        notificationManager.addNotifications(contextNotifications, entityManager);
                    }

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
                    Long reservationRequestId = reservationRequest.getId();
                    if (reallocationReport != null) {
                        // We must keep allocation reports
                        List<SchedulerReport> schedulerReports =
                                reservationRequestManager.detachReports(reservationRequest);
                        reservationRequest = reservationRequestManager.getReservationRequest(reservationRequestId);
                        reservationRequest.setReports(schedulerReports);
                        reallocationReport = schedulerReports.get(schedulerReports.size() - 1);
                    }
                    else {
                        reservationRequest = reservationRequestManager.getReservationRequest(reservationRequestId);
                    }

                    // Update reservation request state to failed
                    reservationRequest.setAllocationState(ReservationRequest.AllocationState.ALLOCATION_FAILED);

                    // Add scheduler report
                    if (exception instanceof SchedulerException) {
                        SchedulerException schedulerException = (SchedulerException) exception;
                        SchedulerReport schedulerReport = schedulerException.getTopReport();
                        if (reallocationReport != null) {
                            reallocationReport.addChildReport(schedulerReport);
                        }
                        else {
                            reservationRequest.setReport(schedulerReport);
                        }
                    }

                    entityManager.getTransaction().commit();

                    if (exception instanceof SchedulerException) {
                        // Notify users/admins only if local
                        if (notificationManager != null && UserInformation.isLocal(reservationRequest.getUpdatedBy())) {
                            notificationManager.addNotification(new AllocationFailedNotification(
                                    reservationRequest, authorizationManager, configuration), entityManager);
                        }
                    }
                    else {
                        // Report allocation failure internal error
                        Reporter.getInstance().reportInternalError(Reporter.SCHEDULER, exception);
                    }
                }
            }

            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            // Delete all executables which should be deleted
            executableManager.deleteAllNotReferenced(authorizationManager);

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction(null);
        }
        catch (Exception exception) {
            if (authorizationManager.isTransactionActive()) {
                authorizationManager.rollbackTransaction();
            }
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            Reporter.getInstance().reportInternalError(Reporter.SCHEDULER, exception);
        }

        if (!result.isEmpty()) {
            logger.info("Scheduling done in {} ms (failed: {}, allocated: {}, deleted: {}).", new Object[]{
                    timer.stop(), result.failedReservationRequests, result.allocatedReservationRequests,
                    result.deletedReservations
            });
            logger.debug("End of scheduler in time: " + DateTime.now());
        }

        modifiedResources.clear();

        return result;
    }

    /**
     * Allocate given {@code reservationRequest}.
     *
     * @param reservationRequest to be allocated
     * @param context
     */
    private void allocateReservationRequest(ReservationRequest reservationRequest, SchedulerContext context)
            throws SchedulerException
    {
        logger.info("Allocating reservation request '{}'...", reservationRequest.getId());

        EntityManager entityManager = context.getEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        ReservationManager reservationManager = new ReservationManager(entityManager);
        AuthorizationManager authorizationManager = context.getAuthorizationManager();

        // Initialize scheduler context
        SchedulerContextState contextState = context.getState();
        contextState.clearReferencedResources();
        context.setReservationRequest(reservationRequest);
        // Set wanted reservation request state
        context.setRequestWantedState(ReservationRequest.AllocationState.ALLOCATED);

        // Get slot
        DateTime minimumDateTime = context.getMinimumDateTime();
        Interval slot = reservationRequest.getSlot();
        if (slot.isBefore(minimumDateTime)) {
            throw new IllegalArgumentException("Requested slot can't entirely belong to history.");
        }
        // Update slot to not allocate reservation before minimum date/time
        if (slot.contains(minimumDateTime)) {
            slot = new Interval(minimumDateTime, slot.getEnd());
        }
        DateTime slotStart = slot.getStart();

        // Find reservation requests which should be reallocated
        List<ReservationRequest> reservationRequestUsages =
                reservationRequestManager.listAllocationActiveUsages(reservationRequest.getAllocation(), slot);
        for (ReservationRequest reservationRequestUsage : reservationRequestUsages) {
            Interval usageSlot = reservationRequestUsage.getSlot();
            DateTime usageSlotEnd = usageSlot.getEnd();
            // If usage is active (it starts before currently being allocate requested slot)
            if (usageSlot.getStart().isBefore(slotStart) && slotStart.isBefore(usageSlotEnd)) {
                // Move currently being allocated requested slot after the usage
                slotStart = usageSlotEnd;
            }
            else {
                contextState.tryReservationRequestReallocation(reservationRequestUsage);
            }
        }
        // Update requested slot start to be after active usages
        if (slotStart.isBefore(minimumDateTime)) {
            throw new IllegalArgumentException("Requested slot can't start before minimum date/time.");
        }
        slot = new Interval(slotStart, slot.getEnd());

        // Fill already allocated reservations as reallocatable
        Specification specification = reservationRequest.getSpecification();
        Interval allocationSlot = slot;
        if (specification instanceof SpecificationIntervalUpdater) {
            SpecificationIntervalUpdater intervalUpdater = (SpecificationIntervalUpdater) specification;
            allocationSlot = intervalUpdater.updateInterval(allocationSlot, minimumDateTime);
        }
        slotStart = allocationSlot.getStart();
        Allocation allocation = reservationRequest.getAllocation();

        for (Reservation allocatedReservation : allocation.getReservations()) {
            if (!allocationSlot.overlaps(allocatedReservation.getSlot())) {
                continue;
            }
            contextState.addAvailableReservation(allocatedReservation, AvailableReservation.Type.REALLOCATABLE);
        }

        // Fill allocated reservation from reused reservation request as reusable
        Allocation reusedAllocation = reservationRequest.getReusedAllocation();
        Reservation reusableReservation = null;
        if (reusedAllocation != null) {
            reusableReservation = context.setReusableAllocation(reusedAllocation, slot);
        }

        // Get reservation task
        ReservationTask reservationTask;
        if (specification instanceof ReservationTaskProvider) {
            ReservationTaskProvider reservationTaskProvider = (ReservationTaskProvider) specification;
            reservationTask = reservationTaskProvider.createReservationTask(
                    context, slot);
        }
        else {
            throw new SchedulerReportSet.SpecificationNotAllocatableException(specification);
        }

        // Prepare reservation request notification as the first notification for allocation
        if (notificationManager != null) {
            AbstractReservationRequest parentReservationRequest = reservationRequest;
            Allocation parentAllocation = reservationRequest.getParentAllocation();
            if (parentAllocation != null) {
                parentReservationRequest = parentAllocation.getReservationRequest();
            }
            notificationManager.getReservationRequestNotification(parentReservationRequest, entityManager);
        }

        // Allocate reservation
        Reservation allocatedReservation = reservationTask.perform(allocation.getCurrentReservation());
        recordModifiedReservationId(allocatedReservation);

        // Check mandatory reusable reservation
        if (reusableReservation != null && reservationRequest.isReusedAllocationMandatory()) {
            boolean reused = false;
            Executable reusableExecutable = reusableReservation.getExecutable();
            for (Reservation reservation : contextState.getAllocatedReservations()) {
                if (reservation instanceof ExistingReservation) {
                    ExistingReservation existingReservation = (ExistingReservation) reservation;
                    if (existingReservation.getReusedReservation().equals(reusableReservation)) {
                        reused = true;
                        break;
                    }
                }
            }
            if (!reused && reusableExecutable instanceof RoomEndpoint) {
                for (Reservation reservation : contextState.getAllocatedReservations()) {
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
        if (!allocatedReservation.isPersisted()) {
            allocatedReservation.setUserId(reservationRequest.getCreatedBy());
            reservationManager.create(allocatedReservation);
        }
        else {
            reservationManager.update(allocatedReservation);
        }

        // Create ACL entries for new reservation
        authorizationManager.createAclEntriesForChildEntity(reservationRequest, allocatedReservation);

        // Get set of all new reservations
        Set<Reservation> newReservations = allocatedReservation.getSetOfAllReservations();

        // Get list of old allocated reservations
        Collection<Reservation> oldReservations = new LinkedList<Reservation>();
        oldReservations.addAll(allocation.getReservations());

        // Remove/update/delete old allocated reservations
        Reservation previousReservation = null;
        for (Reservation oldReservation : oldReservations) {
            // If old reservation has been reallocated to a new reservation
            // NOTE: Currently always FALSE because Scheduler can't reallocate reservations
            if (newReservations.contains(oldReservation)) {
                // Remove reallocated reservation from allocation (it will be re-added be new reservation)
                allocation.removeReservation(oldReservation);
                // Reallocated reservation should not be deleted
                continue;
            }

            // Assign last old existing reservation into previousReservation variable
            if (previousReservation == null
                    || oldReservation.getSlotEnd().isAfter(previousReservation.getSlotEnd())
                    || allocatedReservation.getSlot().overlaps(oldReservation.getSlot())) {
                previousReservation = oldReservation;
            }

            // If old reservation is in history (takes place before minimum date/time slot or has started executable)
            if (oldReservation.isHistory(minimumDateTime)) {
                // If old reservation time slot intersects the new reservation time slot
                if (oldReservation.getSlotEnd().isAfter(slotStart)) {
                    // Shorten the old reservation time slot to not intersect the new reservation time slot
                    oldReservation.setSlotEnd(Temporal.max(slotStart, oldReservation.getSlotStart()));
                    // Finalize reservation
                    contextState.addNotifications(finalizeActiveReservation(oldReservation, entityManager));
                }
            }
            // Else old reservation is not active (takes place in the future and doesn't have started executable)
            else {
                // Old reservation should be deleted
                deleteReservation(oldReservation, context);
            }
        }

        // Add new allocated reservation
        allocation.addReservation(allocatedReservation);

        // Allocate migration
        if (previousReservation != null && previousReservation.getClass().equals(allocatedReservation.getClass())) {
            reservationTask.migrateReservation(previousReservation, allocatedReservation, entityManager);
            if (previousReservation.getExecutable() != null) {
                previousReservation.getExecutable().updateExecutableSummary(entityManager, false);
            }
        }

        //Add new reservation to calendarManager
        if (calendarManager != null) {
            if (allocatedReservation instanceof ResourceReservation) {
                calendarManager.addCalendar(new ReservationCalendar.New(allocatedReservation, authorizationManager), entityManager);
            }
        }

        // Create notification
        contextState.addNotification(new ReservationNotification.New(
                allocatedReservation, previousReservation, authorizationManager));

        // Update reservation request
        if (context.getRequestWantedState() != null) {
            reservationRequest.setAllocationState(context.getRequestWantedState());
        }
        reservationRequest.setReports(reservationTask.getReports());
        reservationRequestManager.update(reservationRequest);
    }

    /**
     * Stores resourceId on which the reservation was modified.
     *
     * @param reservation
     */
    private void recordModifiedReservationId(Reservation reservation)
    {
        if (reservation instanceof ResourceReservation) {
            String id = ObjectIdentifier.formatId(ObjectType.RESOURCE, ((ResourceReservation) reservation).getResource().getId().toString());
            modifiedResources.add(id);
        } else {
            //Implementation for RoomResrvation
        }
    }

    private void removeModifiedReservationsFromCache ()
    {
        for (String resourceId : modifiedResources) {
            cache.removeICalReservation(resourceId);
        }
    }


    /**
     * @param reservation to be deleted in given {@code schedulerContext}
     * @param schedulerContext in which it should be deleted
     */
    private void deleteReservation(Reservation reservation, SchedulerContext schedulerContext)
    {
        SchedulerContextState schedulerContextState = schedulerContext.getState();
        EntityManager entityManager = schedulerContext.getEntityManager();
        AuthorizationManager authorizationManager = schedulerContext.getAuthorizationManager();

        // Finalize reservation
        schedulerContextState.addNotifications(finalizeActiveReservation(reservation, entityManager));

        // Create notification
        //schedulerContextState.addNotification(new ReservationNotification.Deleted(reservation, authorizationManager));

        // Remove the old reservation from allocation
        Allocation allocation = reservation.getAllocation();
        allocation.removeReservation(reservation);

        //Add deleted reservation to calendarManager
        if (calendarManager != null) {
            if (reservation instanceof ResourceReservation) {
                calendarManager.addCalendar(new ReservationCalendar.Deleted(reservation), entityManager);
            }
        }
        // Delete the old reservation
        schedulerContextState.addReservationToDelete(reservation);


    }

    /**
     * @param reservation   to be finalized
     * @param entityManager which can be used
     * @return list of {@link AbstractNotification}
     */
    private List<AbstractNotification> finalizeActiveReservation(Reservation reservation, EntityManager entityManager)
    {
        Collection<Reservation> reservationItems = new LinkedList<Reservation>();
        ReservationManager.getAllReservations(reservation, reservationItems);
        List<AbstractNotification> notifications = new LinkedList<AbstractNotification>();
        for (Reservation reservationItem : reservationItems) {
            Executable executable = reservationItem.getExecutable();
            if (executable instanceof RoomEndpoint) {
                RoomEndpoint roomEndpoint = (RoomEndpoint) reservationItem.getExecutable();
                if (roomEndpoint.isParticipantNotificationEnabled()) {
                    notifications.add(new RoomNotification.RoomDeleted(roomEndpoint, entityManager));
                }
            }
        }
        return notifications;
    }

    /**
     * Sets {@code referencedAllocations} by given {@code allocation} (recursively)
     * @param allocation
     * @param referencedAllocations
     */
    private void getReferencedAllocations(Allocation allocation, List<Allocation> referencedAllocations)
    {
        for (Reservation reservation : allocation.getReservations()) {
            getReferencedAllocations(reservation, referencedAllocations);
        }
    }

    /**
     * Sets {@code referencedAllocations} by given {@code reservation} (recursively)
     *
     * @param reservation
     * @param referencedAllocations
     */
    private void getReferencedAllocations(Reservation reservation, List<Allocation> referencedAllocations)
    {
        if (reservation instanceof ExistingReservation) {
            ExistingReservation existingReservation = (ExistingReservation) reservation;
            Reservation referencedReservation = existingReservation.getReusedReservation();
            if (referencedReservation.getAllocation() != null) {
                referencedAllocations.add(referencedReservation.getAllocation());
            }
            else {
                throw new TodoImplementException("Reservation without allocation");
            }
        }
        for (Reservation childReservation : reservation.getChildReservations()) {
            getReferencedAllocations(childReservation, referencedAllocations);
        }
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
     * {@link Scheduler} result.
     */
    public static class Result
    {
        /**
         * Number of reservation requests which have failed to allocate.
         */
        int failedReservationRequests = 0;

        /**
         * Number of reservation requests which have been successfully allocated.
         */
        int allocatedReservationRequests = 0;

        /**
         * Number of reservations which have been deleted.
         */
        int deletedReservations = 0;

        /**
         * @return true whether no reservation request has failed or has been allocated and not reservation has been deleted,
         *         false otherwise
         */
        public boolean isEmpty()
        {
            return failedReservationRequests == 0 &&
                    allocatedReservationRequests == 0 &&
                    deletedReservations == 0;
        }

        /**
         * @return {@link #failedReservationRequests}
         */
        public int getFailedReservationRequests()
        {
            return failedReservationRequests;
        }

        /**
         * @return {@link #allocatedReservationRequests}
         */
        public int getAllocatedReservationRequests()
        {
            return allocatedReservationRequests;
        }

        /**
         * @return {@link #deletedReservations}
         */
        public int getDeletedReservations()
        {
            return deletedReservations;
        }
    }

    /**
     * Queue of {@link ReservationRequest}s for {@link Scheduler}.
     */
    private static class ReservationRequestQueue
    {
        /**
         * List of {@link ReservationRequest}s.
         */
        private List<ReservationRequest> reservationRequests = new LinkedList<ReservationRequest>();

        /**
         * @param reservationRequests to be added to the {@link #reservationRequests}
         */
        public void add(Collection<ReservationRequest> reservationRequests)
        {
            this.reservationRequests.addAll(reservationRequests);
            sort();
        }

        /**
         * Sort reservation request queue by priority, purpose and created date/time.
         */
        private void sort()
        {
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
        }

        /**
         * @return {@link #reservationRequests#iterator()}
         */
        public Iterator<ReservationRequest> iterator()
        {
            return reservationRequests.iterator();
        }

        /**
         * @return {@link #reservationRequests#isEmpty()}
         */
        public boolean isEmpty()
        {
            return reservationRequests.isEmpty();
        }

        /**
         * @return next {@link ReservationRequest} from {@link #reservationRequests}
         */
        public ReservationRequest pop()
        {
            Iterator<ReservationRequest> iterator = reservationRequests.iterator();
            ReservationRequest reservationRequest = iterator.next();
            iterator.remove();
            return reservationRequest;
        }
    }
}
