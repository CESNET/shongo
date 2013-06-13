package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.api.util.Converter;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.api.Specification;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListRequest;
import cz.cesnet.shongo.controller.api.request.ReservationRequestListResponse;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.request.*;
import cz.cesnet.shongo.controller.request.AliasSetSpecification;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.scheduler.SchedulerException;
import cz.cesnet.shongo.controller.scheduler.SpecificationCheckAvailability;
import cz.cesnet.shongo.controller.util.DatabaseFilter;
import cz.cesnet.shongo.report.Report;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;
import java.util.*;

/**
 * Implementation of {@link ReservationService}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationServiceImpl extends Component
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
    public Object checkSpecificationAvailability(SecurityToken token, Specification specificationApi, Interval slot)
    {
        String userId = authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();

        try {
            specificationApi.setupNewEntity();
            cz.cesnet.shongo.controller.request.Specification specification =
                    cz.cesnet.shongo.controller.request.Specification.createFromApi(specificationApi, entityManager);
            Throwable cause = null;
            if (specification instanceof SpecificationCheckAvailability) {
                SpecificationCheckAvailability checkAvailability = (SpecificationCheckAvailability) specification;
                try {
                    checkAvailability.checkAvailability(slot, entityManager);
                    return Boolean.TRUE;
                }
                catch (SchedulerException exception) {
                    return exception.getReport().getMessageRecursive(Report.MessageType.USER);
                }
                catch (UnsupportedOperationException exception) {
                    cause = exception;
                }
            }
            throw new RuntimeException(String.format("Specification '%s' cannot be checked for availability.",
                    specificationApi.getClass().getSimpleName()), cause);
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

        reservationRequestApi.setupNewEntity();

        // Change user id (only root can do that)
        if (reservationRequestApi.getUserId() != null && authorization.isAdmin(userId)) {
            userId = reservationRequestApi.getUserId();
        }

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        AuthorizationManager authorizationManager = new AuthorizationManager(entityManager);
        try {
            authorizationManager.beginTransaction(authorization);
            entityManager.getTransaction().begin();

            cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequest =
                    cz.cesnet.shongo.controller.request.AbstractReservationRequest.createFromApi(
                            reservationRequestApi, entityManager);
            reservationRequest.setUserId(userId);

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

    /**
     * Check whether {@code abstractReservationRequestImpl} can be modified or deleted.
     *
     * @param abstractReservationRequest
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
     * Properties which can be filled to allow modification of reservation request whose reservation is provided to
     * another reservation request.
     */
    private final static Set<String> MODIFIABLE_FILLED_PROPERTIES = new HashSet<String>()
    {{
            add("id");
            add(cz.cesnet.shongo.controller.api.ReservationRequest.SLOT);
            add(cz.cesnet.shongo.controller.api.ReservationRequestSet.SLOTS);
        }};

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
            switch (oldReservationRequest.getType()) {
                case MODIFIED:
                    throw new ControllerReportSet.ReservationRequestAlreadyModifiedException(entityId.toId());
                case DELETED:
                    throw new ControllerReportSet.ReservationRequestDeletedException(entityId.toId());
            }
            ReservationManager reservationManager = new ReservationManager(entityManager);
            if (!isModifiableReservationRequest(oldReservationRequest, reservationManager)) {
                if (!MODIFIABLE_FILLED_PROPERTIES.containsAll(reservationRequestApi.getFilledProperties())) {
                    throw new ControllerReportSet.ReservationRequestNotModifiableException(entityId.toId());
                }
            }

            // Update old detached reservation request (the changes will not be serialized to database)
            oldReservationRequest.fromApi(reservationRequestApi, entityManager);

            // Create new reservation request by cloning old reservation request
            cz.cesnet.shongo.controller.request.AbstractReservationRequest newReservationRequest =
                    oldReservationRequest.clone();

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
            switch (reservationRequest.getType()) {
                case DELETED:
                    throw new ControllerReportSet.ReservationRequestDeletedException(entityId.toId());
            }
            ReservationManager reservationManager = new ReservationManager(entityManager);
            if (!isModifiableReservationRequest(reservationRequest, reservationManager)) {
                throw new ControllerReportSet.ReservationRequestNotModifiableException(
                        EntityIdentifier.formatId(reservationRequest));
            }

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
                switch (reservationRequest.getState()) {
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
                switch (reservationRequest.getState()) {
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
    public ReservationRequestListResponse listReservationRequestsNew(ReservationRequestListRequest request)
    {
        authorization.validate(request.getSecurityToken());

        System.err.println("\nLISTING\n");
        System.err.flush();

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            // Create query string
            String queryBody = " FROM AbstractReservationRequest reservationRequest"
                    + " LEFT JOIN reservationRequest.specification specification";

            // Create select query
            TypedQuery<Object[]> listQuery = entityManager.createQuery(""
                    + "SELECT "
                    + " reservationRequest.id,"
                    + " reservationRequest.userId,"
                    + " reservationRequest.created,"
                    + " reservationRequest.purpose,"
                    + " reservationRequest.description,"
                    + " specification"
                    + queryBody, Object[].class);

            // Get total result count
            TypedQuery<Long> countQuery = entityManager.createQuery(
                    "SELECT COUNT(reservationRequest.id)" + queryBody, Long.class);
            Integer totalResultCount = countQuery.getSingleResult().intValue();

            // Restrict first result
            Integer firstResult = request.getStart(0);
            if (firstResult <= 0) {
                firstResult = 0;
            }
            listQuery.setFirstResult(firstResult);

            // Restrict result count
            Integer maxResultCount = request.getCount(-1);
            if (maxResultCount != null && maxResultCount != -1) {
                if ((firstResult + maxResultCount) > totalResultCount) {
                    maxResultCount = totalResultCount - firstResult;
                }
                listQuery.setMaxResults(maxResultCount);
            }

            // List requested results
            List<Object[]> results = listQuery.getResultList();

            ReservationRequestListResponse response = new ReservationRequestListResponse();
            response.setCount(totalResultCount);
            response.setStart(firstResult);

            Set<Long> roomReservationRequestIds = new HashSet<Long>();
            Set<Long> aliasReservationRequestIds = new HashSet<Long>();
            Map<Long, ReservationRequestListResponse.Item> reservationRequestById =
                    new HashMap<Long, ReservationRequestListResponse.Item>();
            for (Object[] result : results) {
                Long id = (Long) result[0];
                ReservationRequestListResponse.Item reservationRequest = new ReservationRequestListResponse.Item();
                reservationRequest.setId(EntityIdentifier.formatId(EntityType.RESERVATION_REQUEST, id));
                reservationRequest.setUserId((String) result[1]);
                reservationRequest.setCreated((DateTime) result[2]);
                reservationRequest.setPurpose((ReservationRequestPurpose) result[3]);
                reservationRequest.setDescription((String) result[4]);
                response.addItem(reservationRequest);

                // Prepare specification
                Object specification = result[5];
                if (specification instanceof cz.cesnet.shongo.controller.request.AliasSpecification) {
                    aliasReservationRequestIds.add(id);
                }
                else if (specification instanceof cz.cesnet.shongo.controller.request.AliasSetSpecification) {
                    aliasReservationRequestIds.add(id);
                }
                else if (specification instanceof cz.cesnet.shongo.controller.request.RoomSpecification) {
                    roomReservationRequestIds.add(id);
                }
                reservationRequestById.put(id, reservationRequest);
            }

            if (roomReservationRequestIds.size() > 0) {
                // TODO: create query to fetch description

                for (Long id : roomReservationRequestIds) {
                    ReservationRequestListResponse.Item reservationRequest = reservationRequestById.get(id);
                    ReservationRequestListResponse.RoomType roomType = new ReservationRequestListResponse.RoomType();
                    roomType.setParticipantCount(5);
                    roomType.setName("test");
                    reservationRequest.setType(roomType);
                }
            }
            if (aliasReservationRequestIds.size() > 0) {
                // TODO: create query to fetch description

                for (Long id : aliasReservationRequestIds) {
                    ReservationRequestListResponse.Item reservationRequest = reservationRequestById.get(id);
                    ReservationRequestListResponse.AliasType aliasType = new ReservationRequestListResponse.AliasType();
                    aliasType.setType(AliasType.ROOM_NAME);
                    aliasType.setValue("test");
                    reservationRequest.setType(aliasType);
                }
            }
            return response;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public Collection<ReservationRequestSummary> listReservationRequests(SecurityToken token,
            Map<String, Object> filter)
    {
        String userId = authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

        try {
            Set<Long> reservationRequestIds =
                    authorization.getEntitiesWithPermission(userId, EntityType.RESERVATION_REQUEST, Permission.READ);
            String filterUserId = DatabaseFilter.getUserIdFromFilter(filter);
            Set<Technology> technologies = DatabaseFilter.getTechnologiesFromFilter(filter);
            Set<Class<? extends cz.cesnet.shongo.controller.request.Specification>> specificationClasses =
                    DatabaseFilter.getClassesFromFilter(filter, "specificationClass",
                            cz.cesnet.shongo.controller.request.Specification.class);
            Set<Long> providedReservationIds = null;
            if (filter != null && filter.containsKey("providedReservationId")) {

                Object value = filter.get("providedReservationId");
                Object[] items;
                if (value instanceof String) {
                    items = new Object[]{value};
                }
                else {
                    items = (Object[]) Converter.convert(value, Object[].class);
                }
                if (items.length > 0) {
                    providedReservationIds = new HashSet<Long>();
                    for (Object item : items) {
                        providedReservationIds.add(EntityIdentifier.parseId(
                                cz.cesnet.shongo.controller.reservation.Reservation.class,
                                (String) Converter.convert(item, String.class)));
                    }
                }
            }

            List<cz.cesnet.shongo.controller.request.AbstractReservationRequest> reservationRequests =
                    reservationRequestManager.list(reservationRequestIds, filterUserId, technologies,
                            specificationClasses, providedReservationIds);

            List<ReservationRequestSummary> summaryList = new ArrayList<ReservationRequestSummary>();
            for (cz.cesnet.shongo.controller.request.AbstractReservationRequest abstractReservationRequest : reservationRequests) {
                ReservationRequestSummary summary = getSummary(abstractReservationRequest);
                summaryList.add(summary);
            }
            return summaryList;
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
            switch (reservationRequest.getType()) {
                case DELETED:
                    throw new ControllerReportSet.ReservationRequestDeletedException(entityId.toId());
            }

            return reservationRequest.toApi(authorization.isAdmin(userId));
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
    public Collection<Reservation> getReservations(SecurityToken token, Collection<String> reservationIds)
    {
        String userId = authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationManager reservationManager = new ReservationManager(entityManager);

        try {
            List<Reservation> reservations = new LinkedList<Reservation>();
            for (String reservationId : reservationIds) {
                Long id = EntityIdentifier.parseId(
                        cz.cesnet.shongo.controller.reservation.Reservation.class, reservationId);
                cz.cesnet.shongo.controller.reservation.Reservation reservationImpl = reservationManager.get(id);
                reservations.add(reservationImpl.toApi(authorization.isAdmin(userId)));
            }
            return reservations;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public Collection<Reservation> listReservations(SecurityToken token, Map<String, Object> filter)
    {
        String userId = authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationManager reservationManager = new ReservationManager(entityManager);

        try {
            Set<Long> reservationIds =
                    authorization.getEntitiesWithPermission(userId, EntityType.RESERVATION, Permission.READ);
            Long reservationRequestId = null;
            if (filter != null) {
                if (filter.containsKey("reservationRequestId")) {
                    reservationRequestId = EntityIdentifier.parseId(
                            cz.cesnet.shongo.controller.request.AbstractReservationRequest.class,
                            (String) Converter.convert(filter.get("reservationRequestId"), String.class));
                }
            }
            Set<Technology> technologies = DatabaseFilter.getTechnologiesFromFilter(filter);
            Set<Class<? extends cz.cesnet.shongo.controller.reservation.Reservation>> reservationClasses =
                    DatabaseFilter.getClassesFromFilter(filter, "reservationClass",
                            cz.cesnet.shongo.controller.reservation.Reservation.class);

            List<cz.cesnet.shongo.controller.reservation.Reservation> reservations =
                    reservationManager.list(reservationIds, reservationRequestId, reservationClasses, technologies);
            List<Reservation> apiReservations = new ArrayList<Reservation>();
            for (cz.cesnet.shongo.controller.reservation.Reservation reservation : reservations) {
                apiReservations.add(reservation.toApi(authorization.isAdmin(userId)));
            }
            return apiReservations;
        }
        finally {
            entityManager.close();
        }
    }

    /**
     * @param abstractReservationRequest
     * @return {@link ReservationRequestSummary}
     */
    private ReservationRequestSummary getSummary(
            cz.cesnet.shongo.controller.request.AbstractReservationRequest abstractReservationRequest)
    {
        ReservationRequestSummary summary = new ReservationRequestSummary();
        summary.setId(EntityIdentifier.formatId(abstractReservationRequest));
        summary.setUserId(abstractReservationRequest.getUserId());
        summary.setCreated(abstractReservationRequest.getCreated());
        summary.setPurpose(abstractReservationRequest.getPurpose());
        summary.setDescription(abstractReservationRequest.getDescription());

        // Set type based on specification
        cz.cesnet.shongo.controller.request.Specification specification =
                abstractReservationRequest.getSpecification();
        if (specification instanceof cz.cesnet.shongo.controller.request.ResourceSpecification) {
            cz.cesnet.shongo.controller.request.ResourceSpecification resourceSpecification =
                    (cz.cesnet.shongo.controller.request.ResourceSpecification) specification;
            ReservationRequestSummary.ResourceType resourceType = new ReservationRequestSummary.ResourceType();
            resourceType.setResourceId(EntityIdentifier.formatId(resourceSpecification.getResource()));
            summary.setType(resourceType);
        }
        else if (specification instanceof cz.cesnet.shongo.controller.request.RoomSpecification) {
            cz.cesnet.shongo.controller.request.RoomSpecification roomSpecification =
                    (cz.cesnet.shongo.controller.request.RoomSpecification) specification;
            summary.setType(getSummaryRoomSpecificationType(abstractReservationRequest, roomSpecification));
        }
        else if (specification instanceof cz.cesnet.shongo.controller.request.AliasSpecification) {
            cz.cesnet.shongo.controller.request.AliasSpecification aliasSpecification =
                    (cz.cesnet.shongo.controller.request.AliasSpecification) specification;
            summary.setType(getSummaryAliasSpecificationType(abstractReservationRequest, aliasSpecification));
        }
        else if (specification instanceof AliasSetSpecification) {
            AliasSetSpecification aliasSetSpecification = (AliasSetSpecification) specification;
            summary.setType(getSummaryAliasSpecificationType(abstractReservationRequest, aliasSetSpecification));
        }

        // Set state and get earliest slot
        Interval earliestSlot = null;
        if (abstractReservationRequest instanceof cz.cesnet.shongo.controller.request.ReservationRequest) {
            cz.cesnet.shongo.controller.request.ReservationRequest reservationRequest =
                    (cz.cesnet.shongo.controller.request.ReservationRequest) abstractReservationRequest;
            earliestSlot = reservationRequest.getSlot();
            summary.setState(reservationRequest.getStateAsApi());
        }
        else if (abstractReservationRequest instanceof cz.cesnet.shongo.controller.request.ReservationRequestSet) {
            cz.cesnet.shongo.controller.request.ReservationRequestSet reservationRequestSet =
                    (cz.cesnet.shongo.controller.request.ReservationRequestSet) abstractReservationRequest;
            for (cz.cesnet.shongo.controller.common.DateTimeSlot slot : reservationRequestSet.getSlots()) {
                Interval interval = slot.getEarliest(DateTime.now());
                if (interval == null) {
                    continue;
                }
                if (earliestSlot == null || interval.getStart().isBefore(earliestSlot.getStart())) {
                    earliestSlot = interval;
                }
            }
            List<cz.cesnet.shongo.controller.request.ReservationRequest> requests =
                    reservationRequestSet.getAllocation().getChildReservationRequests();
            if (earliestSlot == null && requests.size() > 0) {
                earliestSlot = requests.get(requests.size() - 1).getSlot();
            }
            summary.setState(ReservationRequestState.NOT_ALLOCATED);
            for (cz.cesnet.shongo.controller.request.ReservationRequest reservationRequest : requests) {
                if (Temporal.isIntervalEqualed(reservationRequest.getSlot(), earliestSlot)) {
                    summary.setState(reservationRequest.getStateAsApi());
                }
            }
        }

        // Set earliest slot
        summary.setEarliestSlot(earliestSlot);

        return summary;
    }

    /**
     * @param reservationRequest
     * @param aliasSpecification
     * @return {@link cz.cesnet.shongo.controller.api.ReservationRequestSummary.AliasType}
     */
    private ReservationRequestSummary.AliasType getSummaryAliasSpecificationType(
            cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequest,
            cz.cesnet.shongo.controller.request.AliasSpecification aliasSpecification)
    {
        if (aliasSpecification.getValue() != null) {
            ReservationRequestSummary.AliasType aliasType = new ReservationRequestSummary.AliasType();
            aliasType.setValue(aliasSpecification.getValue());
            Set<cz.cesnet.shongo.AliasType> aliasTypes = aliasSpecification.getAliasTypes();
            if (aliasTypes.size() == 1) {
                aliasType.setAliasType(aliasTypes.iterator().next());
            }
            return aliasType;
        }
        return null;
    }

    /**
     * @param reservationRequest
     * @param aliasSetSpecification
     * @return {@link cz.cesnet.shongo.controller.api.ReservationRequestSummary.AliasType}
     */
    private ReservationRequestSummary.Type getSummaryAliasSpecificationType(
            cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequest,
            cz.cesnet.shongo.controller.request.AliasSetSpecification aliasSetSpecification)
    {
        for (cz.cesnet.shongo.controller.request.AliasSpecification aliasSpecification :
                aliasSetSpecification.getAliasSpecifications()) {
            ReservationRequestSummary.AliasType aliasType =
                    getSummaryAliasSpecificationType(reservationRequest, aliasSpecification);
            if (aliasType != null) {
                return aliasType;
            }
        }
        return null;
    }

    /**
     * @param reservationRequest
     * @param roomSpecification
     * @return {@link cz.cesnet.shongo.controller.api.ReservationRequestSummary.RoomType}
     */
    private ReservationRequestSummary.RoomType getSummaryRoomSpecificationType(
            cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequest,
            cz.cesnet.shongo.controller.request.RoomSpecification roomSpecification)
    {
        ReservationRequestSummary.RoomType roomType = new ReservationRequestSummary.RoomType();
        roomType.setParticipantCount(roomSpecification.getParticipantCount());

        // Get room name from alias specifications
        String roomName = null;
        for (cz.cesnet.shongo.controller.request.AliasSpecification aliasSpecification :
                roomSpecification.getAliasSpecifications()) {
            String value = aliasSpecification.getValue();
            if (value != null && aliasSpecification.hasAliasType(cz.cesnet.shongo.AliasType.ROOM_NAME)) {
                roomName = value;
                break;
            }
        }
        // Get room name from provider reservations
        if (roomName == null) {
            for (cz.cesnet.shongo.controller.reservation.Reservation reservation :
                    reservationRequest.getProvidedReservations()) {
                if (reservation instanceof cz.cesnet.shongo.controller.reservation.AliasReservation) {
                    cz.cesnet.shongo.controller.reservation.AliasReservation aliasReservation =
                            (cz.cesnet.shongo.controller.reservation.AliasReservation) reservation;
                    Alias alias = aliasReservation.getAlias(cz.cesnet.shongo.AliasType.ROOM_NAME);
                    if (alias != null) {
                        roomName = alias.getValue();
                        break;
                    }
                }
                else {
                    for (cz.cesnet.shongo.controller.reservation.Reservation childReservation :
                            reservation.getChildReservations()) {
                        if (childReservation instanceof cz.cesnet.shongo.controller.reservation.AliasReservation) {
                            cz.cesnet.shongo.controller.reservation.AliasReservation childAliasReservation =
                                    (cz.cesnet.shongo.controller.reservation.AliasReservation) childReservation;
                            Alias alias = childAliasReservation.getAlias(cz.cesnet.shongo.AliasType.ROOM_NAME);
                            if (alias != null) {
                                roomName = alias.getValue();
                                break;
                            }
                        }
                    }

                }
            }
        }
        roomType.setName(roomName);
        return roomType;
    }
}
