package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.ClassHelper;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.AliasReservation;
import cz.cesnet.shongo.controller.api.AliasSetSpecification;
import cz.cesnet.shongo.controller.api.AliasSpecification;
import cz.cesnet.shongo.controller.api.Reservation;
import cz.cesnet.shongo.controller.api.RoomSpecification;
import cz.cesnet.shongo.controller.api.Specification;
import cz.cesnet.shongo.controller.api.request.AvailabilityCheckRequest;
import cz.cesnet.shongo.controller.api.request.ListResponse;
import cz.cesnet.shongo.controller.api.request.ReservationListRequest;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListRequest;
import cz.cesnet.shongo.controller.authorization.*;
import cz.cesnet.shongo.controller.authorization.AclRecord;
import cz.cesnet.shongo.controller.cache.Cache;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.request.*;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.reservation.*;
import cz.cesnet.shongo.controller.resource.Alias;
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
    public void init(Configuration configuration)
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
        SecurityToken securityToken = request.getSecurityToken();
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        try {
            Interval interval = request.getSlot();
            String ignoredReservationRequestId = request.getIgnoredReservationRequestId();
            SchedulerContext schedulerContext = new SchedulerContext(cache, entityManager, authorization, interval);
            schedulerContext.setPurpose(request.getPurpose());
            if (ignoredReservationRequestId != null) {
                EntityIdentifier entityId = EntityIdentifier.parse(
                        ignoredReservationRequestId, EntityType.RESERVATION_REQUEST);
                cz.cesnet.shongo.controller.request.AbstractReservationRequest ignoredReservationRequest =
                        reservationRequestManager.get(entityId.getPersistenceId());
                for (cz.cesnet.shongo.controller.reservation.Reservation reservation :
                        ignoredReservationRequest.getAllocation().getReservations()) {
                    if (reservation.getSlot().overlaps(interval)) {
                        schedulerContext.addAvailableReservation(reservation, AvailableReservation.Type.REALLOCATABLE);
                    }
                }

                for (cz.cesnet.shongo.controller.request.ReservationRequest childReservationRequest :
                        ignoredReservationRequest.getAllocation().getChildReservationRequests()) {
                    for (cz.cesnet.shongo.controller.reservation.Reservation reservation :
                            childReservationRequest.getAllocation().getReservations()) {
                        if (reservation.getSlot().overlaps(interval)) {
                            schedulerContext.addAvailableReservation(reservation,
                                    AvailableReservation.Type.REALLOCATABLE);
                        }
                    }
                }
            }

            try {
                // Check reservation request reusability
                String reservationRequestId = request.getReservationRequestId();
                if (reservationRequestId != null) {
                    EntityIdentifier entityId = EntityIdentifier.parse(
                            reservationRequestId, EntityType.RESERVATION_REQUEST);
                    cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequest =
                            reservationRequestManager.get(entityId.getPersistenceId());
                    schedulerContext.getReusableReservation(reservationRequest.getAllocation());
                }

                // Check specification
                Specification specificationApi = request.getSpecification();
                if (specificationApi != null) {
                    cz.cesnet.shongo.controller.request.Specification specification =
                            cz.cesnet.shongo.controller.request.Specification
                                    .createFromApi(specificationApi, entityManager);
                    if (specification instanceof ReservationTaskProvider) {
                        try {
                            entityManager.getTransaction().begin();
                            ReservationTaskProvider reservationTaskProvider = (ReservationTaskProvider) specification;
                            ReservationTask reservationTask = reservationTaskProvider.createReservationTask(
                                    schedulerContext);
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
                return exception.getReport().toAllocationStateReport(authorization.isAdmin(securityToken) ?
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

        // Change user id (only root can do that)
        String userId = securityToken.getUserId();
        if (reservationRequestApi.getUserId() != null && authorization.isAdmin(securityToken)) {
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

            cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequest =
                    cz.cesnet.shongo.controller.request.AbstractReservationRequest.createFromApi(
                            reservationRequestApi, entityManager);
            reservationRequest.setCreatedBy(userId);
            reservationRequest.setUpdatedBy(userId);

            reservationRequestManager.create(reservationRequest);

            authorizationManager.createAclRecord(userId, reservationRequest, Role.OWNER);

            if (reusedReservationRequestId != null) {
                AbstractReservationRequest reusedReservationRequest = reservationRequestManager.get(
                        EntityIdentifier.parseId(AbstractReservationRequest.class, reusedReservationRequestId));
                if (reusedReservationRequest.getReusement().equals(ReservationRequestReusement.OWNED)) {
                    authorizationManager.createAclRecordsForChildEntity(reusedReservationRequest, reservationRequest);
                }
            }

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();

            return EntityIdentifier.formatId(reservationRequest);
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

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        String reservationRequestId = reservationRequestApi.getId();
        EntityIdentifier entityId = EntityIdentifier.parse(reservationRequestId, EntityType.RESERVATION_REQUEST);

        try {
            // Get old reservation request and check permissions and restrictions for modification
            cz.cesnet.shongo.controller.request.AbstractReservationRequest oldReservationRequest =
                    reservationRequestManager.get(entityId.getPersistenceId());
            if (!authorization.hasPermission(securityToken, oldReservationRequest, Permission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("modify reservation request %s", entityId);
            }
            switch (oldReservationRequest.getState()) {
                case MODIFIED:
                    throw new ControllerReportSet.ReservationRequestAlreadyModifiedException(entityId.toId());
                case DELETED:
                    throw new ControllerReportSet.ReservationRequestDeletedException(entityId.toId());
            }
            if (!isReservationRequestModifiable(oldReservationRequest)) {
                throw new ControllerReportSet.ReservationRequestNotModifiableException(entityId.toId());
            }

            // Change user id (only root can do that)
            String userId = securityToken.getUserId();
            if (reservationRequestApi.getUserId() != null && authorization.isAdmin(securityToken)) {
                userId = reservationRequestApi.getUserId();
            }

            // Check permission for reused reservation request
            String reusedReservationRequestId = reservationRequestApi.getReusedReservationRequestId();
            if (reusedReservationRequestId != null) {
                checkReusedReservationRequest(securityToken, reusedReservationRequestId, reservationRequestManager);
            }

            // Check if modified reservation request is of the same class
            AbstractReservationRequest newReservationRequest;
            Class<? extends AbstractReservationRequest> reservationRequestClass =
                    AbstractReservationRequest.getClassFromApi(reservationRequestApi.getClass());
            if (reservationRequestClass.isInstance(oldReservationRequest)) {
                // Update old detached reservation request (the changes will not be serialized to database)
                oldReservationRequest.fromApi(reservationRequestApi, entityManager);
                // Create new reservation request by cloning old reservation request
                newReservationRequest = oldReservationRequest.clone();
            }
            else {
                // Create new reservation request
                newReservationRequest = ClassHelper.createInstanceFromClass(reservationRequestClass);
                newReservationRequest.synchronizeFrom(oldReservationRequest);
                newReservationRequest.fromApi(reservationRequestApi, entityManager);
            }
            newReservationRequest.setCreatedBy(userId);
            newReservationRequest.setUpdatedBy(userId);
            oldReservationRequest.setUpdatedBy(userId);

            // Revert changes to old reservation request
            entityManager.clear();

            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            oldReservationRequest = reservationRequestManager.get(entityId.getPersistenceId());

            // Create new reservation request and update old reservation request
            reservationRequestManager.modify(oldReservationRequest, newReservationRequest);

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();

            return EntityIdentifier.formatId(newReservationRequest);
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
        if (reservationRequestId == null) {
            throw new IllegalArgumentException();
        }
        EntityIdentifier entityId = EntityIdentifier.parse(reservationRequestId, EntityType.RESERVATION_REQUEST);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.request.AbstractReservationRequest abstractReservationRequest =
                    reservationRequestManager.get(entityId.getPersistenceId());

            if(!abstractReservationRequest.getState().equals(
                    cz.cesnet.shongo.controller.request.AbstractReservationRequest.State.ACTIVE)) {
                throw new ControllerReportSet.ReservationRequestNotRevertibleException(
                        EntityIdentifier.formatId(abstractReservationRequest));
            }

            if (abstractReservationRequest instanceof cz.cesnet.shongo.controller.request.ReservationRequest) {
                cz.cesnet.shongo.controller.request.ReservationRequest reservationRequest =
                        (cz.cesnet.shongo.controller.request.ReservationRequest) abstractReservationRequest;
                if ( reservationRequest.getAllocationState().equals(
                        cz.cesnet.shongo.controller.request.ReservationRequest.AllocationState.ALLOCATED)) {
                    throw new ControllerReportSet.ReservationRequestNotRevertibleException(
                            EntityIdentifier.formatId(abstractReservationRequest));
                }
            }

            if (!authorization.hasPermission(securityToken, abstractReservationRequest, Permission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("revert reservation request %s", entityId);
            }

            // Set modified reservation request as ACTIVE
            cz.cesnet.shongo.controller.request.AbstractReservationRequest modifiedReservationRequest =
                    abstractReservationRequest.getModifiedReservationRequest();
            modifiedReservationRequest.setState(
                    cz.cesnet.shongo.controller.request.AbstractReservationRequest.State.ACTIVE);
            modifiedReservationRequest.getAllocation().setReservationRequest(modifiedReservationRequest);
            reservationRequestManager.update(modifiedReservationRequest);

            // Revert the modification
            reservationRequestManager.delete(abstractReservationRequest, authorizationManager, true);

            entityManager.getTransaction().commit();
            authorizationManager.commitTransaction();

            return EntityIdentifier.formatId(modifiedReservationRequest);
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
        if (reservationRequestId == null) {
            throw new IllegalArgumentException();
        }
        EntityIdentifier entityId = EntityIdentifier.parse(reservationRequestId, EntityType.RESERVATION_REQUEST);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequest =
                    reservationRequestManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(securityToken, reservationRequest, Permission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("delete reservation request %s", entityId);
            }
            switch (reservationRequest.getState()) {
                case MODIFIED:
                    throw new ControllerReportSet.ReservationRequestNotDeletableException(entityId.toId());
                case DELETED:
                    throw new ControllerReportSet.ReservationRequestDeletedException(entityId.toId());
            }
            ReservationManager reservationManager = new ReservationManager(entityManager);
            if (!isReservationRequestDeletable(reservationRequest, reservationManager)) {
                throw new ControllerReportSet.ReservationRequestNotDeletableException(
                        EntityIdentifier.formatId(reservationRequest));
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
        EntityIdentifier entityId = EntityIdentifier.parse(reservationRequestId, EntityType.RESERVATION_REQUEST);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager, authorization);
        try {
            authorizationManager.beginTransaction();
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.request.AbstractReservationRequest abstractReservationRequest =
                    reservationRequestManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(securityToken, abstractReservationRequest, Permission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("update reservation request %s", entityId);
            }

            // Update reservation requests
            if (abstractReservationRequest instanceof cz.cesnet.shongo.controller.request.ReservationRequest) {
                cz.cesnet.shongo.controller.request.ReservationRequest reservationRequest =
                        (cz.cesnet.shongo.controller.request.ReservationRequest) abstractReservationRequest;
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
            for (cz.cesnet.shongo.controller.request.ReservationRequest reservationRequest :
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
        SecurityToken securityToken = request.getSecurityToken();
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            QueryFilter queryFilter = new QueryFilter("reservation_request_summary", true);

            // List only reservation requests which is current user permitted to read
            queryFilter.addFilterId("allocation_id", authorization, securityToken,
                    AclRecord.EntityType.ALLOCATION, Permission.READ);

            // List only reservation requests which are requested
            if (request.getReservationRequestIds().size() > 0) {
                queryFilter.addFilter("reservation_request_summary.id IN (:reservationRequestIds)");
                Set<Long> reservationRequestIds = new HashSet<Long>();
                for (String reservationRequestId : request.getReservationRequestIds()) {
                    reservationRequestIds.add(EntityIdentifier.parseId(
                            cz.cesnet.shongo.controller.request.AbstractReservationRequest.class,
                            reservationRequestId));
                }
                queryFilter.addFilterParameter("reservationRequestIds", reservationRequestIds);
            }

            // List only child reservation requests for specified parent reservation request
            String parentReservationRequestId = request.getParentReservationRequestId();
            if (parentReservationRequestId != null) {
                queryFilter.addFilter("reservation_request.parent_allocation_id IN("
                        + " SELECT DISTINCT abstract_reservation_request.allocation_id"
                        + " FROM abstract_reservation_request "
                        + " WHERE abstract_reservation_request.id = :parentReservationRequestId)");
                queryFilter.addFilterParameter("parentReservationRequestId", EntityIdentifier.parseId(
                        cz.cesnet.shongo.controller.request.AbstractReservationRequest.class,
                        parentReservationRequestId));
            }
            else {
                // List only top reservation requests (no child requests created for a set of reservation requests)
                queryFilter.addFilter("reservation_request.parent_allocation_id IS NULL");
            }

            // List only reservation requests which specifies given technologies
            if (request.getTechnologies().size() > 0) {
                queryFilter.addFilter("reservation_request_summary.id IN ("
                        + "  SELECT DISTINCT abstract_reservation_request.id"
                        + "  FROM abstract_reservation_request"
                        + "  LEFT JOIN specification_technologies ON specification_technologies.specification_id = "
                        + "            abstract_reservation_request.specification_id"
                        + "  WHERE specification_technologies.technologies IN(:technologies))");
                queryFilter.addFilterParameter("technologies", request.getTechnologies());
            }

            // List only reservation requests which has specification of given classes
            if (request.getSpecificationClasses().size() > 0) {
                StringBuilder leftJoinBuilder = new StringBuilder();
                StringBuilder whereBuilder = new StringBuilder();
                for (Class<? extends Specification> type : request.getSpecificationClasses()) {
                    leftJoinBuilder.append(" LEFT JOIN ");
                    if (whereBuilder.length() > 0) {
                        whereBuilder.append(" OR ");
                    }
                    if (type.equals(RoomSpecification.class)) {
                        leftJoinBuilder.append("room_specification ON room_specification.id");
                        whereBuilder.append("room_specification.id");
                    }
                    else if (type.equals(AliasSpecification.class)) {
                        leftJoinBuilder.append("alias_specification ON alias_specification.id");
                        whereBuilder.append("alias_specification.id");
                    }
                    else if (type.equals(AliasSetSpecification.class)) {
                        leftJoinBuilder.append("alias_set_specification ON alias_set_specification.id");
                        whereBuilder.append("alias_set_specification.id");
                    }
                    else {
                        throw new TodoImplementException(type);
                    }
                    leftJoinBuilder.append(" = abstract_reservation_request.specification_id");
                    whereBuilder.append(" IS NOT NULL");
                }
                StringBuilder filterBuilder = new StringBuilder();
                filterBuilder.append("reservation_request_summary.id IN (");
                filterBuilder.append(" SELECT abstract_reservation_request.id FROM abstract_reservation_request");
                filterBuilder.append(leftJoinBuilder);
                filterBuilder.append(" WHERE ");
                filterBuilder.append(whereBuilder);
                filterBuilder.append(")");
                queryFilter.addFilter(filterBuilder.toString());
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
                    Long persistenceId = EntityIdentifier.parseId(
                            cz.cesnet.shongo.controller.request.ReservationRequest.class, reusedReservationRequestId);
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

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(reservationRequestId, EntityType.RESERVATION_REQUEST);
        try {
            cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequest =
                    reservationRequestManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(securityToken, reservationRequest, Permission.READ)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read reservation request %s", entityId);
            }

            return reservationRequest.toApi(authorization.isAdmin(securityToken));
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

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(reservationRequestId, EntityType.RESERVATION_REQUEST);
        try {
            cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequest =
                    reservationRequestManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(securityToken, reservationRequest, Permission.READ)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read reservation request %s", entityId);
            }

            String historyQuery = NativeQuery.getNativeQuery(NativeQuery.RESERVATION_REQUEST_HISTORY);

            List history = entityManager.createNativeQuery(historyQuery)
                    .setParameter("reservationRequestId", entityId.getPersistenceId())
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
    public Reservation getReservation(SecurityToken securityToken, String reservationId)
    {
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationManager reservationManager = new ReservationManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(reservationId, EntityType.RESERVATION);

        try {
            cz.cesnet.shongo.controller.reservation.Reservation reservation =
                    reservationManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(securityToken, reservation, Permission.READ)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read reservation %s", entityId);
            }

            return reservation.toApi(authorization.isAdmin(securityToken));
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public ListResponse<Reservation> listReservations(ReservationListRequest request)
    {
        SecurityToken securityToken = request.getSecurityToken();
        authorization.validate(securityToken);

        EntityManager entityManager = entityManagerFactory.createEntityManager();

        try {
            QueryFilter queryFilter = new QueryFilter("reservation");

            // List only reservations which is current user permitted to read
            queryFilter.addFilterId(authorization, securityToken, AclRecord.EntityType.RESERVATION, Permission.READ);

            // List only reservations which are requested
            if (request.getReservationIds().size() > 0) {
                queryFilter.addFilter("reservation.id IN (:reservationIds)");
                Set<Long> reservationIds = new HashSet<Long>();
                for (String reservationId : request.getReservationIds()) {
                    reservationIds.add(EntityIdentifier.parseId(
                            cz.cesnet.shongo.controller.reservation.Reservation.class, reservationId));
                }
                queryFilter.addFilterParameter("reservationIds", reservationIds);
            }

            // List only reservations of requested classes
            Set<Class<? extends Reservation>> reservationApiClasses = request.getReservationClasses();
            if (reservationApiClasses.size() > 0) {
                if (reservationApiClasses.contains(AliasReservation.class)) {
                    // List only reservations of given classes or raw reservations which have alias reservation as child
                    queryFilter.addFilter("reservation IN ("
                            + "   SELECT mainReservation FROM Reservation mainReservation"
                            + "   LEFT JOIN mainReservation.childReservations childReservation"
                            + "   WHERE TYPE(mainReservation) IN(:classes)"
                            + "      OR (TYPE(mainReservation) = :raw AND TYPE(childReservation) = :alias)"
                            + " )");
                    queryFilter.addFilterParameter("alias",
                            cz.cesnet.shongo.controller.reservation.AliasReservation.class);
                    queryFilter.addFilterParameter("raw", cz.cesnet.shongo.controller.reservation.Reservation.class);
                }
                else {
                    // List only reservations of given classes
                    queryFilter.addFilter("TYPE(reservation) IN(:classes)");
                }
                Set<Class<? extends cz.cesnet.shongo.controller.reservation.Reservation>> reservationClasses =
                        new HashSet<Class<? extends cz.cesnet.shongo.controller.reservation.Reservation>>();
                for (Class<? extends Reservation> reservationApiClass : reservationApiClasses) {
                    reservationClasses.add(cz.cesnet.shongo.controller.reservation.Reservation.getClassFromApi(
                            reservationApiClass));
                }
                queryFilter.addFilterParameter("classes", reservationClasses);
            }

            // List only reservations allocated for requested reservation request
            if (request.getReservationRequestId() != null) {
                // List only reservations which are allocated for reservation request with given id or child reservation requests
                queryFilter.addFilter("reservation.allocation IS NOT NULL AND (reservation.allocation IN ("
                        + "   SELECT allocation FROM AbstractReservationRequest reservationRequest"
                        + "   LEFT JOIN reservationRequest.allocation allocation"
                        + "   WHERE reservationRequest.id = :reservationRequestId"
                        + " ) OR reservation.allocation IN ("
                        + "   SELECT childAllocation FROM AbstractReservationRequest reservationRequest"
                        + "   LEFT JOIN reservationRequest.allocation allocation"
                        + "   LEFT JOIN allocation.childReservationRequests childReservationRequest"
                        + "   LEFT JOIN childReservationRequest.allocation childAllocation"
                        + "   WHERE reservationRequest.id = :reservationRequestId"
                        + " ))");
                queryFilter.addFilterParameter("reservationRequestId", EntityIdentifier.parseId(
                        cz.cesnet.shongo.controller.request.AbstractReservationRequest.class,
                        request.getReservationRequestId()));
            }

            // Sort query part
            String queryOrderBy;
            ReservationListRequest.Sort sort = request.getSort();
            if (sort != null) {
                switch (sort) {
                    case SLOT:
                        queryOrderBy = "reservation.slotStart";
                        break;
                    default:
                        throw new TodoImplementException(sort);
                }
            }
            else {
                queryOrderBy = "reservation.id";
            }
            Boolean sortDescending = request.getSortDescending();
            sortDescending = (sortDescending != null ? sortDescending : false);
            if (sortDescending) {
                queryOrderBy = queryOrderBy + " DESC";
            }

            String query = "SELECT reservation FROM Reservation reservation"
                    + " WHERE " + queryFilter.toQueryWhere()
                    + " ORDER BY " + queryOrderBy;

            ListResponse<Reservation> response = new ListResponse<Reservation>();
            List<cz.cesnet.shongo.controller.reservation.Reservation> reservations = performListRequest(
                    query, queryFilter, cz.cesnet.shongo.controller.reservation.Reservation.class,
                    request, response, entityManager);

            // Filter reservations by technologies
            Set<Technology> technologies = request.getTechnologies();
            if (technologies.size() > 0) {
                Iterator<cz.cesnet.shongo.controller.reservation.Reservation> iterator = reservations.iterator();
                while (iterator.hasNext()) {
                    cz.cesnet.shongo.controller.reservation.Reservation reservation = iterator.next();
                    if (reservation instanceof cz.cesnet.shongo.controller.reservation.AliasReservation) {
                        cz.cesnet.shongo.controller.reservation.AliasReservation aliasReservation = (cz.cesnet.shongo.controller.reservation.AliasReservation) reservation;
                        boolean technologyFound = false;
                        for (Alias alias : aliasReservation.getAliases()) {
                            if (technologies.contains(alias.getTechnology())) {
                                technologyFound = true;
                                break;
                            }
                        }
                        if (!technologyFound) {
                            iterator.remove();
                        }
                    }
                    else if (reservation.getClass().equals(cz.cesnet.shongo.controller.reservation.Reservation.class)) {
                        boolean technologyFound = false;
                        for (cz.cesnet.shongo.controller.reservation.Reservation childReservation : reservation
                                .getChildReservations()) {
                            if (childReservation instanceof cz.cesnet.shongo.controller.reservation.AliasReservation) {
                                cz.cesnet.shongo.controller.reservation.AliasReservation childAliasReservation = (cz.cesnet.shongo.controller.reservation.AliasReservation) childReservation;
                                for (Alias alias : childAliasReservation.getAliases()) {
                                    if (technologies.contains(alias.getTechnology())) {
                                        technologyFound = true;
                                        break;
                                    }
                                }
                            }
                            else {
                                throw new TodoImplementException(childReservation.getClass());
                            }
                        }
                        if (!technologyFound) {
                            iterator.remove();
                        }
                    }
                    else {
                        throw new TodoImplementException(reservation.getClass());
                    }
                }
            }

            // Fill reservations to response
            for (cz.cesnet.shongo.controller.reservation.Reservation reservation : reservations) {
                response.addItem(reservation.toApi(authorization.isAdmin(securityToken)));
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
        EntityIdentifier entityId = EntityIdentifier.parse(reusedReservationRequestId);
        cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequest =
                reservationRequestManager.get(entityId.getPersistenceId());
        if (!authorization.hasPermission(securityToken, reservationRequest, Permission.PROVIDE_RESERVATION_REQUEST)) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault(
                    "provide reservation request %s", entityId);
        }
    }

    /**
     * Check whether {@code abstractReservationRequest} can be modified.
     *
     * @param abstractReservationRequest
     * @return true when the given {@code abstractReservationRequest} can be modified, otherwise false
     */
    private boolean isReservationRequestModifiable(
            cz.cesnet.shongo.controller.request.AbstractReservationRequest abstractReservationRequest)
    {
        Allocation allocation = abstractReservationRequest.getAllocation();

        // Check if reservation request is not created by controller
        if (abstractReservationRequest instanceof cz.cesnet.shongo.controller.request.ReservationRequest) {
            cz.cesnet.shongo.controller.request.ReservationRequest reservationRequestImpl =
                    (cz.cesnet.shongo.controller.request.ReservationRequest) abstractReservationRequest;
            if (reservationRequestImpl.getParentAllocation() != null) {
                return false;
            }
        }

        // Check child reservation requests
        for (cz.cesnet.shongo.controller.request.ReservationRequest reservationRequestImpl :
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
            cz.cesnet.shongo.controller.request.AbstractReservationRequest abstractReservationRequest,
            ReservationManager reservationManager)
    {
        Allocation allocation = abstractReservationRequest.getAllocation();

        // Check if reservation request is not created by controller
        if (abstractReservationRequest instanceof cz.cesnet.shongo.controller.request.ReservationRequest) {
            cz.cesnet.shongo.controller.request.ReservationRequest reservationRequestImpl =
                    (cz.cesnet.shongo.controller.request.ReservationRequest) abstractReservationRequest;
            if (reservationRequestImpl.getParentAllocation() != null) {
                return false;
            }
        }

        // Check allocated reservations
        if (reservationManager.isAllocationReused(allocation)) {
            return false;
        }

        // Check child reservation requests
        for (cz.cesnet.shongo.controller.request.ReservationRequest reservationRequestImpl :
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
        reservationRequestSummary.setId(EntityIdentifier.formatId(
                EntityType.RESERVATION_REQUEST, record[0].toString()));
        reservationRequestSummary.setType(ReservationRequestType.valueOf(record[1].toString().trim()));
        reservationRequestSummary.setDateTime(new DateTime(record[2]));
        reservationRequestSummary.setUserId(record[3].toString());
        reservationRequestSummary.setDescription(record[4] != null ? record[4].toString() : null);
        reservationRequestSummary.setPurpose(ReservationRequestPurpose.valueOf(record[5].toString().trim()));
        reservationRequestSummary.setEarliestSlot(new Interval(
                new DateTime(record[6]), new DateTime(record[7])));
        if (record[8] != null) {
            reservationRequestSummary.setAllocationState(
                    cz.cesnet.shongo.controller.request.ReservationRequest.AllocationState.valueOf(
                            record[8].toString().trim()).toApi());
        }
        if (record[9] != null) {
            reservationRequestSummary.setExecutableState(
                    cz.cesnet.shongo.controller.executor.Executable.State.valueOf(
                            record[9].toString().trim()).toApi());
        }
        reservationRequestSummary.setReusedReservationRequestId(record[10] != null ?
                EntityIdentifier.formatId(EntityType.RESERVATION_REQUEST, record[10].toString()) : null);
        if (record[11] != null) {
            reservationRequestSummary.setLastReservationId(EntityIdentifier.formatId(
                    EntityType.RESERVATION, record[11].toString()));
        }
        String type = record[12].toString().trim();
        if (type.equals("ALIAS")) {
            ReservationRequestSummary.AliasSpecification aliasSpecification =
                    new ReservationRequestSummary.AliasSpecification();
            aliasSpecification.setAliasType(AliasType.ROOM_NAME);
            aliasSpecification.setValue(record[15] != null ? record[15].toString() : null);
            reservationRequestSummary.setSpecification(aliasSpecification);
        }
        else if (type.equals("ROOM")) {
            ReservationRequestSummary.RoomSpecification roomSpecification =
                    new ReservationRequestSummary.RoomSpecification();
            if (record[14] != null) {
                roomSpecification.setParticipantCount(((Number) record[14]).intValue());
            }
            reservationRequestSummary.setSpecification(roomSpecification);
        }
        else if (type.equals("RESOURCE")) {
            ReservationRequestSummary.ResourceSpecification resourceSpecification =
                    new ReservationRequestSummary.ResourceSpecification();
            if (record[16] != null) {
                resourceSpecification.setResourceId(EntityIdentifier.formatId(
                        EntityType.RESOURCE, ((Number) record[16]).longValue()));
            }
            reservationRequestSummary.setSpecification(resourceSpecification);
        }
        if (record[13] != null) {
            String technologies = record[13].toString();
            if (!technologies.isEmpty()) {
                for (String technology : technologies.split(",")) {
                    reservationRequestSummary.addSpecificationTechnology(Technology.valueOf(technology.trim()));
                }
            }
        }
        if (record[17] != null) {
            reservationRequestSummary.setUsageExecutableState(
                    cz.cesnet.shongo.controller.executor.Executable.State.valueOf(
                            record[17].toString().trim()).toApi());
        }
        if (record[18] != null) {
            reservationRequestSummary.setFutureSlotCount(((Number) record[18]).intValue());
        }
        return reservationRequestSummary;
    }
}
