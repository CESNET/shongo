package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.ClassHelper;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.AclIdentityType;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.AvailabilityCheckRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationListRequest;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListRequest;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.booking.Allocation;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import cz.cesnet.shongo.controller.booking.executable.Executable;
import cz.cesnet.shongo.controller.booking.request.ReservationRequest;
import cz.cesnet.shongo.controller.booking.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.booking.reservation.ReservationManager;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.scheduler.*;
import cz.cesnet.shongo.controller.util.NativeQuery;
import cz.cesnet.shongo.controller.util.QueryFilter;
import cz.cesnet.shongo.report.Report;
import org.joda.time.DateTime;
import org.joda.time.Interval;

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
                   Component.AuthorizationAware
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

    @Override
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
    }

    @Override
    public String createReservationRequest(SecurityToken securityToken,
            cz.cesnet.shongo.controller.api.AbstractReservationRequest reservationRequestApi)
    {
        authorization.validate(securityToken);
        checkNotNull("reservationRequest", reservationRequestApi);

        // Check whether user can create reservation requests
        if (!authorization.hasSystemPermission(securityToken, SystemPermission.RESERVATION)) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault("create reservation request");
        }

        // Change user id (only root can do that)
        String userId = securityToken.getUserId();
        if (reservationRequestApi.getUserId() != null && authorization.isAdministrator(securityToken)) {
            userId = reservationRequestApi.getUserId();
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        try {
            // Check permission for reused reservation request
            String reusedReservationRequestId = reservationRequestApi.getReusedReservationRequestId();
            if (reusedReservationRequestId != null) {
                checkReusedReservationRequest(securityToken, reusedReservationRequestId, reservationRequestManager);
            }

            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest reservationRequest =
                    cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest.createFromApi(
                            reservationRequestApi, entityManager);
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

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();

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
            authorizationManager.commitTransaction();

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
            reservationRequestManager.delete(abstractReservationRequest, authorizationManager, true);

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();

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
            authorizationManager.commitTransaction();
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
            authorizationManager.commitTransaction();
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
                    reservationRequestIds.add(ObjectIdentifier.parseId(
                            reservationRequestId, ObjectType.RESERVATION_REQUEST));
                }
                queryFilter.addFilterParameter("reservationRequestIds", reservationRequestIds);
            }
            // Else other filters
            else {
                // List only latest versions of a reservation requests (no it's modifications or deleted requests)
                queryFilter.addFilter("reservation_request_summary.state = 'ACTIVE'");

                // List only child reservation requests for specified parent reservation request
                String parentReservationRequestId = request.getParentReservationRequestId();
                if (parentReservationRequestId != null) {
                    queryFilter.addFilter("reservation_request.parent_allocation_id IN("
                            + " SELECT DISTINCT abstract_reservation_request.allocation_id"
                            + " FROM abstract_reservation_request "
                            + " WHERE abstract_reservation_request.id = :parentReservationRequestId)");
                    queryFilter.addFilterParameter("parentReservationRequestId", ObjectIdentifier.parseId(
                            parentReservationRequestId, ObjectType.RESERVATION_REQUEST));
                }
                else {
                    // List only top reservation requests (no child requests created for a set of reservation requests)
                    queryFilter.addFilter("reservation_request.parent_allocation_id IS NULL");
                }

                // Filter description
                String description = request.getDescription();
                if (description != null) {
                    queryFilter.addFilter("reservation_request_summary.description LIKE :description");
                    queryFilter.addFilterParameter("description", "%" + description + "%");
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
                            ObjectIdentifier.parseId(specificationResourceId, ObjectType.RESOURCE));
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
                        Long persistenceId = ObjectIdentifier.parseId(
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
            }

            // Query order by
            String queryOrderBy;
            ReservationRequestListRequest.Sort sort = request.getSort();
            if (sort != null) {
                switch (sort) {
                    case ALIAS_ROOM_NAME:
                        queryOrderBy = "specification_summary.alias_room_name";
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

            if (!authorization.hasObjectPermission(securityToken, reservationRequest, ObjectPermission.READ)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read reservation request %s", objectId);
            }

            String historyQuery = NativeQuery.getNativeQuery(NativeQuery.RESERVATION_REQUEST_HISTORY);

            List history = entityManager.createNativeQuery(historyQuery)
                    .setParameter("reservationRequestId", objectId.getPersistenceId())
                    .getResultList();

            List<ReservationRequestSummary> reservationRequestSummaries = new LinkedList<ReservationRequestSummary>();
            for (Object historyItem : history) {
                Object[] historyItemData = (Object[]) historyItem;
                ReservationRequestSummary reservationRequestSummary = getReservationRequestSummary(historyItemData);
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
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read reservation %s", objectId);
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
        try {
            QueryFilter queryFilter = new QueryFilter("reservation_summary");

            // List only reservations which is current user permitted to read
            queryFilter.addFilterId(authorization, securityToken,
                    cz.cesnet.shongo.controller.booking.reservation.Reservation.class, ObjectPermission.READ);

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
                queryFilter.addFilter("reservation_summary.resource_id IN(:resourceIds)");
                Set<Long> resourceIds = new HashSet<Long>();
                for (String resourceId : request.getResourceIds()) {
                    resourceIds.add(ObjectIdentifier.parseId(resourceId, ObjectType.RESOURCE));
                }
                queryFilter.addFilterParameter("resourceIds", resourceIds);
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
            for (Object[] record : records) {
                ReservationSummary reservationSummary = getReservationSummary(record);
                response.addItem(reservationSummary);
            }
            return response;
        }
        finally {
            entityManager.close();
        }
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
     * @param record
     * @return {@link ReservationRequestSummary} from given {@code record}
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
            reservationRequestSummary.setResourceId(ObjectIdentifier.formatId(
                    ObjectType.RESOURCE, ((Number) record[19]).longValue()));
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
            reservationSummary.setRoomLicenseCount(record[7] != null ? ((Number) record[7]).intValue() : null);
        }
        if (record[8] != null) {
            reservationSummary.setRoomName(record[8] != null ? record[8].toString() : null);
        }
        if (record[9] != null) {
            reservationSummary.setAliasTypes(record[9] != null ? record[9].toString() : null);
        }
        if (record[10] != null) {
            reservationSummary.setValue(record[10] != null ? record[10].toString() : null);
        }
        return reservationSummary;
    }
}
