package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.allocation.AllocatedCompartmentManager;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import cz.cesnet.shongo.fault.FaultException;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Reservation service implementation
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationServiceImpl extends Component
        implements ReservationService, Component.EntityManagerFactoryAware, Component.DomainAware
{
    /**
     * @see javax.persistence.EntityManagerFactory
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * @see cz.cesnet.shongo.controller.Domain
     */
    private cz.cesnet.shongo.controller.Domain domain;

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
    public void init(Configuration configuration)
    {
        checkDependency(entityManagerFactory, EntityManagerFactory.class);
        checkDependency(domain, cz.cesnet.shongo.controller.Domain.class);
        super.init(configuration);
    }

    @Override
    public String getServiceName()
    {
        return "Reservation";
    }


    @Override
    public String createReservationRequest(SecurityToken token, ReservationRequest reservationRequest)
            throws FaultException
    {
        reservationRequest.setupNewEntity();

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        // Create reservation request
        cz.cesnet.shongo.controller.request.ReservationRequest reservationRequestImpl =
                new cz.cesnet.shongo.controller.request.ReservationRequest();

        reservationRequestImpl.fromApi(reservationRequest, entityManager, domain);

        // Save it
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        reservationRequestManager.create(reservationRequestImpl);

        entityManager.getTransaction().commit();
        entityManager.close();

        // Return reservation request identifier
        return domain.formatIdentifier(reservationRequestImpl.getId());
    }

    @Override
    public void modifyReservationRequest(SecurityToken token, ReservationRequest reservationRequest)
            throws FaultException
    {
        Long reservationRequestId = domain.parseIdentifier(reservationRequest.getIdentifier());

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

        // Get reservation request
        cz.cesnet.shongo.controller.request.ReservationRequest reservationRequestImpl =
                reservationRequestManager.get(reservationRequestId);

        // Synchronize it from API
        reservationRequestImpl.fromApi(reservationRequest, entityManager, domain);

        reservationRequestManager.update(reservationRequestImpl);

        entityManager.getTransaction().commit();
        entityManager.close();
    }

    @Override
    public void deleteReservationRequest(SecurityToken token, String reservationRequestIdentifier) throws FaultException
    {
        Long requestId = domain.parseIdentifier(reservationRequestIdentifier);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

        // Get reservation request
        cz.cesnet.shongo.controller.request.ReservationRequest requestImpl = reservationRequestManager.get(requestId);

        // Delete the request
        reservationRequestManager.delete(requestImpl);

        entityManager.getTransaction().commit();
        entityManager.close();
    }

    @Override
    public Collection<ReservationRequestSummary> listReservationRequests(SecurityToken token)
    {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

        List<cz.cesnet.shongo.controller.request.ReservationRequest> list = reservationRequestManager.list();
        List<ReservationRequestSummary> summaryList = new ArrayList<ReservationRequestSummary>();
        for (cz.cesnet.shongo.controller.request.ReservationRequest reservationRequest : list) {
            ReservationRequestSummary summary = new ReservationRequestSummary();
            summary.setIdentifier(domain.formatIdentifier(reservationRequest.getId()));

            Interval earliestSlot = null;
            for (cz.cesnet.shongo.controller.common.DateTimeSlot slot : reservationRequest.getRequestedSlots()) {
                Interval interval = slot.getEarliest(null);
                if (earliestSlot == null || interval.getStart().isBefore(earliestSlot.getStart())) {
                    earliestSlot = interval;
                }
            }

            summary.setCreated(reservationRequest.getCreated());
            summary.setType(reservationRequest.getType());
            summary.setName(reservationRequest.getName());
            summary.setPurpose(reservationRequest.getPurpose());
            summary.setDescription(reservationRequest.getDescription());
            summary.setEarliestSlot(earliestSlot);
            summaryList.add(summary);
        }

        entityManager.close();

        return summaryList;
    }

    @Override
    public ReservationRequest getReservationRequest(SecurityToken token,
            String reservationRequestIdentifier) throws FaultException
    {
        Long id = domain.parseIdentifier(reservationRequestIdentifier);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

        cz.cesnet.shongo.controller.request.ReservationRequest requestImpl = reservationRequestManager.get(id);
        ReservationRequest request = requestImpl.toApi(entityManager, domain);

        entityManager.close();

        return request;
    }

    @Override
    public Collection<AllocatedCompartment> listAllocatedCompartments(SecurityToken token,
            String reservationRequestIdentifier) throws FaultException
    {
        Long id = domain.parseIdentifier(reservationRequestIdentifier);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AllocatedCompartmentManager allocatedCompartmentManager = new AllocatedCompartmentManager(entityManager);

        List<cz.cesnet.shongo.controller.allocation.AllocatedCompartment> allocatedCompartments =
                allocatedCompartmentManager.listByReservationRequest(id);
        List<AllocatedCompartment> allocatedCompartmentList = new ArrayList<AllocatedCompartment>();
        for (cz.cesnet.shongo.controller.allocation.AllocatedCompartment allocation : allocatedCompartments) {
            allocatedCompartmentList.add(allocation.toApi(domain));
        }

        entityManager.close();

        return allocatedCompartmentList;
    }
}
