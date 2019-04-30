package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.*;
import cz.cesnet.shongo.api.ClassHelper;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.AclIdentityType;
import cz.cesnet.shongo.controller.LocalDomain;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.Reservation;
import cz.cesnet.shongo.controller.api.Specification;
import cz.cesnet.shongo.controller.api.request.*;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.datetime.PeriodicDateTime;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.request.*;
import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.reservation.*;
import cz.cesnet.shongo.controller.booking.resource.*;
import cz.cesnet.shongo.controller.booking.resource.Resource;
import cz.cesnet.shongo.controller.booking.resource.ResourceSpecification;
import cz.cesnet.shongo.controller.booking.room.RoomSpecification;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.domains.InterDomainAgent;
import cz.cesnet.shongo.controller.notification.NotificationManager;
import cz.cesnet.shongo.controller.notification.ReservationRequestConfirmationNotification;
import cz.cesnet.shongo.controller.notification.ReservationRequestDeniedNotification;
import cz.cesnet.shongo.controller.scheduler.*;
import cz.cesnet.shongo.controller.util.NativeQuery;
import cz.cesnet.shongo.controller.util.QueryFilter;
import cz.cesnet.shongo.controller.util.iCalendar;
import cz.cesnet.shongo.report.Report;
import org.joda.time.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * Implementation of {@link ReservationService}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationServiceImpl extends AbstractServiceImpl
        implements ReservationService, Component.EntityManagerFactoryAware,
                   Component.AuthorizationAware, Component.NotificationManagerAware
{
    /**
     * @see cz.cesnet.shongo.controller.cache.Cache
     */
    private Cache cache;

    /**
     * @see javax.persistence.EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see cz.cesnet.shongo.controller.authorization.Authorization
     */
    private Authorization authorization;

    /**
     * @see NotificationManager
     */
    private NotificationManager notificationManager;

    /**
     * Constructor.
     */
    public ReservationServiceImpl(Cache cache)
    {
        this.cache = cache;
    }

    @Override
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
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
        checkDependency(entityManagerFactory, EntityManagerFactory.class);
        checkDependency(authorization, Authorization.class);
        super.init(configuration);
    }

    @Override
    public String getServiceName()
    {
        return "Reservation";
    }

