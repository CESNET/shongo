package cz.cesnet.shongo.controller.impl;

import cz.cesnet.shongo.api.ControllerFault;
import cz.cesnet.shongo.api.FaultException;
import cz.cesnet.shongo.api.SecurityToken;
import cz.cesnet.shongo.common.AbsoluteDateTimeSpecification;
import cz.cesnet.shongo.common.PeriodicDateTimeSpecification;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import org.joda.time.DateTime;

import javax.persistence.EntityManager;
import java.util.Map;

/**
 * Reservation service implementation
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationServiceImpl extends Component implements cz.cesnet.shongo.api.ReservationService
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
    public String createReservationRequest(SecurityToken token,
            cz.cesnet.shongo.api.ReservationRequest reservationRequest)
            throws FaultException
    {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        ReservationRequest reservationRequestImpl = new ReservationRequest();
        reservationRequestImpl.setType(reservationRequest.type);
        reservationRequestImpl.setPurpose(reservationRequest.purpose);
        for (cz.cesnet.shongo.api.DateTimeSlot dateTimeSlot : reservationRequest.slots) {
            Object dateTime = dateTimeSlot.dateTime;
            if (dateTime instanceof DateTime) {
                reservationRequestImpl.addRequestedSlot(new AbsoluteDateTimeSpecification((DateTime) dateTime),
                        dateTimeSlot.duration);
            }
            else if (dateTime instanceof cz.cesnet.shongo.api.PeriodicDateTime) {
                cz.cesnet.shongo.api.PeriodicDateTime periodic = (cz.cesnet.shongo.api.PeriodicDateTime) dateTime;
                reservationRequestImpl.addRequestedSlot(new PeriodicDateTimeSpecification(periodic.start,
                        periodic.period), dateTimeSlot.duration);
            }
            else {
                throw new FaultException(ControllerFault.Common.UNKNOWN_FAULT,
                        "Unknown date/time type.");
            }
        }
        for (cz.cesnet.shongo.api.Compartment compartment : reservationRequest.compartments) {
        }

        // TODO: Check required fields

        if (true) {
            throw new FaultException(ControllerFault.TODO_IMPLEMENT);
        }

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        reservationRequestManager.create(reservationRequestImpl);

        entityManager.getTransaction().commit();
        entityManager.close();

        return domain.formatIdentifier(reservationRequestImpl.getId());
    }

    @Override
    public void modifyReservationRequest(SecurityToken token, String reservationId, Map attributes)
            throws FaultException
    {
        throw new FaultException(ControllerFault.TODO_IMPLEMENT);
    }

    @Override
    public void deleteReservationRequest(SecurityToken token, String reservationId) throws FaultException
    {
        throw new FaultException(ControllerFault.TODO_IMPLEMENT);
    }
}
