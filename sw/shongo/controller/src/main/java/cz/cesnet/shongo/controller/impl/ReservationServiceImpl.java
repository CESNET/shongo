package cz.cesnet.shongo.controller.impl;

import cz.cesnet.shongo.common.AbsoluteDateTimeSpecification;
import cz.cesnet.shongo.common.PeriodicDateTimeSpecification;
import cz.cesnet.shongo.common.api.FaultException;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.api.API;
import cz.cesnet.shongo.controller.api.Fault;
import cz.cesnet.shongo.controller.api.ReservationService;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import org.joda.time.DateTime;

import javax.persistence.EntityManager;
import java.util.Map;

import static cz.cesnet.shongo.common.util.Converter.convert;

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
    public String createReservationRequest(API.SecurityToken token, Map attributes)
            throws FaultException
    {
        API.ReservationRequest apiReservationRequest = new API.ReservationRequest();
        apiReservationRequest.fromMap(attributes);

        Map xx = apiReservationRequest.toMap();

        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setType(convert(apiReservationRequest.type, ReservationRequest.Type.class));
        reservationRequest.setPurpose(convert(apiReservationRequest.purpose, ReservationRequest.Purpose.class));
        for (API.DateTimeSlot dateTimeSlot : apiReservationRequest.slots) {
            Object dateTime = dateTimeSlot.dateTime;
            if (dateTime instanceof DateTime) {
                reservationRequest.addRequestedSlot(new AbsoluteDateTimeSpecification((DateTime) dateTime),
                        dateTimeSlot.duration);
            }
            else if (dateTime instanceof API.PeriodicDateTime) {
                API.PeriodicDateTime periodicDateTime = (API.PeriodicDateTime) dateTime;
                reservationRequest.addRequestedSlot(new PeriodicDateTimeSpecification(periodicDateTime.start,
                        periodicDateTime.period), dateTimeSlot.duration);
            }
            else {
                throw new FaultException(Fault.Common.UNKNOWN_FAULT, "Unknown date/time type.");
            }
        }
        for (API.Compartment compartment : apiReservationRequest.compartments) {
        }

        // TODO: Check required fields

        if (true) {
            throw new FaultException(Fault.TODO_IMPLEMENT);
        }

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        reservationRequestManager.create(reservationRequest);

        entityManager.getTransaction().commit();
        entityManager.close();

        return domain.formatIdentifier(reservationRequest.getId());
    }

    @Override
    public void modifyReservationRequest(API.SecurityToken token, String reservationId, Map attributes)
            throws FaultException
    {
        throw new FaultException(Fault.TODO_IMPLEMENT);
    }

    @Override
    public void deleteReservationRequest(API.SecurityToken token, String reservationId) throws FaultException
    {
        throw new FaultException(Fault.TODO_IMPLEMENT);
    }
}
