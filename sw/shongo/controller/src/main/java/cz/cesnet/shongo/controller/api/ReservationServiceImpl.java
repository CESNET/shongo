package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.util.Converter;
import cz.cesnet.shongo.controller.Authorization;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.fault.ReservationRequestNotModifiableException;
import cz.cesnet.shongo.controller.request.DateTimeSlotSpecification;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import cz.cesnet.shongo.controller.resource.Alias;
import cz.cesnet.shongo.controller.util.DatabaseFilter;
import cz.cesnet.shongo.fault.EntityException;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * Reservation service implementation
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
     * @see cz.cesnet.shongo.controller.Authorization
     */
    private Authorization authorization;

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
    public String createReservationRequest(SecurityToken token, AbstractReservationRequest reservationRequest)
            throws FaultException
    {
        authorization.validate(token);

        if (reservationRequest == null) {
            throw new IllegalArgumentException("Reservation request should not be null.");
        }
        reservationRequest.setupNewEntity();

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequestImpl;
        try {
            reservationRequestImpl = cz.cesnet.shongo.controller.request.AbstractReservationRequest.createFromApi(
                    reservationRequest, entityManager);
            reservationRequestImpl.setUserId(authorization.getUserId(token));

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            reservationRequestManager.create(reservationRequestImpl);

            entityManager.getTransaction().commit();
        }
        catch (FaultException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new FaultException(exception);
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }

        return cz.cesnet.shongo.controller.Domain.getLocalDomain().formatId(reservationRequestImpl);
    }

    /**
     * Check whether {@code abstractReservationRequestImpl} can be modified or deleted.
     *
     * @param abstractReservationRequestImpl
     * @throws ReservationRequestNotModifiableException
     *
     */
    private void checkModifiableReservationRequest(
            cz.cesnet.shongo.controller.request.AbstractReservationRequest abstractReservationRequestImpl,
            EntityManager entityManager)
            throws ReservationRequestNotModifiableException
    {
        ReservationManager reservationManager = new ReservationManager(entityManager);

        boolean modifiable = true;
        if (abstractReservationRequestImpl instanceof cz.cesnet.shongo.controller.request.ReservationRequest) {
            cz.cesnet.shongo.controller.request.ReservationRequest reservationRequestImpl =
                    (cz.cesnet.shongo.controller.request.ReservationRequest) abstractReservationRequestImpl;
            if (reservationRequestImpl.getCreatedBy() ==
                    cz.cesnet.shongo.controller.request.ReservationRequest.CreatedBy.CONTROLLER) {
                modifiable = false;
            }

            if (modifiable) {
                if (reservationManager.isProvided(reservationRequestImpl.getReservation())) {
                    modifiable = false;
                }
            }
        }
        else if (abstractReservationRequestImpl instanceof cz.cesnet.shongo.controller.request.ReservationRequestSet) {
            cz.cesnet.shongo.controller.request.ReservationRequestSet reservationRequestSetImpl =
                    (cz.cesnet.shongo.controller.request.ReservationRequestSet) abstractReservationRequestImpl;
            for (cz.cesnet.shongo.controller.request.ReservationRequest reservationRequestImpl :
                    reservationRequestSetImpl.getReservationRequests()) {
                if (reservationManager.isProvided(reservationRequestImpl.getReservation())) {
                    modifiable = false;
                    break;
                }
            }
        }

        if (!modifiable) {
            throw new ReservationRequestNotModifiableException(
                    cz.cesnet.shongo.controller.Domain.getLocalDomain().formatId(abstractReservationRequestImpl));
        }
    }

    @Override
    public void modifyReservationRequest(SecurityToken token, AbstractReservationRequest reservationRequestApi)
            throws FaultException
    {
        authorization.validate(token);

        Domain localDomain = cz.cesnet.shongo.controller.Domain.getLocalDomain();
        Long id = localDomain.parseId(reservationRequestApi.getId());

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        try {

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

            cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequest =
                    reservationRequestManager.get(id);
            checkModifiableReservationRequest(reservationRequest, entityManager);
            reservationRequest.fromApi(reservationRequestApi, entityManager);

            if (reservationRequest instanceof cz.cesnet.shongo.controller.request.ReservationRequest) {
                cz.cesnet.shongo.controller.request.ReservationRequest singleReservationRequestImpl =
                        (cz.cesnet.shongo.controller.request.ReservationRequest) reservationRequest;
                // Reservation request was modified, so we must clear it's state
                singleReservationRequestImpl.clearState();
                // Update state
                singleReservationRequestImpl.updateStateBySpecification();
            }

            reservationRequestManager.update(reservationRequest);

            entityManager.getTransaction().commit();
        }
        catch (EntityException exception) {
            exception.setEntityId(localDomain.formatId(exception.getEntityId()));
            throw exception;
        }
        catch (FaultException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new FaultException(exception);
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public void deleteReservationRequest(SecurityToken token, String reservationRequestId) throws FaultException
    {
        authorization.validate(token);

        Domain localDomain = cz.cesnet.shongo.controller.Domain.getLocalDomain();
        Long id = localDomain.parseId(reservationRequestId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        try {
            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

            cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequestImpl =
                    reservationRequestManager.get(id);
            checkModifiableReservationRequest(reservationRequestImpl, entityManager);

            reservationRequestManager.delete(reservationRequestImpl);

            entityManager.getTransaction().commit();
        }
        catch (EntityException exception) {
            exception.setEntityId(localDomain.formatId(exception.getEntityId()));
            throw exception;
        }
        catch (FaultException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new FaultException(exception);
        }
        finally {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            entityManager.close();
        }
    }

    @Override
    public Collection<ReservationRequestSummary> listReservationRequests(SecurityToken token,
            Map<String, Object> filter) throws FaultException
    {
        authorization.validate(token);

        cz.cesnet.shongo.controller.Domain localDomain = cz.cesnet.shongo.controller.Domain.getLocalDomain();

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

        try {
            String userId = DatabaseFilter.getUserIdFromFilter(filter, authorization.getUserId(token));
            Set<Technology> technologies = DatabaseFilter.getTechnologiesFromFilter(filter);
            Set<Class<? extends cz.cesnet.shongo.controller.request.Specification>> specificationClasses =
                    DatabaseFilter.getClassesFromFilter(filter, "specificationClass",
                            cz.cesnet.shongo.controller.request.Specification.class);
            Long providedReservationId = null;
            if (filter != null) {
                if (filter.containsKey("providedReservationId")) {
                    providedReservationId = localDomain.parseId((String) Converter.convert(
                            filter.get("providedReservationId"), String.class));
                }
            }

            List<cz.cesnet.shongo.controller.request.AbstractReservationRequest> reservationRequests =
                    reservationRequestManager.list(userId, technologies, specificationClasses, providedReservationId);

            List<ReservationRequestSummary> summaryList = new ArrayList<ReservationRequestSummary>();
            for (cz.cesnet.shongo.controller.request.AbstractReservationRequest abstractReservationRequest : reservationRequests) {
                ReservationRequestSummary summary = getSummary(abstractReservationRequest, localDomain);
                summaryList.add(summary);
            }
            return summaryList;
        }
        finally {
            entityManager.close();
        }
    }

    /**
     * @param abstractReservationRequest
     * @param localDomain
     * @return {@link ReservationRequestSummary}
     */
    private ReservationRequestSummary getSummary(
            cz.cesnet.shongo.controller.request.AbstractReservationRequest abstractReservationRequest,
            cz.cesnet.shongo.controller.Domain localDomain)
    {
        ReservationRequestSummary summary = new ReservationRequestSummary();
        summary.setId(localDomain.formatId(abstractReservationRequest));
        summary.setUserId(abstractReservationRequest.getUserId());
        summary.setCreated(abstractReservationRequest.getCreated());
        summary.setDescription(abstractReservationRequest.getDescription());

        Interval earliestSlot = null;
        if (abstractReservationRequest instanceof cz.cesnet.shongo.controller.request.NormalReservationRequest) {
            cz.cesnet.shongo.controller.request.NormalReservationRequest normalReservationRequest =
                    (cz.cesnet.shongo.controller.request.NormalReservationRequest) abstractReservationRequest;

            // Set purpose
            summary.setPurpose(normalReservationRequest.getPurpose());

            // Set type based on specification
            cz.cesnet.shongo.controller.request.Specification specification =
                    normalReservationRequest.getSpecification();
            if (specification instanceof cz.cesnet.shongo.controller.request.RoomSpecification) {
                cz.cesnet.shongo.controller.request.RoomSpecification roomSpecification =
                        (cz.cesnet.shongo.controller.request.RoomSpecification) specification;
                summary.setType(getSummaryRoomSpecificationType(normalReservationRequest, roomSpecification));
            }
            else if (specification instanceof cz.cesnet.shongo.controller.request.AliasSpecification) {
                cz.cesnet.shongo.controller.request.AliasSpecification aliasSpecification =
                        (cz.cesnet.shongo.controller.request.AliasSpecification) specification;
                summary.setType(getSummaryAliasSpecificationType(normalReservationRequest, aliasSpecification));
            }
            else if (specification instanceof cz.cesnet.shongo.controller.request.AliasGroupSpecification) {
                cz.cesnet.shongo.controller.request.AliasGroupSpecification aliasGroupSpecification =
                        (cz.cesnet.shongo.controller.request.AliasGroupSpecification) specification;
                summary.setType(getSummaryAliasSpecificationType(normalReservationRequest, aliasGroupSpecification));
            }

            // Set slot and state
            if (abstractReservationRequest instanceof cz.cesnet.shongo.controller.request.ReservationRequest) {
                cz.cesnet.shongo.controller.request.ReservationRequest reservationRequest =
                        (cz.cesnet.shongo.controller.request.ReservationRequest) abstractReservationRequest;
                earliestSlot = reservationRequest.getSlot();
                summary.setState(reservationRequest.getStateAsApi());
            }
            else if (abstractReservationRequest instanceof cz.cesnet.shongo.controller.request.ReservationRequestSet) {
                cz.cesnet.shongo.controller.request.ReservationRequestSet reservationRequestSet =
                        (cz.cesnet.shongo.controller.request.ReservationRequestSet) abstractReservationRequest;
                for (DateTimeSlotSpecification slot : reservationRequestSet.getSlots()) {
                    Interval interval = slot.getEarliest(DateTime.now());
                    if (earliestSlot == null || interval.getStart().isBefore(earliestSlot.getStart())) {
                        earliestSlot = interval;
                    }
                }
                List<cz.cesnet.shongo.controller.request.ReservationRequest> requests =
                        reservationRequestSet.getReservationRequests();
                if (earliestSlot == null && requests.size() > 0) {
                    earliestSlot = requests.get(requests.size() - 1).getSlot();
                }
                for (cz.cesnet.shongo.controller.request.ReservationRequest reservationRequest : requests) {
                    if (reservationRequest.getSlot().equals(earliestSlot)) {
                        summary.setState(reservationRequest.getStateAsApi());
                    }
                }
            }
        }
        else if (abstractReservationRequest instanceof cz.cesnet.shongo.controller.request.PermanentReservationRequest) {
            cz.cesnet.shongo.controller.request.PermanentReservationRequest permanentReservationRequest =
                    (cz.cesnet.shongo.controller.request.PermanentReservationRequest) abstractReservationRequest;

            // Set type
            ReservationRequestSummary.PermanentType permanentType = new ReservationRequestSummary.PermanentType();
            permanentType.setResourceId(localDomain.formatId(permanentReservationRequest.getResource()));
            summary.setType(permanentType);

            // Set earliest slot
            for (DateTimeSlotSpecification slot : permanentReservationRequest.getSlots()) {
                Interval interval = slot.getEarliest(null);
                if (earliestSlot == null || interval.getStart().isBefore(earliestSlot.getStart())) {
                    earliestSlot = interval;
                }
            }

            // Set state
            if (permanentReservationRequest.getReservations().size() > 0) {
                summary.setState(ReservationRequestState.ALLOCATED);
            }
            else {
                summary.setState(ReservationRequestState.NOT_ALLOCATED);
            }
            // TODO: Implement allocation failed state to permanent reservations
        }
        else {
            throw new TodoImplementException(abstractReservationRequest.getClass().getCanonicalName());
        }
        summary.setEarliestSlot(earliestSlot);
        return summary;
    }

    /**
     * @param normalReservationRequest
     * @param aliasSpecification
     * @return {@link cz.cesnet.shongo.controller.api.ReservationRequestSummary.AliasType}
     */
    private ReservationRequestSummary.AliasType getSummaryAliasSpecificationType(
            cz.cesnet.shongo.controller.request.NormalReservationRequest normalReservationRequest,
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
     * @param normalReservationRequest
     * @param aliasGroupSpecification
     * @return {@link cz.cesnet.shongo.controller.api.ReservationRequestSummary.AliasType}
     */
    private ReservationRequestSummary.Type getSummaryAliasSpecificationType(
            cz.cesnet.shongo.controller.request.NormalReservationRequest normalReservationRequest,
            cz.cesnet.shongo.controller.request.AliasGroupSpecification aliasGroupSpecification)
    {
        for (cz.cesnet.shongo.controller.request.AliasSpecification aliasSpecification :
                aliasGroupSpecification.getAliasSpecifications()) {
            ReservationRequestSummary.AliasType aliasType =
                    getSummaryAliasSpecificationType(normalReservationRequest, aliasSpecification);
            if (aliasType != null) {
                return aliasType;
            }
        }
        return null;
    }

    /**
     * @param normalReservationRequest
     * @param roomSpecification
     * @return {@link cz.cesnet.shongo.controller.api.ReservationRequestSummary.RoomType}
     */
    private ReservationRequestSummary.RoomType getSummaryRoomSpecificationType(
            cz.cesnet.shongo.controller.request.NormalReservationRequest normalReservationRequest,
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
                    normalReservationRequest.getProvidedReservations()) {
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

    @Override
    public AbstractReservationRequest getReservationRequest(SecurityToken token, String reservationRequestId)
            throws FaultException
    {
        authorization.validate(token);

        Domain localDomain = cz.cesnet.shongo.controller.Domain.getLocalDomain();
        Long id = localDomain.parseId(reservationRequestId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

        try {
            cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequestImpl =
                    reservationRequestManager.get(id);
            return reservationRequestImpl.toApi();
        }
        catch (EntityException exception) {
            exception.setEntityId(localDomain.formatId(exception.getEntityId()));
            throw exception;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public Reservation getReservation(SecurityToken token, String reservationId) throws FaultException
    {
        authorization.validate(token);

        Long id = cz.cesnet.shongo.controller.Domain.getLocalDomain().parseId(reservationId);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationManager reservationManager = new ReservationManager(entityManager);

        try {
            cz.cesnet.shongo.controller.reservation.Reservation reservationImpl = reservationManager.get(id);
            return reservationImpl.toApi();
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public Collection<Reservation> getReservations(SecurityToken token, Collection<String> reservationIds)
            throws FaultException
    {
        authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationManager reservationManager = new ReservationManager(entityManager);

        try {
            List<Reservation> reservations = new LinkedList<Reservation>();
            for (String reservationId : reservationIds) {
                Long id = cz.cesnet.shongo.controller.Domain.getLocalDomain().parseId(reservationId);
                cz.cesnet.shongo.controller.reservation.Reservation reservationImpl = reservationManager.get(id);
                reservations.add(reservationImpl.toApi());
            }
            return reservations;
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public Collection<Reservation> listReservations(SecurityToken token, Map<String, Object> filter)
            throws FaultException
    {
        authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationManager reservationManager = new ReservationManager(entityManager);

        try {
            String userId = DatabaseFilter.getUserIdFromFilter(filter, authorization.getUserId(token));
            Long reservationRequestId = null;
            if (filter != null) {
                if (filter.containsKey("reservationRequestId")) {
                    reservationRequestId = cz.cesnet.shongo.controller.Domain.getLocalDomain().parseId(
                            (String) Converter.convert(filter.get("reservationRequestId"), String.class));
                }
            }
            Set<Technology> technologies = DatabaseFilter.getTechnologiesFromFilter(filter);
            Set<Class<? extends cz.cesnet.shongo.controller.reservation.Reservation>> reservationClasses =
                    DatabaseFilter.getClassesFromFilter(filter, "reservationClass",
                            cz.cesnet.shongo.controller.reservation.Reservation.class);

            List<cz.cesnet.shongo.controller.reservation.Reservation> reservations =
                    reservationManager.list(userId, reservationRequestId, reservationClasses, technologies);
            List<Reservation> apiReservations = new ArrayList<Reservation>();
            for (cz.cesnet.shongo.controller.reservation.Reservation reservation : reservations) {
                apiReservations.add(reservation.toApi());
            }
            return apiReservations;
        }
        finally {
            entityManager.close();
        }
    }
}
