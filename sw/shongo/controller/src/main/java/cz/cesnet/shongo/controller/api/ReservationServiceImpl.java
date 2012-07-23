package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.FaultException;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import org.joda.time.Interval;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Reservation service implementation
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationServiceImpl extends Component implements ReservationService
{
    /**
     * @see Domain
     */
    private Domain domain;

    /**
     * Constructor.
     */
    public ReservationServiceImpl()
    {
    }

    /**
     * Constructor.
     *
     * @param domain sets the {@link #domain}
     */
    public ReservationServiceImpl(Domain domain)
    {
        setDomain(domain);
    }

    /**
     * @param domain sets the {@link #domain}
     */
    public void setDomain(Domain domain)
    {
        this.domain = domain;
    }

    @Override
    public void init()
    {
        super.init();
        if (domain == null) {
            throw new IllegalStateException(getClass().getName() + " doesn't have the domain set!");
        }
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

        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        // Create reservation request
        cz.cesnet.shongo.controller.request.ReservationRequest reservationRequestImpl =
                new cz.cesnet.shongo.controller.request.ReservationRequest();

        reservationRequestImpl.fromApi(reservationRequest, entityManager);

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

        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

        // Get reservation request
        cz.cesnet.shongo.controller.request.ReservationRequest reservationRequestImpl =
                reservationRequestManager.get(reservationRequestId);

        // Synchronize it from API
        reservationRequestImpl.fromApi(reservationRequest, entityManager);

        reservationRequestManager.update(reservationRequestImpl);

        entityManager.getTransaction().commit();
        entityManager.close();
    }

    @Override
    public void deleteReservationRequest(SecurityToken token, String reservationRequestIdentifier) throws FaultException
    {
        Long requestId = domain.parseIdentifier(reservationRequestIdentifier);

        EntityManager entityManager = getEntityManager();
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
    public ReservationRequestSummary[] listReservationRequests(SecurityToken token)
    {
        EntityManager entityManager = getEntityManager();
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

            summary.setType(reservationRequest.getType());
            summary.setName(reservationRequest.getName());
            summary.setPurpose(reservationRequest.getPurpose());
            summary.setDescription(reservationRequest.getDescription());
            summary.setEarliestSlot(earliestSlot);
            summaryList.add(summary);
        }

        entityManager.close();

        return summaryList.toArray(new ReservationRequestSummary[summaryList.size()]);
    }

    @Override
    public cz.cesnet.shongo.controller.api.ReservationRequest getReservationRequest(SecurityToken token,
            String reservationRequestIdentifier) throws FaultException
    {
        Long id = domain.parseIdentifier(reservationRequestIdentifier);

        EntityManager entityManager = getEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);

        cz.cesnet.shongo.controller.request.ReservationRequest requestImpl = reservationRequestManager.get(id);
        ReservationRequest request = requestImpl.toApi(entityManager);
        request.setIdentifier(domain.formatIdentifier(requestImpl.getId()));

        entityManager.close();

        return request;
    }
}
