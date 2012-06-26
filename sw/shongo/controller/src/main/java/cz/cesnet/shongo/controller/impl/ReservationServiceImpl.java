package cz.cesnet.shongo.controller.impl;

import cz.cesnet.shongo.common.api.SecurityToken;
import cz.cesnet.shongo.common.xmlrpc.FaultException;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.api.Fault;
import cz.cesnet.shongo.controller.api.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.api.ReservationRequestType;
import cz.cesnet.shongo.controller.api.ReservationService;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.Map;

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
        if ( domain == null ) {
            throw new IllegalStateException(getClass().getName() + " doesn't have the domain  set!");
        }
    }

    @Override
    public String getServiceName()
    {
        return "Reservation";
    }

    @Override
    public String createReservationRequest(SecurityToken token, ReservationRequestType type, Map attributes)
            throws FaultException
    {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        ReservationRequest reservationRequest = new ReservationRequest();

        // TODO: Continue implementing this

        // TODO: May be change types to map, list, basic types

        // Attribute Type
        switch (type) {
            case NORMAL:
                reservationRequest.setType(ReservationRequest.Type.NORMAL);
                break;
            case PERMANENT:
                reservationRequest.setType(ReservationRequest.Type.NORMAL);
                break;
            default:
                throw new FaultException(Fault.OTHER, "Reservation request type should be normal or permanent, "
                        + "but '" + type + "' was present!");
        }

        // Attribute Purpose
        /*ReservationRequestPurpose purpose =
        switch (type) {
            case NORMAL:
                reservationRequest.setType(ReservationRequest.Type.NORMAL);
                break;
            case PERMANENT:
                reservationRequest.setType(ReservationRequest.Type.NORMAL);
                break;
            default:
                throw new FaultException(Fault.OTHER, "Reservation request type should be normal or permanent, "
                        + "but '" + type + "' was present!");
        }*/

        /*if ( true ) {
            throw new FaultException(Fault.TODO_IMPLEMENT);
        }*/

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        reservationRequestManager.create(reservationRequest);

        entityManager.getTransaction().commit();
        entityManager.close();

        return domain.formatIdentifier(reservationRequest.getId());
    }

    @Override
    public void modifyReservationRequest(SecurityToken token, String reservationId, Map attributes)
            throws FaultException
    {
        throw new FaultException(Fault.TODO_IMPLEMENT);
    }

    @Override
    public void deleteReservationRequest(SecurityToken token, String reservationId) throws FaultException
    {
        throw new FaultException(Fault.TODO_IMPLEMENT);
    }
}
