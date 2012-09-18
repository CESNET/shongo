package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Configuration;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.Collection;

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
    public String createReservationRequest(SecurityToken token, AbstractReservationRequest reservationRequest)
            throws FaultException
    {
        reservationRequest.setupNewEntity();

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequestImpl =
                cz.cesnet.shongo.controller.request.AbstractReservationRequest.createFromApi(
                        reservationRequest, entityManager, domain);

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        reservationRequestManager.create(reservationRequestImpl);

        entityManager.getTransaction().commit();
        entityManager.close();

        return domain.formatIdentifier(reservationRequestImpl.getId());
    }

    @Override
    public void modifyReservationRequest(SecurityToken token, AbstractReservationRequest reservationRequest)
            throws FaultException
    {
        Long reservationRequestId = domain.parseIdentifier(reservationRequest.getIdentifier());

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

        cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequestImpl =
                reservationRequestManager.get(reservationRequestId);
        reservationRequestImpl.fromApi(reservationRequest, entityManager, domain);

        reservationRequestManager.update(reservationRequestImpl);

        entityManager.getTransaction().commit();
        entityManager.close();
    }

    @Override
    public void deleteReservationRequest(SecurityToken token, String reservationRequestIdentifier) throws FaultException
    {
        Long reservationRequestId = domain.parseIdentifier(reservationRequestIdentifier);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        entityManager.getTransaction().begin();

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

        cz.cesnet.shongo.controller.request.AbstractReservationRequest reservationRequest =
                reservationRequestManager.get(reservationRequestId);

        reservationRequestManager.delete(reservationRequest);

        entityManager.getTransaction().commit();
        entityManager.close();
    }

    @Override
    public Collection<ReservationRequestSummary> listReservationRequests(SecurityToken token)
    {
        throw new TodoImplementException();
        /*EntityManager entityManager = entityManagerFactory.createEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

        List<cz.cesnet.shongo.controller.oldrequest.ReservationRequest> list = reservationRequestManager.list();
        List<ReservationRequestSummary> summaryList = new ArrayList<ReservationRequestSummary>();
        for (cz.cesnet.shongo.controller.oldrequest.ReservationRequest reservationRequest : list) {
            ReservationRequestSummary summary = new ReservationRequestSummary();
            summary.setIdentifier(domain.formatIdentifier(reservationRequest.getId()));

            Interval earliestSlot = null;
            for (DateTimeSlotSpecification slot : reservationRequest.getRequestedSlots()) {
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

        return summaryList;*/
    }

    @Override
    public AbstractReservationRequest getReservationRequest(SecurityToken token, String reservationRequestIdentifier)
            throws FaultException
    {
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
    public Collection<Reservation> listReservations(SecurityToken token, String reservationRequestIdentifier)
            throws FaultException
    {
        throw new TodoImplementException();
        /*Long id = domain.parseIdentifier(reservationRequestIdentifier);

        EntityManager entityManager = entityManagerFactory.createEntityManager();
        AllocatedCompartmentManager allocatedCompartmentManager = new AllocatedCompartmentManager(entityManager);

        List<cz.cesnet.shongo.controller.allocation.AllocatedCompartment> allocatedCompartments =
                allocatedCompartmentManager.listByReservationRequest(id);
        List<AllocatedCompartment> allocatedCompartmentList = new ArrayList<AllocatedCompartment>();
        for (cz.cesnet.shongo.controller.allocation.AllocatedCompartment allocation : allocatedCompartments) {
            allocatedCompartmentList.add(allocation.toApi(domain));
        }

        entityManager.close();

        return allocatedCompartmentList;*/
    }
}
