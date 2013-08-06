package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.ClassHelper;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.request.*;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.request.AbstractReservationRequest;
import cz.cesnet.shongo.controller.request.Allocation;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.scheduler.AvailableReservation;
import cz.cesnet.shongo.controller.scheduler.SchedulerContext;
import cz.cesnet.shongo.controller.scheduler.SchedulerException;
import cz.cesnet.shongo.controller.scheduler.SpecificationCheckAvailability;
import cz.cesnet.shongo.controller.util.QueryFilter;
import cz.cesnet.shongo.controller.util.NativeQuery;
import cz.cesnet.shongo.report.Report;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.math.BigInteger;
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
    public ReservationServiceImpl()
    {
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
        authorization.validate(request.getSecurityToken());

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        try {
            Interval interval = request.getSlot();
            String providedReservationRequestId = request.getProvidedReservationRequestId();
            SchedulerContext schedulerContext = new SchedulerContext(null, entityManager, interval);
            if (providedReservationRequestId != null) {
                EntityIdentifier entityId = EntityIdentifier.parse(
                        providedReservationRequestId, EntityType.RESERVATION_REQUEST);
                cz.cesnet.shongo.controller.request.AbstractReservationRequest providedReservationRequest =
                        reservationRequestManager.get(entityId.getPersistenceId());
                for (cz.cesnet.shongo.controller.reservation.Reservation reservation :
                        providedReservationRequest.getAllocation().getReservations()) {
                    if (reservation.getSlot().overlaps(interval)) {
                        schedulerContext.addAvailableReservation(reservation, AvailableReservation.Type.REALLOCATABLE);
                    }
                }
            }

            // Check specification
            Specification specificationApi = request.getSpecification();
            if (specificationApi != null) {
                cz.cesnet.shongo.controller.request.Specification specification =
                        cz.cesnet.shongo.controller.request.Specification
                                .createFromApi(specificationApi, entityManager);
                Throwable cause = null;
                if (specification instanceof SpecificationCheckAvailability) {
                    SpecificationCheckAvailability checkAvailability = (SpecificationCheckAvailability) specification;
                    try {
                        checkAvailability.checkAvailability(schedulerContext);
                    }
                    catch (SchedulerException exception) {
                        // Specification cannot be allocated in requested time slot
                        return exception.getReport().getMessageRecursive(Report.MessageType.USER);
                    }
                }
                else {
                    throw new RuntimeException(String.format("Specification '%s' cannot be checked for availability.",
                            specificationApi.getClass().getSimpleName()), cause);
                }
            }

            // Check reservation request reusability
            String reservationRequestId = request.getReservationRequestId();
            if (reservationRequestId != null) {
                EntityIdentifier entityId = EntityIdentifier.parse(
                        reservationRequestId, EntityType.RESERVATION_REQUEST);
                cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequest =
                        reservationRequestManager.get(entityId.getPersistenceId());
                try {
                    Scheduler.getProvidedReservation(reservationRequest, schedulerContext);
                }
                catch (SchedulerException exception) {
                    // Reservation request cannot be provided in requested time slot
                    return exception.getReport().getMessageRecursive(Report.MessageType.USER);
                }
            }

            // Request is available
            return Boolean.TRUE;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public String createReservationRequest(SecurityToken token,
            cz.cesnet.shongo.controller.api.AbstractReservationRequest reservationRequestApi)
    {
        String userId = authorization.validate(token);

        // Change user id (only root can do that)
        if (reservationRequestApi.getUserId() != null && authorization.isAdmin(userId)) {
            userId = reservationRequestApi.getUserId();
        }

        // Check permission for provided reservation request
        checkProvidedReservationRequest(userId, reservationRequestApi.getProvidedReservationRequestId());

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager);
        try {
            authorizationManager.beginTransaction(authorization);
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequest =
                    cz.cesnet.shongo.controller.request.AbstractReservationRequest.createFromApi(
                            reservationRequestApi, entityManager);
            reservationRequest.setCreatedBy(userId);
            reservationRequest.setUpdatedBy(userId);

            reservationRequestManager.create(reservationRequest);

            authorizationManager.createAclRecord(userId, reservationRequest, Role.OWNER);

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
    public String modifyReservationRequest(SecurityToken token,
            cz.cesnet.shongo.controller.api.AbstractReservationRequest reservationRequestApi)
    {
        String userId = authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager);
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        String reservationRequestId = reservationRequestApi.getId();
        EntityIdentifier entityId = EntityIdentifier.parse(reservationRequestId, EntityType.RESERVATION_REQUEST);

        try {
            // Get old reservation request and check permissions and restrictions for modification
            cz.cesnet.shongo.controller.request.AbstractReservationRequest oldReservationRequest =
                    reservationRequestManager.get(entityId.getPersistenceId());
            if (!authorization.hasPermission(userId, entityId, Permission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("modify reservation request %s", entityId);
            }
            switch (oldReservationRequest.getState()) {
                case MODIFIED:
                    throw new ControllerReportSet.ReservationRequestAlreadyModifiedException(entityId.toId());
                case DELETED:
                    throw new ControllerReportSet.ReservationRequestDeletedException(entityId.toId());
            }
            ReservationManager reservationManager = new ReservationManager(entityManager);
            if (!isModifiableReservationRequest(oldReservationRequest, reservationManager)) {
                throw new ControllerReportSet.ReservationRequestNotModifiableException(entityId.toId());
            }

            // Check permission for provided reservation request
            checkProvidedReservationRequest(userId, reservationRequestApi.getProvidedReservationRequestId());

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

            authorizationManager.beginTransaction(authorization);
            entityManager.getTransaction().begin();

            oldReservationRequest = reservationRequestManager.get(entityId.getPersistenceId());

            // Create new reservation request and update old reservation request
            reservationRequestManager.modify(oldReservationRequest, newReservationRequest);

            // Copy ACL records
            authorizationManager.copyAclRecords(oldReservationRequest, newReservationRequest);

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
    public String revertReservationRequest(SecurityToken token, String reservationRequestId)
    {
        String userId = authorization.validate(token);
        if (reservationRequestId == null) {
            throw new IllegalArgumentException();
        }
        EntityIdentifier entityId = EntityIdentifier.parse(reservationRequestId, EntityType.RESERVATION_REQUEST);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager);
        try {
            authorizationManager.beginTransaction(authorization);
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.request.AbstractReservationRequest abstractReservationRequest =
                    reservationRequestManager.get(entityId.getPersistenceId());

            if (!(abstractReservationRequest instanceof cz.cesnet.shongo.controller.request.ReservationRequest)) {
                throw new ControllerReportSet.ReservationRequestNotRevertibleException(
                        EntityIdentifier.formatId(abstractReservationRequest));
            }

            cz.cesnet.shongo.controller.request.ReservationRequest reservationRequest =
                    (cz.cesnet.shongo.controller.request.ReservationRequest) abstractReservationRequest;
            if (reservationRequest.getAllocationState().equals(
                    cz.cesnet.shongo.controller.request.ReservationRequest.AllocationState.ALLOCATED)
                    || !reservationRequest.getState().equals(
                    cz.cesnet.shongo.controller.request.AbstractReservationRequest.State.ACTIVE)) {
                throw new ControllerReportSet.ReservationRequestNotRevertibleException(
                        EntityIdentifier.formatId(abstractReservationRequest));
            }

            if (!authorization.hasPermission(userId, entityId, Permission.WRITE)) {
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
    public void deleteReservationRequest(SecurityToken token, String reservationRequestId)
    {
        String userId = authorization.validate(token);
        if (reservationRequestId == null) {
            throw new IllegalArgumentException();
        }
        EntityIdentifier entityId = EntityIdentifier.parse(reservationRequestId, EntityType.RESERVATION_REQUEST);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager);
        try {
            authorizationManager.beginTransaction(authorization);
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequest =
                    reservationRequestManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(userId, entityId, Permission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("delete reservation request %s", entityId);
            }
            switch (reservationRequest.getState()) {
                case MODIFIED:
                    throw new ControllerReportSet.ReservationRequestNotModifiableException(entityId.toId());
                case DELETED:
                    throw new ControllerReportSet.ReservationRequestDeletedException(entityId.toId());
            }
            ReservationManager reservationManager = new ReservationManager(entityManager);
            if (!isModifiableReservationRequest(reservationRequest, reservationManager)) {
                throw new ControllerReportSet.ReservationRequestNotModifiableException(
                        EntityIdentifier.formatId(reservationRequest));
            }

            reservationRequest.setUpdatedBy(userId);
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
    public void updateReservationRequest(SecurityToken token, String reservationRequestId)
    {
        String userId = authorization.validate(token);
        EntityIdentifier entityId = EntityIdentifier.parse(reservationRequestId, EntityType.RESERVATION_REQUEST);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager);
        try {
            authorizationManager.beginTransaction(authorization);
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.request.AbstractReservationRequest abstractReservationRequest =
                    reservationRequestManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(userId, entityId, Permission.WRITE)) {
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
        String userId = authorization.validate(request.getSecurityToken());

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            QueryFilter queryFilter = new QueryFilter("reservation_request_summary", true);

            // List only reservation requests which is current user permitted to read
            queryFilter.addIds(authorization, userId, EntityType.RESERVATION_REQUEST, Permission.READ);

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

            // List only reservation requests which specifies given technologies
            if (request.getTechnologies().size() > 0) {
                queryFilter.addFilter("reservation_request_summary.id IN ("
                        + "  SELECT DISTINCT abstract_reservation_request.id"
                        + "  FROM abstract_reservation_request"
                        + "  LEFT JOIN specification_technologies ON specification_technologies.specification_id = "
                        + "            abstract_reservation_request.specification_id"
                        + "  WHERE specification_technologies.technologies IN(:technologies)"
                        + ")");
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

            String providedReservationRequestId = request.getProvidedReservationRequestId();
            if (providedReservationRequestId != null) {
                if (providedReservationRequestId.equals(ReservationRequestListRequest.FILTER_EMPTY)) {
                    // List only reservation requests which hasn't got provided any reservation request
                    queryFilter.addFilter("reservation_request_summary.provided_reservation_request_id IS NULL");
                }
                else if (providedReservationRequestId.equals(ReservationRequestListRequest.FILTER_NOT_EMPTY)) {
                    // List only reservation requests which got provided any reservation request
                    queryFilter.addFilter("reservation_request_summary.provided_reservation_request_id IS NOT NULL");
                }
                else {
                    // List only reservation requests which got provided given reservation request
                    Long persistenceId = EntityIdentifier.parseId(
                            cz.cesnet.shongo.controller.request.ReservationRequest.class, providedReservationRequestId);
                    queryFilter.addFilter("reservation_request_summary.provided_reservation_request_id = "
                            + ":providedReservationRequestId");
                    queryFilter.addFilterParameter("providedReservationRequestId", persistenceId);
                }
            }

            // Query order by
            String queryOrderBy;
            ReservationRequestListRequest.Sort sort = request.getSort();
            if (sort != null) {
                switch (sort) {
                    case DATETIME:
                        queryOrderBy = "reservation_request_summary.created_at";
                        break;
                    default:
                        throw new TodoImplementException(sort.toString());
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
    public cz.cesnet.shongo.controller.api.AbstractReservationRequest getReservationRequest(SecurityToken token,
            String reservationRequestId)
    {
        String userId = authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(reservationRequestId, EntityType.RESERVATION_REQUEST);
        try {
            cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequest =
                    reservationRequestManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(userId, entityId, Permission.READ)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read reservation request %s", entityId);
            }

            return reservationRequest.toApi(authorization.isAdmin(userId));
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public List<ReservationRequestSummary> getReservationRequestHistory(SecurityToken token,
            String reservationRequestId)
    {
        String userId = authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityIdentifier entityId = EntityIdentifier.parse(reservationRequestId, EntityType.RESERVATION_REQUEST);
        try {
            if (!authorization.hasPermission(userId, entityId, Permission.READ)) {
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
    public ListResponse<cz.cesnet.shongo.controller.api.ReservationRequest> listChildReservationRequests(
            ChildReservationRequestListRequest request)
    {
        String userId = authorization.validate(request.getSecurityToken());

        EntityIdentifier reservationRequestId =
                EntityIdentifier.parse(request.getReservationRequestId(), EntityType.RESERVATION_REQUEST);
        if (!authorization.hasPermission(userId, reservationRequestId, Permission.READ)) {
            ControllerReportSetHelper.throwSecurityNotAuthorizedFault(
                    "read reservation request %s", reservationRequestId);
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            QueryFilter queryFilter = new QueryFilter("reservationRequest");

            // List only child reservation requests which are ACTIVE
            queryFilter.addFilter("childReservationRequest.state = :activeState", "activeState",
                    cz.cesnet.shongo.controller.request.AbstractReservationRequest.State.ACTIVE);

            // List only child reservation requests for specified parent reservation request
            queryFilter.addFilter("reservationRequest.id = :reservationRequestId", "reservationRequestId",
                    reservationRequestId.getPersistenceId());

            String query = "SELECT childReservationRequest FROM AbstractReservationRequest reservationRequest"
                    + " LEFT JOIN reservationRequest.allocation.childReservationRequests childReservationRequest"
                    + " WHERE " + queryFilter.toQueryWhere()
                    + " ORDER BY childReservationRequest.slotStart DESC";
            ListResponse<cz.cesnet.shongo.controller.api.ReservationRequest> response =
                    new ListResponse<cz.cesnet.shongo.controller.api.ReservationRequest>();
            List<cz.cesnet.shongo.controller.request.ReservationRequest> reservationRequests = performListRequest(
                    query, queryFilter, cz.cesnet.shongo.controller.request.ReservationRequest.class,
                    request, response, entityManager);

            // Fill reservation requests to response
            for (cz.cesnet.shongo.controller.request.ReservationRequest reservationRequest : reservationRequests) {
                response.addItem(reservationRequest.toApi(authorization.isAdmin(userId)));
            }
            return response;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public Reservation getReservation(SecurityToken token, String reservationId)
    {
        String userId = authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationManager reservationManager = new ReservationManager(entityManager);
        EntityIdentifier entityId = EntityIdentifier.parse(reservationId, EntityType.RESERVATION);

        try {
            cz.cesnet.shongo.controller.reservation.Reservation reservation =
                    reservationManager.get(entityId.getPersistenceId());

            if (!authorization.hasPermission(userId, entityId, Permission.READ)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("read reservation %s", entityId);
            }

            return reservation.toApi(authorization.isAdmin(userId));
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public ListResponse<Reservation> listReservations(ReservationListRequest request)
    {
        String userId = authorization.validate(request.getSecurityToken());

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationManager reservationManager = new ReservationManager(entityManager);

        try {
            QueryFilter queryFilter = new QueryFilter("reservation");

            // List only reservations which is current user permitted to read
            queryFilter.addIds(authorization, userId, EntityType.RESERVATION, Permission.READ);

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
                    queryFilter.addFilterParameter("alias", cz.cesnet.shongo.controller.reservation.AliasReservation.class);
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
                        throw new TodoImplementException(sort.toString());
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
                                throw new TodoImplementException(childReservation.getClass().getName());
                            }
                        }
                        if (!technologyFound) {
                            iterator.remove();
                        }
                    }
                    else {
                        throw new TodoImplementException(reservation.getClass().getName());
                    }
                }
            }

            // Fill reservations to response
            for (cz.cesnet.shongo.controller.reservation.Reservation reservation : reservations) {
                response.addItem(reservation.toApi(authorization.isAdmin(userId)));
            }
            return response;
        }
        finally {
            entityManager.close();
        }
    }

    /**
     * Check whether user with given {@code userId} can provide given {@code providedReservationRequestId}.
     *
     * @param userId
     * @param providedReservationRequestId
     * @throws ControllerReportSet.SecurityNotAuthorizedException
     *
     */
    private void checkProvidedReservationRequest(String userId, String providedReservationRequestId)
    {
        if (providedReservationRequestId != null) {
            EntityIdentifier entityId = EntityIdentifier.parse(providedReservationRequestId);
            if (!authorization.hasPermission(userId, entityId, Permission.PROVIDE_RESERVATION_REQUEST)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault(
                        "provide reservation request %s", entityId);
            }
        }
    }

    /**
     * Check whether {@code abstractReservationRequestImpl} can be modified or deleted.
     *
     * @param abstractReservationRequest
     * @return true when the given {@code abstractReservationRequest} can be modified, otherwise false
     */
    private boolean isModifiableReservationRequest(
            cz.cesnet.shongo.controller.request.AbstractReservationRequest abstractReservationRequest,
            ReservationManager reservationManager)
    {
        Allocation allocation = abstractReservationRequest.getAllocation();

        // Check if reservation is not created by controller
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
            if (isModifiableReservationRequest(reservationRequestImpl, reservationManager)) {
                return false;
            }
        }

        // Check allocated reservations
        for (cz.cesnet.shongo.controller.reservation.Reservation reservation : allocation.getReservations()) {
            if (reservationManager.isProvided(reservation)) {
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
        reservationRequestSummary.setType(ReservationRequestType.valueOf(record[1].toString()));
        reservationRequestSummary.setDateTime(new DateTime(record[2]));
        reservationRequestSummary.setUserId(record[3].toString());
        reservationRequestSummary.setDescription(record[4] != null ? record[4].toString() : null);
        reservationRequestSummary.setPurpose(ReservationRequestPurpose.valueOf(record[5].toString()));
        reservationRequestSummary.setEarliestSlot(new Interval(
                new DateTime(record[6]), new DateTime(record[7])));
        reservationRequestSummary.setState(
                ReservationRequestSummary.State.valueOf(record[8].toString()));
        reservationRequestSummary.setProvidedReservationRequestId(record[9] != null ? record[9].toString() : null);
        String type = record[10].toString();
        if (type.equals("ALIAS")) {
            ReservationRequestSummary.AliasSpecification aliasSpecification =
                    new ReservationRequestSummary.AliasSpecification();
            aliasSpecification.setAliasType(AliasType.ROOM_NAME);
            aliasSpecification.setValue(record[13].toString());
            reservationRequestSummary.setSpecification(aliasSpecification);
        }
        else if (type.equals("ROOM")) {
            ReservationRequestSummary.RoomSpecification roomSpecification =
                    new ReservationRequestSummary.RoomSpecification();
            roomSpecification.setParticipantCount(((Number) record[12]).intValue());
            reservationRequestSummary.setSpecification(roomSpecification);
        }
        else if (type.equals("RESOURCE")) {
            ReservationRequestSummary.ResourceSpecification resourceSpecification =
                    new ReservationRequestSummary.ResourceSpecification();
            resourceSpecification.setResourceId(EntityIdentifier.formatId(EntityType.RESOURCE, ((Number) record[14]).longValue()));
            reservationRequestSummary.setSpecification(resourceSpecification);
        }
        if (record[11] != null) {
            String technologies = record[11].toString();
            if (!technologies.isEmpty()) {
                for (String technology : technologies.split(",")) {
                    reservationRequestSummary.addTechnology(Technology.valueOf(technology));
                }
            }
        }
        return reservationRequestSummary;
    }
}