/*    @Override
    public Object checkAvailability(AvailabilityCheckRequest request)
    {
        checkNotNull("request", request);
        SecurityToken securityToken = request.getSecurityToken();
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        try {
            // We must check only the future (because scheduler allocates only in future)
            DateTime minimumDateTime = DateTime.now();
            Interval slot = request.getSlot();
            if (slot.getEnd().isBefore(minimumDateTime)) {
                throw new ControllerReportSet.ReservationRequestEmptyDurationException();
            }
            if (slot.getStart().isBefore(minimumDateTime)) {
                slot = slot.withStart(minimumDateTime);
            }

            Specification specificationApi = request.getSpecification();
            cz.cesnet.shongo.controller.booking.specification.Specification specification = null;
            Interval allocationSlot = slot;
            if (specificationApi != null) {
                 specification = cz.cesnet.shongo.controller.booking.specification.Specification.createFromApi(
                         specificationApi, entityManager);
                if (specification instanceof SpecificationIntervalUpdater) {
                    SpecificationIntervalUpdater intervalUpdater = (SpecificationIntervalUpdater) specification;
                    allocationSlot = intervalUpdater.updateInterval(allocationSlot, minimumDateTime);
                }
            }

            // Create scheduler context
            SchedulerContext schedulerContext = new SchedulerContext(DateTime.now(), cache, entityManager,
                    new AuthorizationManager(entityManager, authorization));
            schedulerContext.setPurpose(request.getPurpose());

            // Ignore reservations for already allocated reservation request
            SchedulerContextState schedulerContextState = schedulerContext.getState();
            String ignoredReservationRequestId = request.getIgnoredReservationRequestId();
            if (ignoredReservationRequestId != null) {
                ObjectIdentifier objectId = ObjectIdentifier.parse(
                        ignoredReservationRequestId, ObjectType.RESERVATION_REQUEST);
                cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest ignoredReservationRequest =
                        reservationRequestManager.get(objectId.getPersistenceId());
                for (cz.cesnet.shongo.controller.booking.reservation.Reservation reservation :
                        ignoredReservationRequest.getAllocation().getReservations()) {
                    if (allocationSlot.overlaps(reservation.getSlot())) {
                        schedulerContextState.addAvailableReservation(
                                reservation, AvailableReservation.Type.REALLOCATABLE);
                    }
                }

                for (cz.cesnet.shongo.controller.booking.request.ReservationRequest childReservationRequest :
                        ignoredReservationRequest.getAllocation().getChildReservationRequests()) {
                    for (cz.cesnet.shongo.controller.booking.reservation.Reservation reservation :
                            childReservationRequest.getAllocation().getReservations()) {
                        if (reservation.getSlot().overlaps(slot)) {
                            schedulerContextState.addAvailableReservation(
                                    reservation, AvailableReservation.Type.REALLOCATABLE);
                        }
                    }
                }
            }

            try {
                // Check reservation request reusability
                String reservationRequestId = request.getReservationRequestId();
                if (reservationRequestId != null) {
                    ObjectIdentifier objectId = ObjectIdentifier.parse(
                            reservationRequestId, ObjectType.RESERVATION_REQUEST);
                    cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest =
                            reservationRequestManager.get(objectId.getPersistenceId());
                    schedulerContext.setReusableAllocation(reservationRequest.getAllocation(), slot);
                }

                // Check specification availability
                if (specification != null) {
                    if (specification instanceof ReservationTaskProvider) {

                        try {
                            entityManager.getTransaction().begin();
                            ReservationTaskProvider reservationTaskProvider = (ReservationTaskProvider) specification;
                            ReservationTask reservationTask =
                                    reservationTaskProvider.createReservationTask(schedulerContext, slot);
                            reservationTask.perform();
                        }
                        finally {
                            entityManager.getTransaction().rollback();
                        }
                    }
                    else {
                        throw new SchedulerReportSet.SpecificationNotAllocatableException(specification);
                    }
                }
            }
            catch (SchedulerException exception) {
                // Specification cannot be allocated or reservation request cannot be reused in requested time slot
                return exception.getReport().toAllocationStateReport(authorization.isAdministrator(securityToken) ?
                        Report.UserType.DOMAIN_ADMIN : Report.UserType.USER);
            }

            // Request is available
            return Boolean.TRUE;
        }
        finally {
            entityManager.close();
        }
    }*/

    @Override
    public Object checkPeriodicAvailability(AvailabilityCheckRequest request)
    {
        checkNotNull("request", request);
        SecurityToken securityToken = request.getSecurityToken();
        authorization.validate(securityToken);
        // Check if local resource
        if (request.getSpecification() instanceof cz.cesnet.shongo.controller.api.ResourceSpecification) {
            cz.cesnet.shongo.controller.api.ResourceSpecification resourceSpecificationApi =
                    (cz.cesnet.shongo.controller.api.ResourceSpecification) request.getSpecification();
            if (!ObjectIdentifier.isLocal(resourceSpecificationApi.getResourceId())) {
                //TODO: check availability for foreign resources???
                return Boolean.TRUE;
            }
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        try {
            List<Interval> slots = new ArrayList<Interval>();
            for (PeriodicDateTimeSlot slot : request.getSlots()) {
                PeriodicDateTime periodicDateTime = new PeriodicDateTime(slot.getStart(), slot.getPeriod(), slot.getEnd(), slot.getPeriodicityDayOrder(), slot.getPeriodicityDayInMonth());
                periodicDateTime.setTimeZone(slot.getTimeZone());
                periodicDateTime.addAllRules(PeriodicDateTime.RuleType.DISABLE, slot.getExcludeDates());

                for (DateTime slotStart : periodicDateTime.enumerate()) {
                    slots.add(new Interval(slotStart, slot.getDuration()));
                }
            }

            boolean rollback = false;
            // For each time slot in periodic reservation request
            for (Interval slotToTest : slots) {
                // We must check only the future (because scheduler allocates only in future)
                DateTime minimumDateTime = DateTime.now();
                Interval slot = slotToTest;
                if (slot.getEnd().isBefore(minimumDateTime)) {
                    throw new ControllerReportSet.ReservationRequestEmptyDurationException();
                }
                if (slot.getStart().isBefore(minimumDateTime)) {
                    slot = slot.withStart(minimumDateTime);
                }

                Specification specificationApi = request.getSpecification();
                cz.cesnet.shongo.controller.booking.specification.Specification specification = null;
                Interval allocationSlot = slot;
                if (specificationApi != null) {
                    specification = cz.cesnet.shongo.controller.booking.specification.Specification.createFromApi(
                            specificationApi, entityManager);
                    if (specification instanceof SpecificationIntervalUpdater) {
                        SpecificationIntervalUpdater intervalUpdater = (SpecificationIntervalUpdater) specification;
                        allocationSlot = intervalUpdater.updateInterval(allocationSlot, minimumDateTime);
                    }
                }

                // Create scheduler context
                SchedulerContext schedulerContext = new SchedulerContext(DateTime.now(), cache, entityManager,
                        new AuthorizationManager(entityManager, authorization));
                schedulerContext.setUserId(securityToken.getUserId());
                schedulerContext.setPurpose(request.getPurpose());


                // Ignore reservations for already allocated reservation request
                SchedulerContextState schedulerContextState = schedulerContext.getState();
                String ignoredReservationRequestId = request.getIgnoredReservationRequestId();
                if (ignoredReservationRequestId != null) {
                    ObjectIdentifier objectId = ObjectIdentifier.parse(
                            ignoredReservationRequestId, ObjectType.RESERVATION_REQUEST);
                    cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest ignoredReservationRequest =
                            reservationRequestManager.get(objectId.getPersistenceId());
                    for (cz.cesnet.shongo.controller.booking.reservation.Reservation reservation :
                            ignoredReservationRequest.getAllocation().getReservations()) {
                        if (allocationSlot.overlaps(reservation.getSlot())) {
                            schedulerContextState.addAvailableReservation(
                                    reservation, AvailableReservation.Type.REALLOCATABLE);
                        }
                    }

                    for (cz.cesnet.shongo.controller.booking.request.ReservationRequest childReservationRequest :
                            ignoredReservationRequest.getAllocation().getChildReservationRequests()) {
                        for (cz.cesnet.shongo.controller.booking.reservation.Reservation reservation :
                                childReservationRequest.getAllocation().getReservations()) {
                            if (reservation.getSlot().overlaps(slot)) {
                                schedulerContextState.addAvailableReservation(
                                        reservation, AvailableReservation.Type.REALLOCATABLE);
                            }
                        }
                    }
                }

                try {
                    // Check reservation request reusability (TODO: check if permanent room)
                    String reservationRequestId = request.getReservationRequestId();
                    if (reservationRequestId != null) {
                        ObjectIdentifier objectId = ObjectIdentifier.parse(
                                reservationRequestId, ObjectType.RESERVATION_REQUEST);
                        cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest =
                                reservationRequestManager.get(objectId.getPersistenceId());
                        schedulerContext.setReusableAllocation(reservationRequest.getAllocation(), slot);
                    }

                    // Check specification availability
                    if (specification != null) {
                        if (specification instanceof ReservationTaskProvider) {
                            entityManager.getTransaction().begin();
                            try {
                                schedulerContext.setAvailabilityCheck(true);
                                ReservationTaskProvider reservationTaskProvider = (ReservationTaskProvider) specification;
                                ReservationTask reservationTask =
                                        reservationTaskProvider.createReservationTask(schedulerContext, slot);
                                reservationTask.perform();
                            } finally {
                                rollback = true;
                            }
                        } else {
                            throw new SchedulerReportSet.SpecificationNotAllocatableException(specification);
                        }
                    }
                }
                catch (SchedulerException exception) {
                    SchedulerReport schedulerReport = exception.getReport();

                    // Specification cannot be allocated or reservation request cannot be reused in requested time slot
                    /* TODO: PREPARED for reporting all errors at once
                    if (slotsLeft > 0) {
                        AvailabilityCheckRequest newRequest = request;
                        newRequest.setSlot(slots.get(slots.size()-slotsLeft));
                        Object availabilityCheckResult = checkPeriodicAvailability(newRequest);
                        if (!Boolean.TRUE.equals(availabilityCheckResult)) {
                            AllocationStateReport report = (AllocationStateReport) availabilityCheckResult;
                            return schedulerReport.toAllocationStateReport(report);
                        }
                    }*/
                    return schedulerReport.toAllocationStateReport(authorization.isAdministrator(securityToken) ?
                            Report.UserType.DOMAIN_ADMIN : Report.UserType.USER);
                }
                finally {
                    if (rollback) {
                        entityManager.getTransaction().rollback();
                    }
                }

            } //END OF FOR-EACH LOOP
            // Request is available
            return Boolean.TRUE;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public String createReservationRequest(SecurityToken securityToken,
            cz.cesnet.shongo.controller.api.AbstractReservationRequest reservationRequestApi)
    {
        authorization.validate(securityToken);
        checkNotNull("reservationRequest", reservationRequestApi);

        // Change user id (only root can do that)
        String userId = securityToken.getUserId();
        if (reservationRequestApi.getUserId() != null && authorization.isAdministrator(securityToken)) {
            userId = reservationRequestApi.getUserId();
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        try {
            // Check permission for reused reservation request.
            // Allows even when user doesn't have system permission {@link SystemPermission.RESERVATION}.
            String reusedReservationRequestId = reservationRequestApi.getReusedReservationRequestId();
            if (reusedReservationRequestId != null) {
                checkReusedReservationRequest(securityToken, reusedReservationRequestId, reservationRequestManager);
            }

            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest =
                    cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest.createFromApi(
                            reservationRequestApi, entityManager);

            // Check whether user can create reservation requests OR has permissions to create capacity for permanent room
            if (!authorization.hasSystemPermission(securityToken, SystemPermission.RESERVATION)) {
                boolean allowOnAcl = false;
                if (reusedReservationRequestId != null) {
                    cz.cesnet.shongo.controller.booking.specification.Specification specification;
                    specification = reservationRequest.getSpecification();
                    if (specification instanceof cz.cesnet.shongo.controller.booking.room.RoomSpecification) {
                        RoomSpecification roomSpecification = (RoomSpecification) specification;
                        if (roomSpecification.isReusedRoom()) {
                            allowOnAcl = true;
                        }
                    }
                }
                if (!allowOnAcl) {
                    ControllerReportSetHelper.throwSecurityNotAuthorizedFault("create reservation request");
                }
            }

            reservationRequest.setCreatedBy(userId);
            reservationRequest.setUpdatedBy(userId);

            reservationRequestManager.create(reservationRequest);

            reservationRequest.getSpecification().updateTechnologies(entityManager);

            authorizationManager.createAclEntry(AclIdentityType.USER, userId, reservationRequest, ObjectRole.OWNER);

            Allocation reusedAllocation = reservationRequest.getReusedAllocation();
            if (reusedAllocation != null) {
                ReservationRequest reusedReservationRequest = (ReservationRequest) reusedAllocation.getReservationRequest();
                if (reusedReservationRequest.getReusement().equals(ReservationRequestReusement.OWNED)) {
                    authorizationManager.createAclEntriesForChildEntity(reusedReservationRequest, reservationRequest);
                }
            }

            reservationRequest.getSpecification().updateSpecificationSummary(entityManager, false);

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction(securityToken);

            if (reservationRequest instanceof ReservationRequest) {
                ReservationRequest simpleReservationRequest = (ReservationRequest) reservationRequest;
                if (ReservationRequest.AllocationState.CONFIRM_AWAITING.equals(simpleReservationRequest.getAllocationState())) {
                    if (simpleReservationRequest.getSpecification() instanceof ResourceSpecification) {
                        ResourceSpecification resourceSpecification = (ResourceSpecification) simpleReservationRequest.getSpecification();
                        List<PersonInformation> recipients = resourceSpecification.getResource().getAdministrators(authorizationManager, false);
                        ReservationRequestConfirmationNotification notification;

                        notification = new ReservationRequestConfirmationNotification(simpleReservationRequest);
                        notification.addRecipients(recipients, false);

                        notificationManager.addNotification(notification, entityManager);
                    }
                    else {
                        throw new TodoImplementException("Confirmation not supported for specification type: "
                                + simpleReservationRequest.getSpecification().getClass().getSimpleName());
                    }
                }
            }

            return ObjectIdentifier.formatId(reservationRequest);
        }
        finally {
            if (authorizationManager.isTransactionActive()) {
                authorizationManager.rollbackTransaction();
            }
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public String modifyReservationRequest(SecurityToken securityToken,
            cz.cesnet.shongo.controller.api.AbstractReservationRequest reservationRequestApi)
    {
        authorization.validate(securityToken);
        checkNotNull("reservationRequest", reservationRequestApi);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        String reservationRequestId = reservationRequestApi.getId();
        ObjectIdentifier objectId = ObjectIdentifier.parse(reservationRequestId, ObjectType.RESERVATION_REQUEST);

        try {
            // Get old reservation request and check permissions and restrictions for modification
            cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest oldReservationRequest =
                    reservationRequestManager.get(objectId.getPersistenceId());
            if (!authorization.hasObjectPermission(securityToken, oldReservationRequest, ObjectPermission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("modify reservation request %s", objectId);
            }
            switch (oldReservationRequest.getState()) {
                case MODIFIED:
                    throw new ControllerReportSet.ReservationRequestAlreadyModifiedException(objectId.toId());
                case DELETED:
                    throw new ControllerReportSet.ReservationRequestDeletedException(objectId.toId());
            }
            if (!isReservationRequestModifiable(oldReservationRequest)) {
                throw new ControllerReportSet.ReservationRequestNotModifiableException(objectId.toId());
            }

            // Change user id (only root can do that)
            String userId = securityToken.getUserId();
            if (reservationRequestApi.getUserId() != null && authorization.isAdministrator(securityToken)) {
                userId = reservationRequestApi.getUserId();
            }

            // Check permission for reused reservation request
            String reusedReservationRequestId = reservationRequestApi.getReusedReservationRequestId();
            if (reusedReservationRequestId != null) {
                checkReusedReservationRequest(securityToken, reusedReservationRequestId, reservationRequestManager);
            }

            // Check if modified reservation request is of the same class
            cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest newReservationRequest;
            Class<? extends cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest> reservationRequestClass =
                    cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest
                            .getClassFromApi(reservationRequestApi.getClass());
            if (reservationRequestClass.isInstance(oldReservationRequest)) {
                // Update old detached reservation request (the changes will not be serialized to database)
                oldReservationRequest.fromApi(reservationRequestApi, entityManager);
                // Create new reservation request by cloning old reservation request
                newReservationRequest = oldReservationRequest.clone(entityManager);
            }
            else {
                // Create new reservation request
                newReservationRequest = ClassHelper.createInstanceFromClass(reservationRequestClass);
                newReservationRequest.synchronizeFrom(oldReservationRequest, entityManager);
                newReservationRequest.fromApi(reservationRequestApi, entityManager);
            }
            newReservationRequest.setCreatedBy(userId);
            newReservationRequest.setUpdatedBy(userId);

            // Revert changes to old reservation request
            entityManager.clear();

            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            oldReservationRequest = reservationRequestManager.get(objectId.getPersistenceId());
            oldReservationRequest.setUpdatedBy(userId);

            // Create new reservation request and update old reservation request
            reservationRequestManager.modify(oldReservationRequest, newReservationRequest);
            entityManager.flush();
            entityManager.refresh(newReservationRequest);

            // Update ACL entries by reused reservation requests
            Allocation oldReusedAllocation = oldReservationRequest.getReusedAllocation();
            Allocation newReusedAllocation = newReservationRequest.getReusedAllocation();
            if (oldReusedAllocation != newReusedAllocation) {
                // Remove ACL entries from old reused reservation request
                if (oldReusedAllocation != null) {
                    ReservationRequest oldReusedReservationRequest =
                            (ReservationRequest) oldReusedAllocation.getReservationRequest();
                    if (oldReusedReservationRequest.getReusement().equals(ReservationRequestReusement.OWNED)) {
                        // TODO: consider removing ACL entries from parent object
                        // But some ACL entries (at least for the user who performs modification) must be
                        // preserved
                    }
                }
                // Create ACL entries from new reused reservation request
                if (newReusedAllocation != null) {
                    ReservationRequest newReusedReservationRequest =
                            (ReservationRequest) newReusedAllocation.getReservationRequest();
                    if (newReusedReservationRequest.getReusement().equals(ReservationRequestReusement.OWNED)) {
                        authorizationManager.createAclEntriesForChildEntity(
                                newReusedReservationRequest, newReservationRequest);
                    }
                }
            }

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction(securityToken);

            return ObjectIdentifier.formatId(newReservationRequest);
        }
        finally {
            if (authorizationManager.isTransactionActive()) {
                authorizationManager.rollbackTransaction();
            }
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public Boolean confirmReservationRequest(SecurityToken securityToken, String reservationRequestId, boolean denyOthers)
    {
        authorization.validate(securityToken);
        checkNotNull("reservationRequestId", reservationRequestId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ObjectIdentifier objectId = ObjectIdentifier.parse(reservationRequestId, ObjectType.RESERVATION_REQUEST);

        ReservationRequest reservationRequest;
        String resourceId;

        try {
            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            entityManager.getTransaction().begin();

            // Get old reservation request and check permissions and restrictions for confirmation
            cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest abstractReservationRequest =
                    reservationRequestManager.get(objectId.getPersistenceId());
            if (abstractReservationRequest instanceof ReservationRequest) {
                reservationRequest = (ReservationRequest) abstractReservationRequest;
            } else {
                throw new TodoImplementException("ReservationRequestSet is unsupported for confirmation.");
            }

            if (!ReservationRequest.AllocationState.CONFIRM_AWAITING.equals(reservationRequest.getAllocationState())) {
                return Boolean.TRUE;
            }

            cz.cesnet.shongo.controller.booking.specification.Specification specification = reservationRequest.getSpecification();
            Resource resource;
            if (specification instanceof ResourceSpecification) {
                ResourceSpecification resourceSpecification = (ResourceSpecification) specification;
                resource = resourceSpecification.getResource();
                resourceId = ObjectIdentifier.formatId(resource);
            } else {
                throw new TodoImplementException("Unsupported specification type: " + specification.getClass().getSimpleName());
            }

            if (!authorization.hasObjectPermission(securityToken, resource, ObjectPermission.CONTROL_RESOURCE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("confirm reservation request %s", resource.getId());
            }

            switch (reservationRequest.getState()) {
                case MODIFIED:
                    throw new ControllerReportSet.ReservationRequestAlreadyModifiedException(objectId.toId());
                case DELETED:
                    throw new ControllerReportSet.ReservationRequestDeletedException(objectId.toId());
            }

            reservationRequest.setAllocationState(ReservationRequest.AllocationState.COMPLETE);

            reservationRequestManager.update(reservationRequest);

            entityManager.getTransaction().commit();
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }

        // Deny other reservation requests colliding with this one allowed
        if (denyOthers) {
            ReservationRequestListRequest requestListRequest = new ReservationRequestListRequest(securityToken);
            requestListRequest.setInterval(reservationRequest.getSlot());
            requestListRequest.setIntervalDateOnly(false);
            requestListRequest.setAllocationState(AllocationState.CONFIRM_AWAITING);
            requestListRequest.setSpecificationResourceId(resourceId);

            ListResponse<ReservationRequestSummary> listResponse = listOwnedResourcesReservationRequests(requestListRequest);
            try {
                entityManager = entityManagerFactory.createEntityManager();
                ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
                ResourceManager resourceManager = new ResourceManager(entityManager);
                entityManager.getTransaction().begin();

                Resource resource = resourceManager.get(ObjectIdentifier.parseLocalId(resourceId, ObjectType.RESOURCE));
                for (ReservationRequestSummary reservationRequestSummary : listResponse) {
                    Long requestId = ObjectIdentifier.parseLocalId(reservationRequestSummary.getId(), ObjectType.RESERVATION_REQUEST);
                    ReservationRequest request = reservationRequestManager.getReservationRequest(requestId);
                    if (AbstractReservationRequest.State.ACTIVE.equals(request.getState())) {
                        request.setAllocationState(ReservationRequest.AllocationState.DENIED);

                        SchedulerReportSet.ReservationRequestDeniedAlreadyAllocatedReport report = new SchedulerReportSet.ReservationRequestDeniedAlreadyAllocatedReport();
                        report.setResource(resource);
                        report.setInterval(reservationRequest.getSlot());
                        request.setReport(report);

                        reservationRequestManager.update(request);
                    }
                }

                entityManager.getTransaction().commit();

                // Send notifications to resource administrators and owner of the reservation requests
                for (ReservationRequestSummary reservationRequestSummary : listResponse) {
                    Long requestId = ObjectIdentifier.parseLocalId(reservationRequestSummary.getId(), ObjectType.RESERVATION_REQUEST);
                    ReservationRequest reservationRequestDenied = reservationRequestManager.getReservationRequest(requestId);

                    AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
                    List<PersonInformation> recipients = resource.getAdministrators(authorizationManager, false);
                    ReservationRequestDeniedNotification notification;

                    notification = new ReservationRequestDeniedNotification(reservationRequestDenied, authorizationManager);
                    notification.addRecipients(recipients, true);

                    notificationManager.addNotification(notification, entityManager);
                }
            }
            finally {
                if (entityManager.getTransaction().isActive()) {
                    entityManager.getTransaction().rollback();
                }
                entityManager.close();
            }
        }

        return Boolean.TRUE;
    }

    @Override
    public Boolean denyReservationRequest(SecurityToken securityToken, String reservationRequestId, String reason)
    {
        authorization.validate(securityToken);
        checkNotNull("reservationRequestId", reservationRequestId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        ObjectIdentifier objectId = ObjectIdentifier.parse(reservationRequestId, ObjectType.RESERVATION_REQUEST);

        try {
            entityManager.getTransaction().begin();

            // Get old reservation request and check permissions and restrictions for confirmation
            cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest abstractReservationRequest =
                    reservationRequestManager.get(objectId.getPersistenceId());
            ReservationRequest reservationRequest;
            if (abstractReservationRequest instanceof ReservationRequest) {
                reservationRequest = (ReservationRequest) abstractReservationRequest;
            } else {
                throw new TodoImplementException("ReservationRequestSet is unsupported for confirmation.");
            }

            if (!ReservationRequest.AllocationState.CONFIRM_AWAITING.equals(reservationRequest.getAllocationState())) {
                return Boolean.TRUE;
            }

            cz.cesnet.shongo.controller.booking.specification.Specification specification = reservationRequest.getSpecification();
            Resource resource;
            if (specification instanceof ResourceSpecification) {
                ResourceSpecification resourceSpecification = (ResourceSpecification) specification;
                resource = resourceSpecification.getResource();
            } else {
                throw new TodoImplementException("Unsupported specification type: " + specification.getClass().getSimpleName());
            }

            if (!authorization.hasObjectPermission(securityToken, resource, ObjectPermission.CONTROL_RESOURCE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("confirm reservation request %s", objectId);
            }

            switch (reservationRequest.getState()) {
                case MODIFIED:
                    throw new ControllerReportSet.ReservationRequestAlreadyModifiedException(objectId.toId());
                case DELETED:
                    throw new ControllerReportSet.ReservationRequestDeletedException(objectId.toId());
            }

            reservationRequest.setAllocationState(ReservationRequest.AllocationState.DENIED);

            SchedulerReportSet.ReservationRequestDeniedReport report = new SchedulerReportSet.ReservationRequestDeniedReport();
            report.setDeniedBy(authorization.getUserInformation(securityToken).getUserId());
            report.setReason(reason);
            reservationRequest.setReport(report);
            reservationRequestManager.update(reservationRequest);

            entityManager.getTransaction().commit();

            // Send notifications to resource administrators and owner of the reservation request
            if (reservationRequest.getSpecification() instanceof ResourceSpecification) {
                AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
                ResourceSpecification resourceSpecification = (ResourceSpecification) reservationRequest.getSpecification();
                List<PersonInformation> recipients = resourceSpecification.getResource().getAdministrators(authorizationManager, false);
                ReservationRequestDeniedNotification notification;

                notification = new ReservationRequestDeniedNotification(reservationRequest, authorizationManager);
                notification.addRecipients(recipients, true);

                notificationManager.addNotification(notification, entityManager);
            }
            else {
                throw new TodoImplementException("Confirmation not supported for specification type: "
                        + reservationRequest.getSpecification().getClass().getSimpleName());
            }

            return Boolean.TRUE;
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public String revertReservationRequest(SecurityToken securityToken, String reservationRequestId)
    {
        authorization.validate(securityToken);
        checkNotNull("reservationRequestId", reservationRequestId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        ObjectIdentifier objectId = ObjectIdentifier.parse(reservationRequestId, ObjectType.RESERVATION_REQUEST);
        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest abstractReservationRequest =
                    reservationRequestManager.get(objectId.getPersistenceId());

            if (!abstractReservationRequest.getState().equals(
                    cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest.State.ACTIVE)) {
                throw new ControllerReportSet.ReservationRequestNotRevertibleException(
                        ObjectIdentifier.formatId(abstractReservationRequest));
            }

            if (abstractReservationRequest instanceof ReservationRequest) {
                ReservationRequest reservationRequest =
                        (ReservationRequest) abstractReservationRequest;
                if (reservationRequest.getAllocationState().equals(
                        ReservationRequest.AllocationState.ALLOCATED)) {
                    throw new ControllerReportSet.ReservationRequestNotRevertibleException(
                            ObjectIdentifier.formatId(abstractReservationRequest));
                }
            }

            if (!authorization.hasObjectPermission(securityToken, abstractReservationRequest, ObjectPermission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("revert reservation request %s", objectId);
            }

            // Set modified reservation request as ACTIVE
            cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest modifiedReservationRequest =
                    abstractReservationRequest.getModifiedReservationRequest();
            modifiedReservationRequest.setState(
                    cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest.State.ACTIVE);
            modifiedReservationRequest.getAllocation().setReservationRequest(modifiedReservationRequest);
            reservationRequestManager.update(modifiedReservationRequest);

            // Revert the modification
            reservationRequestManager.delete(abstractReservationRequest, true);

            abstractReservationRequest.getSpecification().updateSpecificationSummary(entityManager, true);

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction(securityToken);

            return ObjectIdentifier.formatId(modifiedReservationRequest);
        }
        finally {
            if (authorizationManager.isTransactionActive()) {
                authorizationManager.rollbackTransaction();
            }
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public void deleteReservationRequest(SecurityToken securityToken, String reservationRequestId)
    {
        authorization.validate(securityToken);
        checkNotNull("reservationRequestId", reservationRequestId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        ObjectIdentifier objectId = ObjectIdentifier.parse(reservationRequestId, ObjectType.RESERVATION_REQUEST);
        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest =
                    reservationRequestManager.get(objectId.getPersistenceId());

            if (!authorization.hasObjectPermission(securityToken, reservationRequest, ObjectPermission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("delete reservation request %s", objectId);
            }
            switch (reservationRequest.getState()) {
                case MODIFIED:
                    throw new ControllerReportSet.ReservationRequestNotDeletableException(objectId.toId());
                case DELETED:
                    throw new ControllerReportSet.ReservationRequestDeletedException(objectId.toId());
            }
            ReservationManager reservationManager = new ReservationManager(entityManager);
            if (!isReservationRequestDeletable(reservationRequest, reservationManager)) {
                throw new ControllerReportSet.ReservationRequestNotDeletableException(
                        ObjectIdentifier.formatId(reservationRequest));
            }

            reservationRequest.setUpdatedBy(securityToken.getUserId());
            reservationRequestManager.softDelete(reservationRequest, authorizationManager);

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction(securityToken);
        }
        finally {
            if (authorizationManager.isTransactionActive()) {
                authorizationManager.rollbackTransaction();
            }
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public void deleteReservationRequestHard(SecurityToken securityToken, String reservationRequestId)
    {
        authorization.validate(securityToken);
        checkNotNull("reservationRequestId", reservationRequestId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        ObjectIdentifier objectId = ObjectIdentifier.parse(reservationRequestId, ObjectType.RESERVATION_REQUEST);
        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest =
                    reservationRequestManager.get(objectId.getPersistenceId());

            if (!authorization.hasObjectPermission(securityToken, reservationRequest, ObjectPermission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("delete reservation request %s", objectId);
            }
            switch (reservationRequest.getState()) {
                case MODIFIED:
                    throw new ControllerReportSet.ReservationRequestNotDeletableException(objectId.toId());
            }
            ReservationManager reservationManager = new ReservationManager(entityManager);
            if (!isReservationRequestDeletable(reservationRequest, reservationManager)) {
                throw new ControllerReportSet.ReservationRequestNotDeletableException(
                        ObjectIdentifier.formatId(reservationRequest));
            }

            reservationRequest.setUpdatedBy(securityToken.getUserId());
            reservationRequestManager.hardDelete(reservationRequest, authorizationManager);

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction(securityToken);
        }
        finally {
            if (authorizationManager.isTransactionActive()) {
                authorizationManager.rollbackTransaction();
            }
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public void updateReservationRequest(SecurityToken securityToken, String reservationRequestId)
    {
        authorization.validate(securityToken);
        checkNotNull("reservationRequestId", reservationRequestId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        ObjectIdentifier objectId = ObjectIdentifier.parse(reservationRequestId, ObjectType.RESERVATION_REQUEST);
        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest abstractReservationRequest =
                    reservationRequestManager.get(objectId.getPersistenceId());

            if (!authorization.hasObjectPermission(securityToken, abstractReservationRequest, ObjectPermission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("update reservation request %s", objectId);
            }

            // Update reservation requests
            if (abstractReservationRequest instanceof ReservationRequest) {
                ReservationRequest reservationRequest =
                        (ReservationRequest) abstractReservationRequest;
                switch (reservationRequest.getAllocationState()) {
                    case ALLOCATION_FAILED: {
                        // Reservation request was modified, so we must clear it's state
                        reservationRequest.clearState();
                        // Update state
                        reservationRequest.updateStateBySpecification();
                    }
                }
            }

            // Update child reservation requests
            for (ReservationRequest reservationRequest :
                    abstractReservationRequest.getAllocation().getChildReservationRequests()) {
                switch (reservationRequest.getAllocationState()) {
                    case ALLOCATION_FAILED: {
                        // Reservation request was modified, so we must clear it's state
                        reservationRequest.clearState();
                        // Update state
                        reservationRequest.updateStateBySpecification();
                    }
                }
            }

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction(securityToken);
        }
        finally {
            if (authorizationManager.isTransactionActive()) {
                authorizationManager.rollbackTransaction();
            }
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public ListResponse<ReservationRequestSummary> listReservationRequests(ReservationRequestListRequest request)
    {
        checkNotNull("request", request);
        SecurityToken securityToken = request.getSecurityToken();
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            QueryFilter queryFilter = new QueryFilter("reservation_request_summary", true);

            // List only reservation requests which is current user permitted to read
            queryFilter.addFilterId("allocation_id", authorization, securityToken,
                    Allocation.class, ObjectPermission.READ);

            // List only reservation requests which are requested (but latest versions of them)
            if (request.getReservationRequestIds().size() > 0) {
                queryFilter.addFilter("reservation_request_summary.id IN ("
                        + " SELECT allocation.abstract_reservation_request_id"
                        + " FROM abstract_reservation_request"
                        + " LEFT JOIN allocation ON allocation.id = abstract_reservation_request.allocation_id"
                        + " WHERE abstract_reservation_request.id IN(:reservationRequestIds)"
                        + ")");
                Set<Long> reservationRequestIds = new HashSet<Long>();
                for (String reservationRequestId : request.getReservationRequestIds()) {
                    reservationRequestIds.add(ObjectIdentifier.parseLocalId(
                            reservationRequestId, ObjectType.RESERVATION_REQUEST));
                }
                queryFilter.addFilterParameter("reservationRequestIds", reservationRequestIds);
            }
            // Else other filters
            else {
                if (request.isHistory()) {
                    // List only latest versions of a reservation requests with deleted requests
                    queryFilter.addFilter("reservation_request_summary.state != 'MODIFIED'");
                }
                else {
                    // List only latest versions of a reservation requests (no it's modifications or deleted requests)
                    queryFilter.addFilter("reservation_request_summary.state = 'ACTIVE'");
                }

                // List only child reservation requests for specified parent reservation request
                String parentReservationRequestId = request.getParentReservationRequestId();
                if (parentReservationRequestId != null) {
                    queryFilter.addFilter("reservation_request.parent_allocation_id IN("
                            + " SELECT DISTINCT abstract_reservation_request.allocation_id"
                            + " FROM abstract_reservation_request "
                            + " WHERE abstract_reservation_request.id = :parentReservationRequestId)");
                    queryFilter.addFilterParameter("parentReservationRequestId", ObjectIdentifier.parseLocalId(
                            parentReservationRequestId, ObjectType.RESERVATION_REQUEST));
                }
                else {
                    // List only top reservation requests (no child requests created for a set of reservation requests)
                    queryFilter.addFilter("reservation_request.parent_allocation_id IS NULL");
                }

                // List only reservation requests which specifies given technologies
                if (request.getSpecificationTechnologies().size() > 0) {
                    queryFilter.addFilter("reservation_request_summary.id IN ("
                            + "  SELECT DISTINCT abstract_reservation_request.id"
                            + "  FROM abstract_reservation_request"
                            + "  LEFT JOIN specification_technologies ON specification_technologies.specification_id = "
                            + "            abstract_reservation_request.specification_id"
                            + "  WHERE specification_technologies.technologies IN(:technologies))");
                    queryFilter.addFilterParameter("technologies", request.getSpecificationTechnologies());
                }

                // List only reservation requests which has specification of given classes
                if (request.getSpecificationTypes().size() > 0) {
                    StringBuilder specificationTypes = new StringBuilder();
                    for (ReservationRequestSummary.SpecificationType type : request.getSpecificationTypes()) {
                        if (specificationTypes.length() > 0) {
                            specificationTypes.append(",");
                        }
                        specificationTypes.append("'");
                        specificationTypes.append(type);
                        specificationTypes.append("'");
                    }
                    queryFilter.addFilter("specification_summary.type IN(" + specificationTypes.toString() + ")");
                }

                // Filter specification resource id
                String specificationResourceId = request.getSpecificationResourceId();
                if (specificationResourceId != null) {
                    queryFilter.addFilter("specification_summary.resource_id = :resource_id");
                    queryFilter.addFilterParameter("resource_id",
                            ObjectIdentifier.parseLocalId(specificationResourceId, ObjectType.RESOURCE));
                }

                String reusedReservationRequestId = request.getReusedReservationRequestId();
                if (reusedReservationRequestId != null) {
                    if (reusedReservationRequestId.equals(ReservationRequestListRequest.FILTER_EMPTY)) {
                        // List only reservation requests which hasn't reused any reservation request
                        queryFilter.addFilter("reservation_request_summary.reused_reservation_request_id IS NULL");
                    }
                    else if (reusedReservationRequestId.equals(ReservationRequestListRequest.FILTER_NOT_EMPTY)) {
                        // List only reservation requests which reuse any reservation request
                        queryFilter.addFilter("reservation_request_summary.reused_reservation_request_id IS NOT NULL");
                    }
                    else {
                        // List only reservation requests which reuse given reservation request
                        Long persistenceId = ObjectIdentifier.parseLocalId(
                                reusedReservationRequestId, ObjectType.RESERVATION_REQUEST);
                        queryFilter.addFilter("reservation_request_summary.reused_reservation_request_id = "
                                + ":reusedReservationRequestId");
                        queryFilter.addFilterParameter("reusedReservationRequestId", persistenceId);
                    }
                }

                AllocationState allocationState = request.getAllocationState();
                if (allocationState != null) {
                    queryFilter.addFilter("reservation_request_summary.allocation_state = :allocationState");
                    queryFilter.addFilterParameter("allocationState", allocationState.toString());
                }

                Interval interval = request.getInterval();
                if (interval != null) {
                    queryFilter.addFilter("reservation_request_summary.slot_start < :intervalEnd");
                    queryFilter.addFilter("reservation_request_summary.slot_end > :intervalStart");
                    if (request.isIntervalDateOnly()) {
                        queryFilter.addFilterParameter("intervalStart", interval.getStart().toDate());
                        queryFilter.addFilterParameter("intervalEnd", interval.getEnd().toDate());
                    } else {
                        throw new TodoImplementException("interval used only for dates without time");
                    }
                }

                String userId = request.getUserId();
                if (userId != null) {
                    queryFilter.addFilter("reservation_request_summary.created_by = :userId"
                            + " OR reservation_request_summary.updated_by = :userId"
                            + " OR reservation_request_summary.allocation_id IN("
                            + "   SELECT acl_object_identity.object_id FROM acl_entry"
                            + "   LEFT JOIN acl_identity ON acl_identity.id = acl_entry.acl_identity_id"
                            + "   LEFT JOIN acl_object_identity ON acl_object_identity.id = acl_entry.acl_object_identity_id"
                            + "   LEFT JOIN acl_object_class ON acl_object_class.id = acl_object_identity.acl_object_class_id"
                            + "   WHERE acl_object_class.class = 'RESERVATION_REQUEST' AND acl_identity.principal_id = :userId)");
                    queryFilter.addFilterParameter("userId", userId);
                }

                String participantUserId = request.getParticipantUserId();
                if (participantUserId != null) {
                    queryFilter.addFilter("reservation_request_summary.allocation_id IN("
                            + " SELECT reservation.allocation_id FROM reservation"
                            + " LEFT JOIN room_endpoint_participants ON room_endpoint_participants.room_endpoint_id = reservation.executable_id"
                            + " LEFT JOIN person_participant ON person_participant.id = abstract_participant_id"
                            + " LEFT JOIN person ON person.id = person_participant.person_id"
                            + " WHERE person.user_id = :participantUserId)");
                    queryFilter.addFilterParameter("participantUserId", participantUserId);
                }

                String search = request.getSearch();
                if (search != null) {
                    queryFilter.addFilter("LOWER(reservation_request_summary.description) LIKE :search"
                            + " OR LOWER(specification_summary.alias_room_name) LIKE :search"
                            + " OR LOWER(reused_specification_summary.alias_room_name) LIKE :search");
                    queryFilter.addFilterParameter("search", "%" + search.toLowerCase() + "%");
                }
            }

            // Query order by
            String queryOrderBy;
            ReservationRequestListRequest.Sort sort = request.getSort();
            if (sort != null) {
                switch (sort) {
                    case ALIAS_ROOM_NAME:
                        queryOrderBy = "specification_summary.alias_room_name";
                        break;
                    case RESOURCE_ROOM_NAME:
                        queryOrderBy = "resource_summary.name";
                        break;
                    case DATETIME:
                        queryOrderBy = "reservation_request_summary.created_at";
                        break;
                    case REUSED_RESERVATION_REQUEST:
                        queryOrderBy = "reservation_request_summary.reused_reservation_request_id IS NOT NULL";
                        break;
                    case ROOM_PARTICIPANT_COUNT:
                        queryOrderBy = "specification_summary.room_participant_count";
                        break;
                    case SLOT:
                        queryOrderBy = "reservation_request_summary.slot_end";
                        break;
                    case SLOT_NEAREST:
                        queryOrderBy = "reservation_request_summary.slot_nearness_priority, reservation_request_summary.slot_nearness_value";
                        break;
                    case STATE:
                        queryOrderBy = "reservation_request_summary.allocation_state, reservation_request_summary.executable_state";
                        break;
                    case TECHNOLOGY:
                        queryOrderBy = "specification_summary.technologies";
                        break;
                    case TYPE:
                        queryOrderBy = "specification_summary.type";
                        break;
                    case USER:
                        queryOrderBy = "reservation_request_summary.created_by";
                        break;
                    default:
                        throw new TodoImplementException(sort);
                }
            }
            else {
                queryOrderBy = "reservation_request_summary.id";
            }
            Boolean sortDescending = request.getSortDescending();
            sortDescending = (sortDescending != null ? sortDescending : false);
            if (sortDescending) {
                queryOrderBy = queryOrderBy + " DESC";
            }

            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("filter", queryFilter.toQueryWhere());
            parameters.put("order", queryOrderBy);
            String query = NativeQuery.getNativeQuery(NativeQuery.RESERVATION_REQUEST_LIST, parameters);

            ListResponse<ReservationRequestSummary> response = new ListResponse<ReservationRequestSummary>();
            List<Object[]> records = performNativeListRequest(query, queryFilter, request, response, entityManager);
            for (Object[] record : records) {
                ReservationRequestSummary reservationRequestSummary = getReservationRequestSummary(record);
                response.addItem(reservationRequestSummary);
            }
            return response;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public ListResponse<ReservationRequestSummary> listOwnedResourcesReservationRequests(ReservationRequestListRequest request)
    {
        checkNotNull("request", request);
        SecurityToken securityToken = request.getSecurityToken();
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            QueryFilter queryFilter = new QueryFilter("reservation_request_summary", true);

            // List only reservation requests which current user owns
            Set<Long> ownedResourceIds = authorization.getEntitiesWithRole(securityToken, Resource.class, ObjectRole.OWNER);

            // Return empty response when user doesn't own any resources
            if (ownedResourceIds == null || ownedResourceIds.isEmpty()) {
                return new ListResponse<>();
            } else {
                queryFilter.addFilter("specification_summary.resource_id IN (:resourceIds)");
                queryFilter.addFilterParameter("resourceIds", ownedResourceIds);
            }

            // Filter specification resource id
            String specificationResourceId = request.getSpecificationResourceId();
            if (specificationResourceId != null) {
                queryFilter.addFilter("specification_summary.resource_id = :resource_id");
                queryFilter.addFilterParameter("resource_id",
                        ObjectIdentifier.parseLocalId(specificationResourceId, ObjectType.RESOURCE));
            }

            // List only latest versions of a reservation requests (no it's modifications or deleted requests)
            queryFilter.addFilter("reservation_request_summary.state = 'ACTIVE'");

            AllocationState allocationState = request.getAllocationState();
            if (allocationState != null) {
                queryFilter.addFilter("reservation_request_summary.allocation_state = :allocationState");
                queryFilter.addFilterParameter("allocationState", allocationState.toString());
            }

            Interval interval = request.getInterval();
            if (interval != null) {
                queryFilter.addFilter("reservation_request_summary.slot_start < :intervalEnd");
                queryFilter.addFilter("reservation_request_summary.slot_end > :intervalStart");
                if (request.isIntervalDateOnly()) {
                    queryFilter.addFilterParameter("intervalStart", interval.getStart().toDate());
                    queryFilter.addFilterParameter("intervalEnd", interval.getEnd().toDate());
                } else {
                    queryFilter.addFilterParameter("intervalStart", Temporal.convertDateTimeToTimestamp(interval.getStart()));
                    queryFilter.addFilterParameter("intervalEnd", Temporal.convertDateTimeToTimestamp(interval.getEnd()));
                }
            }

            // Query order by
            String queryOrderBy;
            ReservationRequestListRequest.Sort sort = request.getSort();
            if (sort != null) {
                switch (sort) {
                    case ALIAS_ROOM_NAME:
                        queryOrderBy = "specification_summary.alias_room_name";
                        break;
                    case RESOURCE_ROOM_NAME:
                        queryOrderBy = "resource_summary.name";
                        break;
                    case DATETIME:
                        queryOrderBy = "reservation_request_summary.created_at";
                        break;
                    case REUSED_RESERVATION_REQUEST:
                        queryOrderBy = "reservation_request_summary.reused_reservation_request_id IS NOT NULL";
                        break;
                    case ROOM_PARTICIPANT_COUNT:
                        queryOrderBy = "specification_summary.room_participant_count";
                        break;
                    case SLOT:
                        queryOrderBy = "reservation_request_summary.slot_end";
                        break;
                    case SLOT_NEAREST:
                        queryOrderBy = "reservation_request_summary.slot_nearness_priority, reservation_request_summary.slot_nearness_value";
                        break;
                    case STATE:
                        queryOrderBy = "reservation_request_summary.allocation_state, reservation_request_summary.executable_state";
                        break;
                    case TECHNOLOGY:
                        queryOrderBy = "specification_summary.technologies";
                        break;
                    case TYPE:
                        queryOrderBy = "specification_summary.type";
                        break;
                    case USER:
                        queryOrderBy = "reservation_request_summary.created_by";
                        break;
                    default:
                        throw new TodoImplementException(sort);
                }
            }
            else {
                queryOrderBy = "reservation_request_summary.id";
            }
            Boolean sortDescending = request.getSortDescending();
            sortDescending = (sortDescending != null ? sortDescending : false);
            if (sortDescending) {
                queryOrderBy = queryOrderBy + " DESC";
            }

            Map<String, String> parameters = new HashMap<>();
            parameters.put("filter", queryFilter.toQueryWhere());
            parameters.put("order", queryOrderBy);
            String query = NativeQuery.getNativeQuery(NativeQuery.RESERVATION_REQUEST_LIST, parameters);

            ListResponse<ReservationRequestSummary> response = new ListResponse<>();
            List<Object[]> records = performNativeListRequest(query, queryFilter, request, response, entityManager);
            for (Object[] record : records) {
                ReservationRequestSummary reservationRequestSummary = getReservationRequestSummary(record);
                response.addItem(reservationRequestSummary);
            }
            return response;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public cz.cesnet.shongo.controller.api.AbstractReservationRequest getReservationRequest(SecurityToken securityToken,
            String reservationRequestId)
    {
        authorization.validate(securityToken);
        checkNotNull("reservationRequestId", reservationRequestId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        ObjectIdentifier objectId = ObjectIdentifier.parse(reservationRequestId, ObjectType.RESERVATION_REQUEST);
        try {
            cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest =
                    reservationRequestManager.get(objectId.getPersistenceId());
            if (!authorization.hasObjectPermission(securityToken, reservationRequest, ObjectPermission.READ)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read reservation request %s", objectId);
            }

            return reservationRequest.toApi(authorization.isOperator(securityToken));
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public List<ReservationRequestSummary> getReservationRequestHistory(SecurityToken securityToken,
            String reservationRequestId)
    {
        authorization.validate(securityToken);
        checkNotNull("reservationRequestId", reservationRequestId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        ObjectIdentifier objectId = ObjectIdentifier.parse(reservationRequestId, ObjectType.RESERVATION_REQUEST);
        try {
            cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest =
                    reservationRequestManager.get(objectId.getPersistenceId());

            DateTime timeTTTTtime = DateTime.now();

            if (!authorization.hasObjectPermission(securityToken, reservationRequest, ObjectPermission.READ)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read reservation request %s", objectId);
            }

            String historyQuery = NativeQuery.getNativeQuery(NativeQuery.RESERVATION_REQUEST_HISTORY);

            List history = entityManager.createNativeQuery(historyQuery)
                    .setParameter("allocationId", reservationRequest.getAllocation().getId())
                    .getResultList();

            System.out.println("LIST: " + DateTime.now().minus(timeTTTTtime.getMillis()).getMillis() + " ms");

            List<ReservationRequestSummary> reservationRequestSummaries = new LinkedList<>();
            ReservationRequestSummary deletedReservationRequestSummary = null;
            for (Object historyItem : history) {
                Object[] historyItemData = (Object[]) historyItem;

                // Add last request as deleted to the list one more time if exists
                if (historyItemData[22] != null) {
                    AbstractReservationRequest.State state = AbstractReservationRequest.State.valueOf((String) historyItemData[22]);
                    if (AbstractReservationRequest.State.DELETED == state) {
                        if (deletedReservationRequestSummary != null) {
                            throw new TodoImplementException("Multiple deleted requests in history.");
                        }
                        deletedReservationRequestSummary = getReservationRequestHistory(historyItemData);
                        deletedReservationRequestSummary.setType(ReservationRequestType.DELETED);
                        deletedReservationRequestSummary.setAllocationState(null);
                        deletedReservationRequestSummary.setExecutableState(null);
                        reservationRequestSummaries.add(deletedReservationRequestSummary);
                    }
                }

                ReservationRequestSummary reservationRequestSummary = getReservationRequestHistory(historyItemData);
                reservationRequestSummaries.add(reservationRequestSummary);
            }


            return reservationRequestSummaries;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public List<Reservation> getReservationRequestReservations(SecurityToken securityToken, String reservationRequestId)
    {
        authorization.validate(securityToken);
        checkNotNull("reservationRequestId", reservationRequestId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        ReservationManager reservationManager = new ReservationManager(entityManager);
        ObjectIdentifier objectId = ObjectIdentifier.parse(reservationRequestId, ObjectType.RESERVATION_REQUEST);
        try {
            cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest =
                    reservationRequestManager.get(objectId.getPersistenceId());

            if (!authorization.hasObjectPermission(securityToken, reservationRequest, ObjectPermission.READ)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read reservation request %s", objectId);
            }

            List<Reservation> reservations = new LinkedList<Reservation>();
            for (cz.cesnet.shongo.controller.booking.reservation.Reservation reservation :
                    reservationManager.listByReservationRequest(objectId.getPersistenceId())) {
                reservations.add(reservation.toApi(entityManager, authorization.isOperator(securityToken)));
            }
            return reservations;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public Reservation getReservation(SecurityToken securityToken, String reservationId)
    {
        authorization.validate(securityToken);
        checkNotNull("reservationId", reservationId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationManager reservationManager = new ReservationManager(entityManager);
        ObjectIdentifier objectId = ObjectIdentifier.parse(reservationId, ObjectType.RESERVATION);

        try {
            cz.cesnet.shongo.controller.booking.reservation.Reservation reservation =
                    reservationManager.get(objectId.getPersistenceId());

            if (!authorization.hasObjectPermission(securityToken, reservation, ObjectPermission.READ)) {
                cz.cesnet.shongo.controller.booking.resource.Resource resource = reservation.getAllocatedResource();
                if (resource == null || !authorization.hasObjectRole(securityToken, resource, ObjectRole.OWNER)) {
                    ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read reservation %s", objectId);
                }
            }

            return reservation.toApi(entityManager, authorization.isOperator(securityToken));
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public List<Reservation> getReservations(SecurityToken securityToken, Collection<String> reservationIds)
    {
        authorization.validate(securityToken);
        checkNotNull("reservationIds", reservationIds);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationManager reservationManager = new ReservationManager(entityManager);
        try {
            Set<Long> reservationPersistentIds = new HashSet<Long>();
            for (String reservationId : reservationIds) {
                ObjectIdentifier objectId = ObjectIdentifier.parse(reservationId, ObjectType.RESERVATION);
                cz.cesnet.shongo.controller.booking.reservation.Reservation reservation =
                        reservationManager.get(objectId.getPersistenceId());
                if (!authorization.hasObjectPermission(securityToken, reservation, ObjectPermission.READ)) {
                    ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read reservation %s", objectId);
                }
                reservationPersistentIds.add(objectId.getPersistenceId());
            }
            List<Reservation> reservations = new LinkedList<Reservation>();
            for (cz.cesnet.shongo.controller.booking.reservation.Reservation reservation :
                    reservationManager.listByIds(reservationPersistentIds)) {
                reservations.add(reservation.toApi(entityManager, authorization.isOperator(securityToken)));
            }
            return reservations;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public ListResponse<ReservationSummary> listReservations(ReservationListRequest request)
    {
        checkNotNull("request", request);
        SecurityToken securityToken = request.getSecurityToken();
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ResourceManager resourceManager = new ResourceManager(entityManager);
        try {
            QueryFilter queryFilter = new QueryFilter("reservation_summary");

            // Show reservations
            Boolean hasReadForAll = false;
            if (request.getResourceIds().size() == 1) {
                String resourceId = request.getResourceIds().iterator().next();

                PersistentObject persistentObject = resourceManager.findResourcesPersistentObject(resourceId);
                if (authorization.hasObjectPermission(securityToken, persistentObject, ObjectPermission.READ)) {
                    hasReadForAll = true;
                }

                try {
                    if (!ObjectIdentifier.isLocal(resourceId)) {
                        ListResponse<ReservationSummary> reservations = new ListResponse<>();
                        reservations.addAll(InterDomainAgent.getInstance().getConnector().listForeignResourcesReservations(resourceId, request.getInterval()));
                        reservations.setCount(reservations.getItemCount());
                        return reservations;
                    }
                }
                catch (Exception ex) {
                    String message = "Listing of foreign resources reservations has failed.";
                    InterDomainAgent.getInstance().logAndNotifyDomainAdmins(message, ex);
                }
            }
            // List only reservations which is current user permitted to read or which allocates resource owned by the user
            Set<Long> readableReservationIds = null;
            if (!hasReadForAll) {
                readableReservationIds = authorization.getEntitiesWithPermission(securityToken,
                        cz.cesnet.shongo.controller.booking.reservation.Reservation.class, ObjectPermission.READ);
            }
            if (readableReservationIds != null) {
                Set<Long> ownedResourceIds = authorization.getEntitiesWithRole(securityToken,
                        cz.cesnet.shongo.controller.booking.resource.Resource.class, ObjectRole.OWNER);
                StringBuilder filterBuilder = new StringBuilder();
                filterBuilder.append("1=0");
                //TODO: except reservations without slot (see AbstractForeingReservation)
                if (!readableReservationIds.isEmpty()) {
                    filterBuilder.append(" OR reservation_summary.id IN(:readableReservationIds)");
                    queryFilter.addFilterParameter("readableReservationIds", readableReservationIds);
                }
                if (!ownedResourceIds.isEmpty()) {
                    filterBuilder.append(" OR reservation_summary.resource_id IN(:ownedResourceIds)");
                    queryFilter.addFilterParameter("ownedResourceIds", ownedResourceIds);
                }
                queryFilter.addFilter(filterBuilder.toString());
            }

            // List only reservations of requested types
            if (request.getReservationTypes().size() > 0) {
                StringBuilder reservationTypes = new StringBuilder();
                for (ReservationSummary.Type reservationType : request.getReservationTypes()) {
                    if (reservationTypes.length() > 0) {
                        reservationTypes.append(",");
                    }
                    reservationTypes.append("'");
                    reservationTypes.append(reservationType);
                    reservationTypes.append("'");
                }
                queryFilter.addFilter("reservation_summary.type IN(" + reservationTypes.toString() + ")");
            }

            // List only reservations which allocates requested resource
            if (!request.getResourceIds().isEmpty()) {
                Set<Long> resourceIds = new HashSet<>();
                Set<Long> foreignResourcesIds = new HashSet<>();
                for (String resourceId : request.getResourceIds()) {
                    if (ObjectIdentifier.isLocal(resourceId)) {
                        resourceIds.add(ObjectIdentifier.parseLocalId(resourceId, ObjectType.RESOURCE));
                    }
                    else {
                        ObjectIdentifier resourceIdentifier = ObjectIdentifier.parseForeignId(resourceId);
                        ForeignResources foreignResources = resourceManager.findForeignResourcesByResourceId(resourceIdentifier);
                        foreignResourcesIds.add(foreignResources.getId());
                    }
                }
                if (!resourceIds.isEmpty()) {
                    queryFilter.addFilter("reservation_summary.resource_id IN(:resourceIds)");
                    queryFilter.addFilterParameter("resourceIds", resourceIds);
                }
                if (!foreignResourcesIds.isEmpty()) {
                    queryFilter.addFilter("reservation_summary.foreign_resources_id IN(:foreignResourcesIds)");
                    queryFilter.addFilterParameter("foreignResourcesIds", foreignResourcesIds);
                }
            }

            // List only reservations in requested interval
            Interval interval = request.getInterval();
            if (interval != null) {
                queryFilter.addFilter("reservation_summary.slot_start < :slotEnd");
                queryFilter.addFilter("reservation_summary.slot_end > :slotStart");
                queryFilter.addFilterParameter("slotStart", interval.getStart().toDate());
                queryFilter.addFilterParameter("slotEnd", interval.getEnd().toDate());
            }

            // Sort query part
            String queryOrderBy;
            ReservationListRequest.Sort sort = request.getSort();
            if (sort != null) {
                switch (sort) {
                    case SLOT:
                        queryOrderBy = "reservation_summary.slot_start";
                        break;
                    default:
                        throw new TodoImplementException(sort);
                }
            }
            else {
                queryOrderBy = "reservation_summary.id";
            }
            Boolean sortDescending = request.getSortDescending();
            sortDescending = (sortDescending != null ? sortDescending : false);
            if (sortDescending) {
                queryOrderBy = queryOrderBy + " DESC";
            }

            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("filter", queryFilter.toQueryWhere());
            parameters.put("order", queryOrderBy);
            String query = NativeQuery.getNativeQuery(NativeQuery.RESERVATION_LIST, parameters);

            ListResponse<ReservationSummary> response = new ListResponse<ReservationSummary>();
            List<Object[]> records = performNativeListRequest(query, queryFilter, request, response, entityManager);

            Set<Long> writableReservationIds = authorization.getEntitiesWithPermission(securityToken,
                    cz.cesnet.shongo.controller.booking.reservation.Reservation.class, ObjectPermission.WRITE);

            for (Object[] record : records) {
                ReservationSummary reservationSummary = getReservationSummary(record);
                response.addItem(reservationSummary);
                Long reservationPersistenceId = ObjectIdentifier.parseLocalId(reservationSummary.getId(), ObjectType.RESERVATION);
                boolean isWritable = writableReservationIds == null ? true : writableReservationIds.contains(reservationPersistenceId);
                reservationSummary.setIsWritableByUser(isWritable);
            }
            return response;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public String getResourceReservationsICalendar(ReservationListRequest request) {
        checkNotNull("request", request);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ListResponse<ReservationSummary> reservationSummaries = new ListResponse<ReservationSummary>();
        String resourceName;

        try {
            QueryFilter queryFilter = new QueryFilter("reservation_summary");

            // Check if reservation has set calendar as public and if there is only one resource requested
            if (request.getResourceIds().size() == 1) {
                String resourceId = request.getResourceIds().iterator().next();
                if  (isCachedResourceNullOrNotPublicCalnedar(resourceId)) {
                    //To prevent DoS
                    return "";
                }
                Long persistentResourceId = ObjectIdentifier.parseLocalId(resourceId, ObjectType.RESOURCE);
                ResourceManager resourceManager = new ResourceManager(entityManager);

                cz.cesnet.shongo.controller.booking.resource.Resource resource = resourceManager.get(persistentResourceId);
                if (!resource.isCalendarPublic()) {
                    throw new ControllerReportSet.SecurityNotAuthorizedException("Not authorized");
                }
                resourceName = resource.getName();
            } else {
                throw new TodoImplementException("ReservationService.getResourceReservationsICalendar() support just one resource ID.");
            }

            // List only reservations of requested types
            // NOT NEEDED WHEN JUST ONE RESOURCE IS WANTED
            /*if (request.getReservationTypes().size() > 0) {
                StringBuilder reservationTypes = new StringBuilder();
                for (ReservationSummary.Type reservationType : request.getReservationTypes()) {
                    if (reservationTypes.length() > 0) {
                        reservationTypes.append(",");
                    }
                    reservationTypes.append("'");
                    reservationTypes.append(reservationType);
                    reservationTypes.append("'");
                }
                queryFilter.addFilter("reservation_summary.type IN(" + reservationTypes.toString() + ")");
            }*/

            // List only reservations which allocates requested resource
            // ONLY ONE IS ALLOWED, BUT MAY BE USED IN FUTURE
            if (!request.getResourceIds().isEmpty()) {
                queryFilter.addFilter("reservation_summary.resource_id IN(:resourceIds)");
                Set<Long> resourceIds = new HashSet<Long>();
                for (String resourceId : request.getResourceIds()) {
                    resourceIds.add(ObjectIdentifier.parseLocalId(resourceId, ObjectType.RESOURCE));
                }
                queryFilter.addFilterParameter("resourceIds", resourceIds);
            }

            // List only reservations in requested interval
            Interval interval = request.getInterval();
            if (interval != null) {
                queryFilter.addFilter("reservation_summary.slot_end > :slotStart");
                if (interval.getStart() != null) {
                    queryFilter.addFilterParameter("slotStart", interval.getStart().toDate());
                }
                else {
                    queryFilter.addFilterParameter("slotStart", DateTime.now().minusMonths(1).toDate());
                }

                if (interval.getEnd() != null) {
                    queryFilter.addFilter("reservation_summary.slot_start < :slotEnd");
                    queryFilter.addFilterParameter("slotEnd", interval.getEnd().toDate());
                }
            }
            else {
                queryFilter.addFilter("reservation_summary.slot_end > :slotStart");
                queryFilter.addFilterParameter("slotStart", DateTime.now().minusMonths(1).toDate());
            }

            String queryOrderBy = "reservation_summary.id";
            Boolean sortDescending = request.getSortDescending();
            sortDescending = (sortDescending != null ? sortDescending : false);
            if (sortDescending) {
                queryOrderBy = queryOrderBy + " DESC";
            }

            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("filter", queryFilter.toQueryWhere());
            parameters.put("order", queryOrderBy);
            String query = NativeQuery.getNativeQuery(NativeQuery.RESERVATION_LIST, parameters);

            List<Object[]> records = performNativeListRequest(query, queryFilter, request, reservationSummaries, entityManager);
            for (Object[] record : records) {
                ReservationSummary reservationSummary = getReservationSummary(record);
                reservationSummaries.addItem(reservationSummary);
            }
        }
        finally {
            entityManager.close();
        }

        iCalendar iCalendar = new iCalendar(getConfiguration().getString("domain.name"),resourceName);
        for (ReservationSummary reservation : reservationSummaries) {
            //TODO: consolidate periodic reservations
            cz.cesnet.shongo.controller.util.iCalendar.Event event = iCalendar.addEvent(LocalDomain.getLocalDomainName(), reservation.getId(), reservation.getReservationRequestDescription());
            event.setInterval(reservation.getSlot(), DateTimeZone.getDefault());
        }


        return iCalendar.toString();
    }

    @Override
    public String getCachedResourceReservationsICalendar (ReservationListRequest request) {

        if (request.getResourceIds().size() == 1) {
            String resourceId = request.getResourceIds().iterator().next();
            String iCalendarData = cache.getICalReservation(resourceId);
            if (iCalendarData == null) {
                if  (isCachedResourceNullOrNotPublicCalnedar(resourceId)) {
                    //To prevent DoS
                    return "";
                }

                iCalendarData = getResourceReservationsICalendar(request);
                cache.addICalReservation(resourceId, iCalendarData);
            }
            return iCalendarData;
        } else {
            throw new TodoImplementException("ReservationService.getCachedResourceReservationsICalendar() support just one resource ID.");
        }

    }

    /**
     * Check whether resource with given resourceId is cached and it has public calendar.
     *
     * @param resourceId
     * @return
     */
    private boolean isCachedResourceNullOrNotPublicCalnedar(String resourceId)
    {
        Long persistentId = ObjectIdentifier.parseLocalId(resourceId, ObjectType.RESOURCE);
        Resource resource = cache.getResourceCache().getObject(persistentId);

        if (resource == null || !resource.isCalendarPublic()) {
            return true;
        }
        return false;
    }

    /**
     * Check whether user with given {@code userId} can provide given {@code reusedReservationRequestId}.
     *
     * @param securityToken
     * @param reusedReservationRequestId
     * @throws ControllerReportSet.SecurityNotAuthorizedException
     *
     */
    private void checkReusedReservationRequest(SecurityToken securityToken, String reusedReservationRequestId,
            ReservationRequestManager reservationRequestManager)
    {
        ObjectIdentifier objectId = ObjectIdentifier.parse(reusedReservationRequestId);
        cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest =
                reservationRequestManager.get(objectId.getPersistenceId());
        if (reservationRequest.getState().equals(
                cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest.State.DELETED)) {
            throw new ControllerReportSet.ReservationRequestDeletedException(reusedReservationRequestId);
        }
        if (!authorization.hasObjectPermission(securityToken, reservationRequest,
                ObjectPermission.PROVIDE_RESERVATION_REQUEST)) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault(
                    "provide reservation request %s", objectId);
        }
    }

    /**
     * Check whether {@code abstractReservationRequest} can be modified.
     *
     * @param abstractReservationRequest
     * @return true when the given {@code abstractReservationRequest} can be modified, otherwise false
     */
    private boolean isReservationRequestModifiable(
            cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest abstractReservationRequest)
    {
        Allocation allocation = abstractReservationRequest.getAllocation();

        // Check if reservation request is not created by controller
        if (abstractReservationRequest instanceof ReservationRequest) {
            ReservationRequest reservationRequestImpl =
                    (ReservationRequest) abstractReservationRequest;
            if (reservationRequestImpl.getParentAllocation() != null) {
                return false;
            }
        }

        // Check child reservation requests
        for (ReservationRequest reservationRequestImpl :
                allocation.getChildReservationRequests()) {
            if (isReservationRequestModifiable(reservationRequestImpl)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check whether {@code abstractReservationRequest} can be deleted.
     *
     * @param abstractReservationRequest
     * @return true when the given {@code abstractReservationRequest} can be deleted, otherwise false
     */
    private boolean isReservationRequestDeletable(
            cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest abstractReservationRequest,
            ReservationManager reservationManager)
    {
        Allocation allocation = abstractReservationRequest.getAllocation();

        // Check if reservation request is not created by controller
        if (abstractReservationRequest instanceof ReservationRequest) {
            ReservationRequest reservationRequestImpl =
                    (ReservationRequest) abstractReservationRequest;
            if (reservationRequestImpl.getParentAllocation() != null) {
                return false;
            }
        }

        // Check allocated reservations
        if (reservationManager.isAllocationReused(allocation)) {
            return false;
        }

        // Check child reservation requests
        for (ReservationRequest reservationRequestImpl :
                allocation.getChildReservationRequests()) {
            if (isReservationRequestDeletable(reservationRequestImpl, reservationManager)) {
                return false;
            }
        }

        return true;
    }

    /**
     * For record's params see {@link NativeQuery#RESERVATION_REQUEST_LIST}
     *
     * @param record
     * @return {@link ReservationRequestSummary} from given {@code record} of current reservation request
     */
    private ReservationRequestSummary getReservationRequestSummary(Object[] record)
    {
        ReservationRequestSummary reservationRequestSummary = new ReservationRequestSummary();
        reservationRequestSummary.setId(ObjectIdentifier.formatId(
                ObjectType.RESERVATION_REQUEST, record[0].toString()));
        if (record[1] != null) {
            reservationRequestSummary.setParentReservationRequestId(ObjectIdentifier.formatId(
                    ObjectType.RESERVATION_REQUEST, record[1].toString()));
        }
        reservationRequestSummary.setType(ReservationRequestType.valueOf(record[2].toString().trim()));
        reservationRequestSummary.setDateTime(new DateTime(record[3]));
        reservationRequestSummary.setUserId(record[4].toString());
        reservationRequestSummary.setDescription(record[5] != null ? record[5].toString() : null);
        reservationRequestSummary.setPurpose(ReservationRequestPurpose.valueOf(record[6].toString().trim()));
        reservationRequestSummary.setEarliestSlot(new Interval(
                new DateTime(record[7]), new DateTime(record[8])));
        if (record[9] != null) {
            reservationRequestSummary.setAllocationState(
                    ReservationRequest.AllocationState.valueOf(
                            record[9].toString().trim()).toApi());
        }
        if (record[10] != null) {
            reservationRequestSummary.setExecutableState(
                    Executable.State.valueOf(
                            record[10].toString().trim()).toApi());
        }
        reservationRequestSummary.setReusedReservationRequestId(record[11] != null ?
                ObjectIdentifier.formatId(ObjectType.RESERVATION_REQUEST, record[11].toString()) : null);
        if (record[12] != null) {
            reservationRequestSummary.setLastReservationId(ObjectIdentifier.formatId(
                    ObjectType.RESERVATION, record[12].toString()));
        }
        String type = record[13].toString().trim();
        if (type.equals("ALIAS")) {
            reservationRequestSummary.setSpecificationType(ReservationRequestSummary.SpecificationType.ALIAS);
            reservationRequestSummary.setRoomName(record[16] != null ? record[16].toString() : null);
        }
        else if (type.equals("ROOM")) {
            reservationRequestSummary.setSpecificationType(ReservationRequestSummary.SpecificationType.ROOM);
            reservationRequestSummary.setRoomParticipantCount(
                    record[15] != null ? ((Number) record[15]).intValue() : null);
            reservationRequestSummary.setRoomHasRecordingService(record[16] != null && (Boolean) record[16]);
            reservationRequestSummary.setRoomHasRecordings(record[17] != null && (Boolean) record[17]);
            reservationRequestSummary.setRoomName(record[18] != null ? record[18].toString() : null);
        }
        else if (type.equals("PERMANENT_ROOM")) {
            reservationRequestSummary.setSpecificationType(ReservationRequestSummary.SpecificationType.PERMANENT_ROOM);
            reservationRequestSummary.setRoomHasRecordingService(record[16] != null && (Boolean) record[16]);
            reservationRequestSummary.setRoomHasRecordings(record[17] != null && (Boolean) record[17]);
            reservationRequestSummary.setRoomName(record[18] != null ? record[18].toString() : null);
        }
        else if (type.equals("USED_ROOM")) {
            reservationRequestSummary.setSpecificationType(ReservationRequestSummary.SpecificationType.USED_ROOM);
            reservationRequestSummary.setRoomParticipantCount(
                    record[15] != null ? ((Number) record[15]).intValue() : null);
            reservationRequestSummary.setRoomHasRecordingService(record[16] != null && (Boolean) record[16]);
            reservationRequestSummary.setRoomHasRecordings(record[17] != null && (Boolean) record[17]);
            reservationRequestSummary.setRoomName(record[18] != null ? record[18].toString() : null);
        }
        else if (type.equals("RESOURCE")) {
            reservationRequestSummary.setSpecificationType(ReservationRequestSummary.SpecificationType.RESOURCE);
            if (record[19] != null) {
                reservationRequestSummary.setResourceId(ObjectIdentifier.formatId(
                        ObjectType.RESOURCE, ((Number) record[19]).longValue()));
            }
            else {
                if (record[24] != null && record[23] != null) {
                    String domainName = record[24].toString();
                    Long resourceId = ((Number) record[23]).longValue();
                String foreignResourceId = ObjectIdentifier.formatId(domainName, ObjectType.RESOURCE, resourceId);
                reservationRequestSummary.setResourceId(foreignResourceId);
                }
            }
        }
        else {
            reservationRequestSummary.setSpecificationType(ReservationRequestSummary.SpecificationType.OTHER);
        }
        if (record[14] != null) {
            String technologies = record[14].toString();
            if (!technologies.isEmpty()) {
                for (String technology : technologies.split(",")) {
                    reservationRequestSummary.addSpecificationTechnology(Technology.valueOf(technology.trim()));
                }
            }
        }
        if (record[20] != null) {
            reservationRequestSummary.setUsageExecutableState(
                    cz.cesnet.shongo.controller.booking.executable.Executable.State.valueOf(
                            record[20].toString().trim()).toApi());
        }
        if (record[21] != null) {
            reservationRequestSummary.setFutureSlotCount(((Number) record[21]).intValue());
        }
        if (record[25] != null) {
            reservationRequestSummary.setAllowCache((Boolean) record[25]);
        }
        return reservationRequestSummary;
    }

    /**
     * For record's params see {@link NativeQuery#RESERVATION_REQUEST_HISTORY}.
     *
     * @param record
     * @return {@link ReservationRequestSummary} from given {@code record} of reservation request history
     */
    private ReservationRequestSummary getReservationRequestHistory(Object[] record)
    {
        ReservationRequestSummary reservationRequestSummary = new ReservationRequestSummary();
        reservationRequestSummary.setId(ObjectIdentifier.formatId(
                ObjectType.RESERVATION_REQUEST, record[0].toString()));
        if (record[1] != null) {
            reservationRequestSummary.setParentReservationRequestId(ObjectIdentifier.formatId(
                    ObjectType.RESERVATION_REQUEST, record[1].toString()));
        }
        reservationRequestSummary.setType(ReservationRequestType.valueOf(record[2].toString().trim()));
        reservationRequestSummary.setDateTime(new DateTime(record[3]));
        reservationRequestSummary.setUserId(record[4].toString());
        reservationRequestSummary.setDescription(record[5] != null ? record[5].toString() : null);
        reservationRequestSummary.setPurpose(ReservationRequestPurpose.valueOf(record[6].toString().trim()));
        reservationRequestSummary.setEarliestSlot(new Interval(
                new DateTime(record[7]), new DateTime(record[8])));
        if (record[9] != null) {
            reservationRequestSummary.setAllocationState(
                    ReservationRequest.AllocationState.valueOf(
                            record[9].toString().trim()).toApi());
        }
        if (record[10] != null) {
            reservationRequestSummary.setExecutableState(
                    Executable.State.valueOf(
                            record[10].toString().trim()).toApi());
        }
        reservationRequestSummary.setReusedReservationRequestId(record[11] != null ?
                ObjectIdentifier.formatId(ObjectType.RESERVATION_REQUEST, record[11].toString()) : null);
        if (record[12] != null) {
            reservationRequestSummary.setLastReservationId(ObjectIdentifier.formatId(
                    ObjectType.RESERVATION, record[12].toString()));
        }
        String type = record[13].toString().trim();
        if (type.equals("ALIAS")) {
            reservationRequestSummary.setSpecificationType(ReservationRequestSummary.SpecificationType.ALIAS);
            reservationRequestSummary.setRoomName(record[16] != null ? record[16].toString() : null);
        }
        else if (type.equals("ROOM")) {
            reservationRequestSummary.setSpecificationType(ReservationRequestSummary.SpecificationType.ROOM);
            reservationRequestSummary.setRoomParticipantCount(
                    record[15] != null ? ((Number) record[15]).intValue() : null);
            reservationRequestSummary.setRoomHasRecordingService(record[16] != null && (Boolean) record[16]);
            reservationRequestSummary.setRoomHasRecordings(record[17] != null && (Boolean) record[17]);
            reservationRequestSummary.setRoomName(record[18] != null ? record[18].toString() : null);
        }
        else if (type.equals("PERMANENT_ROOM")) {
            reservationRequestSummary.setSpecificationType(ReservationRequestSummary.SpecificationType.PERMANENT_ROOM);
            reservationRequestSummary.setRoomHasRecordingService(record[16] != null && (Boolean) record[16]);
            reservationRequestSummary.setRoomHasRecordings(record[17] != null && (Boolean) record[17]);
            reservationRequestSummary.setRoomName(record[18] != null ? record[18].toString() : null);
        }
        else if (type.equals("USED_ROOM")) {
            reservationRequestSummary.setSpecificationType(ReservationRequestSummary.SpecificationType.USED_ROOM);
            reservationRequestSummary.setRoomParticipantCount(
                    record[15] != null ? ((Number) record[15]).intValue() : null);
            reservationRequestSummary.setRoomHasRecordingService(record[16] != null && (Boolean) record[16]);
            reservationRequestSummary.setRoomHasRecordings(record[17] != null && (Boolean) record[17]);
            reservationRequestSummary.setRoomName(record[18] != null ? record[18].toString() : null);
        }
        else if (type.equals("RESOURCE")) {
            reservationRequestSummary.setSpecificationType(ReservationRequestSummary.SpecificationType.RESOURCE);
            if (record[19] != null) {
                reservationRequestSummary.setResourceId(ObjectIdentifier.formatId(
                        ObjectType.RESOURCE, ((Number) record[19]).longValue()));
            }
        }
        else {
            reservationRequestSummary.setSpecificationType(ReservationRequestSummary.SpecificationType.OTHER);
        }
        if (record[14] != null) {
            String technologies = record[14].toString();
            if (!technologies.isEmpty()) {
                for (String technology : technologies.split(",")) {
                    reservationRequestSummary.addSpecificationTechnology(Technology.valueOf(technology.trim()));
                }
            }
        }
        if (record[20] != null) {
            reservationRequestSummary.setUsageExecutableState(
                    cz.cesnet.shongo.controller.booking.executable.Executable.State.valueOf(
                            record[20].toString().trim()).toApi());
        }
        if (record[21] != null) {
            reservationRequestSummary.setFutureSlotCount(((Number) record[21]).intValue());
        }
        return reservationRequestSummary;
    }

    /**
     * @param record
     * @return {@link ReservationSummary} from given {@code record}
     */
    private ReservationSummary getReservationSummary(Object[] record)
    {
        ReservationSummary reservationSummary = new ReservationSummary();
        reservationSummary.setId(ObjectIdentifier.formatId(ObjectType.RESERVATION, record[0].toString()));
        reservationSummary.setUserId(record[1] != null ? record[1].toString() : null);
        reservationSummary.setReservationRequestId(record[2] != null ?
                ObjectIdentifier.formatId(ObjectType.RESERVATION_REQUEST, record[2].toString()) : null);
        reservationSummary.setType(ReservationSummary.Type.valueOf(record[3].toString().trim()));
        reservationSummary.setSlot(new Interval(new DateTime(record[4]), new DateTime(record[5])));
        if (record[6] != null) {
            reservationSummary.setResourceId(ObjectIdentifier.formatId(ObjectType.RESOURCE, record[6].toString()));
        }
        if (record[7] != null) {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            ResourceManager resourceManager = new ResourceManager(entityManager);
            try {
                ForeignResources foreignResources = resourceManager.getForeignResources(((Number) record[7]).longValue());
                String domain = foreignResources.getDomain().getName();
                Long resourceId = foreignResources.getForeignResourceId();
                reservationSummary.setResourceId(ObjectIdentifier.formatId(domain, ObjectType.RESOURCE, resourceId));
            }
            finally {
                entityManager.close();
            }
        }
        if (record[8] != null) {
            reservationSummary.setRoomLicenseCount(record[8] != null ? ((Number) record[8]).intValue() : null);
        }
        if (record[9] != null) {
            reservationSummary.setRoomName(record[9] != null ? record[9].toString() : null);
        }
        if (record[10] != null) {
            reservationSummary.setAliasTypes(record[10] != null ? record[10].toString() : null);
        }
        if (record[11] != null) {
            reservationSummary.setValue(record[11] != null ? record[11].toString() : null);
        }
        if (record[12] != null) {
            reservationSummary.setReservationRequestDescription(record[12] != null ? record[12].toString() : null);
        }
        if (record[13] != null) {
            reservationSummary.setParentReservationRequestId(record[13] != null ?
                    ObjectIdentifier.formatId(ObjectType.RESERVATION_REQUEST, record[13].toString()) : null);
        }
        return reservationSummary;
    }

    /**
     * @param objectId      of object which should be checked for existence
     * @param entityManager which can be used
     * @return {@link cz.cesnet.shongo.PersistentObject} for given {@code objectId}
     * @throws cz.cesnet.shongo.CommonReportSet.ObjectNotExistsException
     *
     */
    private PersistentObject checkObjectExistence(ObjectIdentifier objectId, EntityManager entityManager)
            throws CommonReportSet.ObjectNotExistsException
    {
        PersistentObject object = entityManager.find(objectId.getObjectClass(), objectId.getPersistenceId());
        if (object == null) {
            ControllerReportSetHelper.throwObjectNotExistFault(objectId);
        }
        return object;
    }
}
