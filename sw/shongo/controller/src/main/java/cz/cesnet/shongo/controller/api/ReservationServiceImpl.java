package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.util.Converter;
import cz.cesnet.shongo.controller.Authorization;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.fault.ReservationRequestNotModifiableException;
import cz.cesnet.shongo.controller.request.DateTimeSlotSpecification;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import cz.cesnet.shongo.controller.reservation.ReservationManager;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
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
        implements ReservationService, Component.EntityManagerFactoryAware, Component.DomainAware,
                   Component.AuthorizationAware
{
    /**
     * @see javax.persistence.EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see cz.cesnet.shongo.controller.Domain
     */
    private cz.cesnet.shongo.controller.Domain domain;

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
    public void setDomain(cz.cesnet.shongo.controller.Domain domain)
    {
        this.domain = domain;
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
        checkDependency(domain, cz.cesnet.shongo.controller.Domain.class);
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
                    reservationRequest, entityManager, domain);
            reservationRequestImpl.setUserId(authorization.getUserId(token));

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
            reservationRequestManager.create(reservationRequestImpl);

            entityManager.getTransaction().commit();
        }
        catch (Exception exception) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            if (exception instanceof FaultException) {
                throw (FaultException) exception;
            }
            else {
                throw new FaultException(exception);
            }
        }
        finally {
            entityManager.close();
        }

        return domain.formatIdentifier(reservationRequestImpl.getId());
    }

    /**
     * Check whether {@code abstractReservationRequestImpl} can be modified or deleted.
     *
     * @param abstractReservationRequestImpl
     * @throws ReservationRequestNotModifiableException
     *
     */
    private void checkModifiableReservationRequest(
            cz.cesnet.shongo.controller.request.AbstractReservationRequest abstractReservationRequestImpl)
            throws ReservationRequestNotModifiableException
    {
        if (abstractReservationRequestImpl instanceof cz.cesnet.shongo.controller.request.ReservationRequest) {
            cz.cesnet.shongo.controller.request.ReservationRequest reservationRequestImpl =
                    (cz.cesnet.shongo.controller.request.ReservationRequest) abstractReservationRequestImpl;
            if (reservationRequestImpl.getCreatedBy() ==
                    cz.cesnet.shongo.controller.request.ReservationRequest.CreatedBy.CONTROLLER) {
                throw new ReservationRequestNotModifiableException(
                        domain.formatIdentifier(abstractReservationRequestImpl.getId()));
            }
        }
    }

    @Override
    public void modifyReservationRequest(SecurityToken token, AbstractReservationRequest reservationRequestApi)
            throws FaultException
    {
        authorization.validate(token);

        Long reservationRequestId = domain.parseIdentifier(reservationRequestApi.getIdentifier());

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        try {

            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

            cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequest =
                    reservationRequestManager.get(reservationRequestId);
            checkModifiableReservationRequest(reservationRequest);
            reservationRequest.fromApi(reservationRequestApi, entityManager, domain);

            if (reservationRequest instanceof cz.cesnet.shongo.controller.request.ReservationRequest) {
                cz.cesnet.shongo.controller.request.ReservationRequest singleReservationRequestImpl =
                        (cz.cesnet.shongo.controller.request.ReservationRequest) reservationRequest;
                // Reservation request was modified, so we must clear it's state
                singleReservationRequestImpl.clearState();
                // Update state
                singleReservationRequestImpl.updateStateBySpecifications();
            }

            reservationRequestManager.update(reservationRequest);

            entityManager.getTransaction().commit();
        }
        catch (Exception exception) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            if (exception instanceof FaultException) {
                throw (FaultException) exception;
            }
            else {
                throw new FaultException(exception);
            }
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public void deleteReservationRequest(SecurityToken token, String reservationRequestIdentifier) throws FaultException
    {
        authorization.validate(token);

        Long reservationRequestId = domain.parseIdentifier(reservationRequestIdentifier);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        try {
            ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

            cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequestImpl =
                    reservationRequestManager.get(reservationRequestId);
            checkModifiableReservationRequest(reservationRequestImpl);

            reservationRequestManager.delete(reservationRequestImpl);

            entityManager.getTransaction().commit();
        }
        catch (Exception exception) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            if (exception instanceof FaultException) {
                throw (FaultException) exception;
            }
            else {
                throw new FaultException(exception);
            }
        }
        finally {
            entityManager.close();
        }
    }

    @Override
    public Collection<ReservationRequestSummary> listReservationRequests(SecurityToken token,
            Map<String, Object> filter) throws FaultException
    {
        authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

        if (filter == null) {
            filter = new HashMap<String, Object>();
        }
        Long userId = null;
        Set<Technology> technologies = null;
        if (filter != null) {
            if (filter.containsKey("userId")) {
                Object value = filter.get("userId");
                userId = (value != null ? Long.valueOf(value.toString()) : null);
            }
            if (filter.containsKey("technology")) {
                @SuppressWarnings("unchecked")
                Set<Technology> value = (Set<Technology>) Converter.convert(filter.get("technology"), Set.class,
                        new Class[]{Technology.class});
                technologies = value;
            }
        }
        List<cz.cesnet.shongo.controller.request.AbstractReservationRequest> reservationRequests =
                reservationRequestManager.list(userId, technologies);

        List<ReservationRequestSummary> summaryList = new ArrayList<ReservationRequestSummary>();
        for (cz.cesnet.shongo.controller.request.AbstractReservationRequest abstractReservationRequest : reservationRequests) {
            ReservationRequestSummary summary = new ReservationRequestSummary();
            summary.setIdentifier(domain.formatIdentifier(abstractReservationRequest.getId()));
            summary.setUserId(abstractReservationRequest.getUserId().intValue());
            summary.setState(ReservationRequestSummary.State.NOT_ALLOCATED);

            Interval earliestSlot = null;
            if (abstractReservationRequest instanceof cz.cesnet.shongo.controller.request.ReservationRequest) {
                cz.cesnet.shongo.controller.request.ReservationRequest reservationRequest =
                        (cz.cesnet.shongo.controller.request.ReservationRequest) abstractReservationRequest;
                earliestSlot = reservationRequest.getSlot();
                if (reservationRequest.getState().equals(ReservationRequest.State.ALLOCATED)) {
                    summary.setState(ReservationRequestSummary.State.ALLOCATED);
                }
                else if (reservationRequest.getState().equals(ReservationRequest.State.ALLOCATION_FAILED)) {
                    summary.setState(ReservationRequestSummary.State.ALLOCATION_FAILED);
                }
            }
            else if (abstractReservationRequest instanceof cz.cesnet.shongo.controller.request.ReservationRequestSet) {
                cz.cesnet.shongo.controller.request.ReservationRequestSet reservationRequestSet =
                        (cz.cesnet.shongo.controller.request.ReservationRequestSet) abstractReservationRequest;
                for (DateTimeSlotSpecification slot : reservationRequestSet.getSlots()) {
                    Interval interval = slot.getEarliest(null);
                    if (earliestSlot == null || interval.getStart().isBefore(earliestSlot.getStart())) {
                        earliestSlot = interval;
                    }
                }
                for (ReservationRequest reservationRequest : reservationRequestSet.getReservationRequests()) {
                    if (reservationRequest.getState().equals(ReservationRequest.State.ALLOCATED)) {
                        if (summary.getState().equals(ReservationRequestSummary.State.NOT_ALLOCATED)) {
                            summary.setState(ReservationRequestSummary.State.ALLOCATED);
                        }
                    }
                    else if (reservationRequest.getState().equals(ReservationRequest.State.ALLOCATION_FAILED)) {
                        summary.setState(ReservationRequestSummary.State.ALLOCATION_FAILED);
                    }

                }
            }
            else if (abstractReservationRequest instanceof cz.cesnet.shongo.controller.request.PermanentReservationRequest) {
                cz.cesnet.shongo.controller.request.PermanentReservationRequest permanentReservationRequest =
                        (cz.cesnet.shongo.controller.request.PermanentReservationRequest) abstractReservationRequest;
                for (DateTimeSlotSpecification slot : permanentReservationRequest.getSlots()) {
                    Interval interval = slot.getEarliest(null);
                    if (earliestSlot == null || interval.getStart().isBefore(earliestSlot.getStart())) {
                        earliestSlot = interval;
                    }
                }
                if (permanentReservationRequest.getResourceReservations().size() > 0) {
                    summary.setState(ReservationRequestSummary.State.ALLOCATION_FAILED);
                }
                // TODO: Implement allocation failed state to permanent reservations
            }
            else {
                throw new TodoImplementException(abstractReservationRequest.getClass().getCanonicalName());
            }

            summary.setCreated(abstractReservationRequest.getCreated());
            summary.setName(abstractReservationRequest.getName());
            if (abstractReservationRequest instanceof cz.cesnet.shongo.controller.request.NormalReservationRequest) {
                cz.cesnet.shongo.controller.request.NormalReservationRequest normalReservationRequest =
                        (cz.cesnet.shongo.controller.request.NormalReservationRequest) abstractReservationRequest;
                summary.setPurpose(normalReservationRequest.getPurpose());
                summary.setType(ReservationRequestSummary.Type.NORMAL);
            }
            if (abstractReservationRequest instanceof cz.cesnet.shongo.controller.request.PermanentReservationRequest) {
                summary.setType(ReservationRequestSummary.Type.PERMANENT);
            }
            summary.setDescription(abstractReservationRequest.getDescription());
            summary.setEarliestSlot(earliestSlot);
            summaryList.add(summary);
        }

        entityManager.close();

        return summaryList;
    }

    @Override
    public AbstractReservationRequest getReservationRequest(SecurityToken token, String reservationRequestIdentifier)
            throws FaultException
    {
        authorization.validate(token);

        Long id = domain.parseIdentifier(reservationRequestIdentifier);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

        cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequestImpl =
                reservationRequestManager.get(id);
        AbstractReservationRequest reservationRequest = reservationRequestImpl.toApi(domain);

        entityManager.close();

        return reservationRequest;
    }

    @Override
    public Reservation getReservation(SecurityToken token, String reservationIdentifier) throws FaultException
    {
        authorization.validate(token);

        Long reservationId = domain.parseIdentifier(reservationIdentifier);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationManager reservationManager = new ReservationManager(entityManager);

        cz.cesnet.shongo.controller.reservation.Reservation reservationImpl = reservationManager.get(reservationId);
        Reservation reservation = reservationImpl.toApi(domain);

        entityManager.close();

        return reservation;
    }

    @Override
    public Collection<Reservation> listReservations(SecurityToken token, String reservationRequestIdentifier)
            throws FaultException
    {
        authorization.validate(token);

        Long id = domain.parseIdentifier(reservationRequestIdentifier);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationManager reservationManager = new ReservationManager(entityManager);

        List<cz.cesnet.shongo.controller.reservation.Reservation> reservations =
                reservationManager.listByReservationRequest(id);
        List<Reservation> apiReservations = new ArrayList<Reservation>();
        for (cz.cesnet.shongo.controller.reservation.Reservation reservation : reservations) {
            apiReservations.add(reservation.toApi(domain));
        }

        entityManager.close();

        return apiReservations;
    }

    @Override
    public Collection<Reservation> listReservations(SecurityToken token) throws FaultException
    {
        authorization.validate(token);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationManager reservationManager = new ReservationManager(entityManager);

        List<cz.cesnet.shongo.controller.reservation.Reservation> reservations = reservationManager.list();
        List<Reservation> apiReservations = new ArrayList<Reservation>();
        for (cz.cesnet.shongo.controller.reservation.Reservation reservation : reservations) {
            apiReservations.add(reservation.toApi(domain));
        }

        entityManager.close();

        return apiReservations;
    }
}
