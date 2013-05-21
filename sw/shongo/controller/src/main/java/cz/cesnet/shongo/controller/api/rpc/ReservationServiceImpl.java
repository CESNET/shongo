package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.api.util.Converter;
import cz.cesnet.shongo.controller.*;
import cz.cesnet.shongo.controller.api.*;
import cz.cesnet.shongo.controller.authorization.Authorization;
import cz.cesnet.shongo.controller.authorization.AuthorizationManager;
import cz.cesnet.shongo.controller.common.EntityIdentifier;
import cz.cesnet.shongo.controller.request.AliasSetSpecification;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
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
            reservationRequest.validate();

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
     * @param reservationRequest
     */
    private boolean isModifiableReservationRequest(
            cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequest,
            EntityManager entityManager)
    {
        ReservationManager reservationManager = new ReservationManager(entityManager);

        if (reservationRequest instanceof cz.cesnet.shongo.controller.request.ReservationRequest) {
            cz.cesnet.shongo.controller.request.ReservationRequest reservationRequestImpl =
                    (cz.cesnet.shongo.controller.request.ReservationRequest) reservationRequest;
            if (reservationRequestImpl.getReservationRequestSet() != null) {
                return false;
            }
            cz.cesnet.shongo.controller.reservation.Reservation reservation = reservationRequestImpl.getReservation();
            if (reservation != null && reservationManager.isProvided(reservation)) {
                return false;
            }
        }
        else if (reservationRequest instanceof cz.cesnet.shongo.controller.request.ReservationRequestSet) {
            cz.cesnet.shongo.controller.request.ReservationRequestSet reservationRequestSetImpl =
                    (cz.cesnet.shongo.controller.request.ReservationRequestSet) reservationRequest;
            for (cz.cesnet.shongo.controller.request.ReservationRequest reservationRequestImpl :
                    reservationRequestSetImpl.getReservationRequests()) {
                cz.cesnet.shongo.controller.reservation.Reservation reservation = reservationRequestImpl
                        .getReservation();
                if (reservation != null && reservationManager.isProvided(reservation)) {
                    return false;
                }
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
            if (!isModifiableReservationRequest(oldReservationRequest, entityManager)) {
                if (!MODIFIABLE_FILLED_PROPERTIES.containsAll(reservationRequestApi.getFilledProperties())) {
                    throw new ControllerReportSet.ReservationRequestNotModifiableException(entityId.toId());
                }
            }

            // Update old detached reservation request (the changes will not be serialized to database)
            oldReservationRequest.loadLazyCollections();
            oldReservationRequest.fromApi(reservationRequestApi, entityManager);
            oldReservationRequest.validate();

            authorizationManager.beginTransaction(authorization);
            entityManager.detach(oldReservationRequest);
            entityManager.getTransaction().begin();

            // Create new reservation request by cloning old reservation request
            cz.cesnet.shongo.controller.request.AbstractReservationRequest newReservationRequest =
                    reservationRequestManager.modify(oldReservationRequest);

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
            if (!isModifiableReservationRequest(reservationRequest, entityManager)) {
                throw new ControllerReportSet.ReservationRequestNotModifiableException(
                        EntityIdentifier.formatId(reservationRequest));
            }

            reservationRequestManager.delete(reservationRequest, authorizationManager);

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

            cz.cesnet.shongo.controller.request.ReservationRequest reservationRequest =
                    reservationRequestManager.getReservationRequest(entityId.getPersistenceId());

            if (!authorization.hasPermission(userId, entityId, Permission.WRITE)) {
                ControllerReportSetHelper.throwSecurityNotAuthorizedFault("update reservation request %s", entityId);
            }
            if (reservationRequest.getState().equals(ReservationRequest.State.ALLOCATION_FAILED)) {
                // Reservation request was modified, so we must clear it's state
                reservationRequest.clearState();
                // Update state
                reservationRequest.updateStateBySpecification();
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
                    reservationRequestSet.getReservationRequests();
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
