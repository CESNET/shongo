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
import cz.cesnet.shongo.controller.util.DatabaseFilter;
import cz.cesnet.shongo.controller.util.NativeQuery;
import cz.cesnet.shongo.report.Report;
import org.joda.time.DateTime;
import org.joda.time.Interval;

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
            DatabaseFilter filter = new DatabaseFilter("request");

            // List only reservation requests which aren't created for another reservation request
            filter.addFilter("TYPE(request) != ReservationRequest OR request.parentAllocation IS NULL");

            // List only reservation requests which is current user permitted to read
            filter.addIds(authorization, userId, EntityType.RESERVATION_REQUEST, Permission.READ);

            // List only reservation requests which are requested
            if (request.getReservationRequestIds().size() > 0) {
                filter.addFilter("request.id IN (:reservationRequestIds)");
                Set<Long> reservationRequestIds = new HashSet<Long>();
                for (String reservationRequestId : request.getReservationRequestIds()) {
                    reservationRequestIds.add(EntityIdentifier.parseId(
                            cz.cesnet.shongo.controller.request.AbstractReservationRequest.class,
                            reservationRequestId));
                }
                filter.addFilterParameter("reservationRequestIds", reservationRequestIds);
            }

            // List only reservation requests which are ACTIVE
            filter.addFilter("request.state = :activeState", "activeState",
                    cz.cesnet.shongo.controller.request.AbstractReservationRequest.State.ACTIVE);

            if (request.getTechnologies().size() > 0) {
                // List only reservation requests which specifies given technologies
                filter.addFilter("request IN ("
                        + "  SELECT reservationRequest"
                        + "  FROM AbstractReservationRequest reservationRequest"
                        + "  LEFT JOIN reservationRequest.specification specification"
                        + "  LEFT JOIN specification.technologies technology"
                        + "  WHERE technology IN(:technologies)"
                        + ")");
                filter.addFilterParameter("technologies", request.getTechnologies());
            }

            if (request.getSpecificationClasses().size() > 0) {
                // List only reservation requests which has specification of given classes
                filter.addFilter("request IN ("
                        + "  SELECT reservationRequest"
                        + "  FROM AbstractReservationRequest reservationRequest"
                        + "  LEFT JOIN reservationRequest.specification reservationRequestSpecification"
                        + "  WHERE TYPE(reservationRequestSpecification) IN(:classes)"
                        + ")");
                Set<Class<? extends cz.cesnet.shongo.controller.request.Specification>> specificationClasses =
                        new HashSet<Class<? extends cz.cesnet.shongo.controller.request.Specification>>();
                for (Class<? extends Specification> type : request.getSpecificationClasses()) {
                    specificationClasses.add(cz.cesnet.shongo.controller.request.Specification.getClassFromApi(type));
                }
                filter.addFilterParameter("classes", specificationClasses);
            }

            String providedReservationRequestId = request.getProvidedReservationRequestId();
            if (providedReservationRequestId != null) {
                if (providedReservationRequestId.equals(ReservationRequestListRequest.FILTER_EMPTY)) {
                    // List only reservation requests which hasn't got provided any reservation request
                    filter.addFilter("request.providedReservationRequest IS NULL");
                }
                else if (providedReservationRequestId.equals(ReservationRequestListRequest.FILTER_NOT_EMPTY)) {
                    // List only reservation requests which got provided any reservation request
                    filter.addFilter("request.providedReservationRequest IS NOT NULL");
                }
                else {
                    // List only reservation requests which got provided given reservation request
                    Long persistenceId = EntityIdentifier.parseId(
                            cz.cesnet.shongo.controller.request.ReservationRequest.class, providedReservationRequestId);
                    filter.addFilter("request.providedReservationRequest.id = :providedReservationRequestId");
                    filter.addFilterParameter("providedReservationRequestId", persistenceId);
                }
            }

            // Create parts
            String querySelect = " request.id,"
                    + " request.state,"
                    + " request.modifiedReservationRequest.id,"
                    + " request.createdAt,"
                    + " request.createdBy,"
                    + " request.updatedAt,"
                    + " request.updatedBy,"
                    + " request.purpose,"
                    + " request.description,"
                    + " request.slotStart,"
                    + " request.slotEnd,"
                    + " request.providedReservationRequest.id,"
                    + " request.allocationState,"
                    + " specification";
            String queryFrom = "AbstractReservationRequest request"
                    + " LEFT JOIN request.specification specification";

            // Sort query part
            String queryOrderBy;
            ReservationRequestListRequest.Sort sort = request.getSort();
            if (sort != null) {
                switch (sort) {
                    case DATETIME:
                        queryOrderBy = "request.createdAt";
                        break;
                    default:
                        throw new TodoImplementException(sort.toString());
                }
            }
            else {
                queryOrderBy = "request.id";
            }
            Boolean sortDescending = request.getSortDescending();
            sortDescending = (sortDescending != null ? sortDescending : false);
            if (sortDescending) {
                queryOrderBy = queryOrderBy + " DESC";
            }

            ListResponse<ReservationRequestSummary> response = new ListResponse<ReservationRequestSummary>();
            List<Object[]> results = performListRequest("request", querySelect, Object[].class, queryFrom, queryOrderBy,
                    filter, request, response, entityManager);

            // Fill reservation requests to response
            Set<Long> aliasReservationRequestIds = new HashSet<Long>();
            Set<Long> reservationRequestSetIds = new HashSet<Long>();
            Map<Long, ReservationRequestSummary> reservationRequestById =
                    new HashMap<Long, ReservationRequestSummary>();
            for (Object[] result : results) {
                Long id = (Long) result[0];
                AbstractReservationRequest.State state = (AbstractReservationRequest.State) result[1];

                ReservationRequestSummary reservationRequestSummary = new ReservationRequestSummary();
                reservationRequestSummary.setId(EntityIdentifier.formatId(EntityType.RESERVATION_REQUEST, id));
                reservationRequestSummary.setDateTime((DateTime) result[3]);
                reservationRequestSummary.setUserId((String) result[4]);
                reservationRequestSummary.setPurpose((ReservationRequestPurpose) result[7]);
                reservationRequestSummary.setDescription((String) result[8]);

                // Determine reservation request type
                ReservationRequestType type;

                // Type can be anything
                if (state.equals(AbstractReservationRequest.State.DELETED)) {
                    type = ReservationRequestType.DELETED;
                }
                else if (result[2] != null) {
                    type = ReservationRequestType.MODIFIED;
                }
                else {
                    type = ReservationRequestType.NEW;
                }


                // Set reservation request type
                reservationRequestSummary.setType(type);

                // Date/time slot
                DateTime slotStart = (DateTime) result[9];
                DateTime slotEnd = (DateTime) result[10];
                if (slotStart != null && slotEnd != null) {
                    reservationRequestSummary.setEarliestSlot(new Interval(slotStart, slotEnd));
                    /*reservationRequestSummary.setState(
                            cz.cesnet.shongo.controller.request.ReservationRequest.AllocationState.getApi(
                                    (cz.cesnet.shongo.controller.request.ReservationRequest.AllocationState) result[12]));*/
                }
                else {
                    reservationRequestSetIds.add(id);
                }

                // Provided reservation request
                if (result[11] != null) {
                    reservationRequestSummary.setProvidedReservationRequestId(
                            EntityIdentifier.formatId(EntityType.RESERVATION_REQUEST, (Long) result[11]));
                }

                // Prepare specification
                Object specification = result[13];
                if (specification instanceof cz.cesnet.shongo.controller.request.AliasSpecification) {
                    aliasReservationRequestIds.add(id);
                }
                else if (specification instanceof cz.cesnet.shongo.controller.request.AliasSetSpecification) {
                    aliasReservationRequestIds.add(id);
                }
                else if (specification instanceof cz.cesnet.shongo.controller.request.RoomSpecification) {
                    cz.cesnet.shongo.controller.request.RoomSpecification roomSpecification =
                            (cz.cesnet.shongo.controller.request.RoomSpecification) specification;
                    ReservationRequestSummary.RoomSpecification room =
                            new ReservationRequestSummary.RoomSpecification();
                    room.setParticipantCount(roomSpecification.getParticipantCount());
                    reservationRequestSummary.setSpecification(room);
                }
                reservationRequestById.put(id, reservationRequestSummary);

                // Append reservation request summary
                response.addItem(reservationRequestSummary);
            }

            // Fill reservation request set earliest slot
            if (reservationRequestSetIds.size() > 0) {
                // Select reservation request id of the last reservation request or
                // the first reservation request in the future
                List resultList = entityManager.createNativeQuery("SELECT reservation_request.id"
                        + " FROM reservation_request"
                        + " INNER JOIN ("
                        + "    SELECT"
                        + "      abstract_reservation_request.allocation_id AS id,"
                        + "      MAX(reservation_request.slot_end) AS slot_end_max,"
                        + "      MIN(CASE WHEN reservation_request.slot_end > now() THEN reservation_request.slot_end ELSE NULL END) AS slot_end_future_min"
                        + "    FROM reservation_request_set"
                        + "      LEFT OUTER JOIN abstract_reservation_request ON abstract_reservation_request.id = reservation_request_set.id"
                        + "      LEFT OUTER JOIN allocation ON allocation.id = abstract_reservation_request.allocation_id"
                        + "      LEFT OUTER JOIN reservation_request ON reservation_request.parent_allocation_id = allocation.id"
                        + "    WHERE reservation_request_set.id IN (:reservationRequestSetIds)"
                        + "    GROUP BY abstract_reservation_request.allocation_id"
                        + " ) AS allocation ON allocation.id = reservation_request.parent_allocation_id AND ("
                        + "                    allocation.slot_end_max = reservation_request.slot_end OR"
                        + "                    allocation.slot_end_future_min = reservation_request.slot_end)")
                        .setParameter("reservationRequestSetIds", reservationRequestSetIds)
                        .getResultList();
                if (resultList.size() > 0) {
                    Set<Long> reservationRequestIds = new HashSet<Long>();
                    for (Object result : resultList) {
                        reservationRequestIds.add(((BigInteger) result).longValue());
                    }

                    // Get list of requested slots
                    List<Object[]> requestedSlots = entityManager.createQuery(""
                            + "SELECT "
                            + " reservationRequest.parentAllocation.reservationRequest.id,"
                            + " reservationRequest.slotStart,"
                            + " reservationRequest.slotEnd,"
                            + " reservationRequest.allocationState"
                            + " FROM ReservationRequest reservationRequest"
                            + " WHERE reservationRequest.id IN(:reservationRequestIds)",
                            Object[].class)
                            .setParameter("reservationRequestIds", reservationRequestIds)
                            .getResultList();

                    // Sort requested slots for each reservation request
                    Collections.sort(requestedSlots, new Comparator<Object[]>()
                    {
                        @Override
                        public int compare(Object[] object1, Object[] object2)
                        {
                            if (!object1[0].equals(object2[0])) {
                                // Skip different reservation requests
                                return 0;
                            }
                            return ((DateTime) object1[1]).compareTo((DateTime) object2[1]);
                        }
                    });

                    // Fill first requested slot for each reservation request
                    for (Object[] requestedSlot : requestedSlots) {
                        Long id = (Long) requestedSlot[0];
                        if (!reservationRequestSetIds.contains(id)) {
                            continue;
                        }
                        reservationRequestSetIds.remove(id);

                        DateTime slotStart = (DateTime) requestedSlot[1];
                        DateTime slotEnd = (DateTime) requestedSlot[2];
                        ReservationRequestSummary reservationRequestSummary = reservationRequestById.get(id);
                        reservationRequestSummary.setEarliestSlot(new Interval(slotStart, slotEnd));
                        /*reservationRequestSummary.setState(
                                cz.cesnet.shongo.controller.request.ReservationRequest.AllocationState.getApi(
                                        (cz.cesnet.shongo.controller.request.ReservationRequest.AllocationState) requestedSlot[3]));*/
                    }
                }
            }

            // Fill reservation request collections
            Set<Long> reservationRequestIds = reservationRequestById.keySet();
            if (reservationRequestIds.size() > 0) {
                // Fill technologies
                List<Object[]> technologies = entityManager.createQuery(""
                        + "SELECT reservationRequest.id, technology"
                        + " FROM AbstractReservationRequest reservationRequest"
                        + " LEFT JOIN reservationRequest.specification.technologies technology"
                        + " WHERE reservationRequest.id IN(:reservationRequestIds) AND technology != null",
                        Object[].class)
                        .setParameter("reservationRequestIds", reservationRequestIds)
                        .getResultList();
                for (Object[] providedReservation : technologies) {
                    Long id = (Long) providedReservation[0];
                    Technology technology = (Technology) providedReservation[1];
                    ReservationRequestSummary reservationRequestSummary = reservationRequestById.get(id);
                    reservationRequestSummary.addTechnology(technology);
                }
            }

            // Fill aliases
            if (aliasReservationRequestIds.size() > 0) {
                // Get list of requested aliases for all reservation requests
                List<Object[]> aliasReservationRequests = entityManager.createQuery(""
                        + "SELECT reservationRequest.id, aliasType, aliasSpecification.value"
                        + " FROM AbstractReservationRequest reservationRequest, AliasSpecification aliasSpecification"
                        + " LEFT JOIN aliasSpecification.aliasTypes aliasType"
                        + " WHERE reservationRequest.id IN(:reservationRequestIds)"
                        + " AND ("
                        + "       reservationRequest.specification.id = aliasSpecification.id"
                        + "    OR aliasSpecification.id IN("
                        + "       SELECT childAliasSpecification.id FROM AliasSetSpecification aliasSetSpecification"
                        + "       LEFT JOIN aliasSetSpecification.aliasSpecifications childAliasSpecification"
                        + "       WHERE reservationRequest.specification.id = aliasSetSpecification.id)"
                        + " )",
                        Object[].class)
                        .setParameter("reservationRequestIds", aliasReservationRequestIds)
                        .getResultList();

                // Sort requested aliases for each reservation request
                Collections.sort(aliasReservationRequests, new Comparator<Object[]>()
                {
                    @Override
                    public int compare(Object[] object1, Object[] object2)
                    {
                        if (!object1[0].equals(object2[0])) {
                            // Skip different reservation requests
                            return 0;
                        }
                        return ((AliasType) object1[1]).compareTo((AliasType) object2[1]);
                    }
                });

                // Fill first requested alias for each reservation request
                for (Object[] aliasReservation : aliasReservationRequests) {
                    Long id = (Long) aliasReservation[0];
                    if (!aliasReservationRequestIds.contains(id)) {
                        continue;
                    }
                    aliasReservationRequestIds.remove(id);

                    ReservationRequestSummary item = reservationRequestById.get(id);
                    ReservationRequestSummary.AliasSpecification alias =
                            new ReservationRequestSummary.AliasSpecification();
                    alias.setAliasType((AliasType) aliasReservation[1]);
                    alias.setValue((String) aliasReservation[2]);
                    item.setSpecification(alias);
                }
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


                ReservationRequestSummary reservationRequestSummary = new ReservationRequestSummary();
                reservationRequestSummary.setId(EntityIdentifier.formatId(
                        EntityType.RESERVATION_REQUEST, historyItemData[0].toString()));
                reservationRequestSummary.setType(ReservationRequestType.valueOf(historyItemData[1].toString()));
                reservationRequestSummary.setDateTime(new DateTime(historyItemData[2]));
                reservationRequestSummary.setUserId(historyItemData[3].toString());
                reservationRequestSummary.setDescription(historyItemData[4].toString());
                reservationRequestSummary.setPurpose(ReservationRequestPurpose.valueOf(historyItemData[5].toString()));
                reservationRequestSummary.setEarliestSlot(new Interval(
                        new DateTime(historyItemData[6]), new DateTime(historyItemData[7])));
                reservationRequestSummary.setState(
                        ReservationRequestSummary.State.valueOf(historyItemData[8].toString()));
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
            DatabaseFilter filter = new DatabaseFilter("reservationRequest");

            // List only child reservation requests which are ACTIVE
            filter.addFilter("childReservationRequest.state = :activeState", "activeState",
                    cz.cesnet.shongo.controller.request.AbstractReservationRequest.State.ACTIVE);

            // List only child reservation requests for specified parent reservation request
            filter.addFilter("reservationRequest.id = :reservationRequestId", "reservationRequestId",
                    reservationRequestId.getPersistenceId());

            String queryFrom = "AbstractReservationRequest reservationRequest"
                    + " LEFT JOIN reservationRequest.allocation.childReservationRequests childReservationRequest";
            ListResponse<cz.cesnet.shongo.controller.api.ReservationRequest> response =
                    new ListResponse<cz.cesnet.shongo.controller.api.ReservationRequest>();
            List<cz.cesnet.shongo.controller.request.ReservationRequest> reservationRequests = performListRequest(
                    "childReservationRequest", "childReservationRequest",
                    cz.cesnet.shongo.controller.request.ReservationRequest.class, queryFrom,
                    "childReservationRequest.slotStart DESC", filter, request, response, entityManager);

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
            DatabaseFilter filter = new DatabaseFilter("reservation");

            // List only reservations which is current user permitted to read
            filter.addIds(authorization, userId, EntityType.RESERVATION, Permission.READ);

            // List only reservations which are requested
            if (request.getReservationIds().size() > 0) {
                filter.addFilter("reservation.id IN (:reservationIds)");
                Set<Long> reservationIds = new HashSet<Long>();
                for (String reservationId : request.getReservationIds()) {
                    reservationIds.add(EntityIdentifier.parseId(
                            cz.cesnet.shongo.controller.reservation.Reservation.class, reservationId));
                }
                filter.addFilterParameter("reservationIds", reservationIds);
            }

            // List only reservations of requested classes
            Set<Class<? extends Reservation>> reservationApiClasses = request.getReservationClasses();
            if (reservationApiClasses.size() > 0) {
                if (reservationApiClasses.contains(AliasReservation.class)) {
                    // List only reservations of given classes or raw reservations which have alias reservation as child
                    filter.addFilter("reservation IN ("
                            + "   SELECT mainReservation FROM Reservation mainReservation"
                            + "   LEFT JOIN mainReservation.childReservations childReservation"
                            + "   WHERE TYPE(mainReservation) IN(:classes)"
                            + "      OR (TYPE(mainReservation) = :raw AND TYPE(childReservation) = :alias)"
                            + " )");
                    filter.addFilterParameter("alias", cz.cesnet.shongo.controller.reservation.AliasReservation.class);
                    filter.addFilterParameter("raw", cz.cesnet.shongo.controller.reservation.Reservation.class);
                }
                else {
                    // List only reservations of given classes
                    filter.addFilter("TYPE(reservation) IN(:classes)");
                }
                Set<Class<? extends cz.cesnet.shongo.controller.reservation.Reservation>> reservationClasses =
                        new HashSet<Class<? extends cz.cesnet.shongo.controller.reservation.Reservation>>();
                for (Class<? extends Reservation> reservationApiClass : reservationApiClasses) {
                    reservationClasses.add(cz.cesnet.shongo.controller.reservation.Reservation.getClassFromApi(
                            reservationApiClass));
                }
                filter.addFilterParameter("classes", reservationClasses);
            }

            // List only reservations allocated for requested reservation request
            if (request.getReservationRequestId() != null) {
                // List only reservations which are allocated for reservation request with given id or child reservation requests
                filter.addFilter("reservation.allocation IS NOT NULL AND (reservation.allocation IN ("
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
                filter.addFilterParameter("reservationRequestId", EntityIdentifier.parseId(
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

            ListResponse<Reservation> response = new ListResponse<Reservation>();
            List<cz.cesnet.shongo.controller.reservation.Reservation> reservations = performListRequest(
                    "reservation", "reservation", cz.cesnet.shongo.controller.reservation.Reservation.class,
                    "Reservation reservation", queryOrderBy, filter, request, response, entityManager);

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
}
