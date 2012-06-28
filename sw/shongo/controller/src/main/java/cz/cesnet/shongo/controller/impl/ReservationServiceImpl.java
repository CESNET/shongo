package cz.cesnet.shongo.controller.impl;

import cz.cesnet.shongo.common.api.FaultException;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.api.API;
import cz.cesnet.shongo.controller.api.Fault;
import cz.cesnet.shongo.controller.api.ReservationService;
import cz.cesnet.shongo.controller.request.ReservationRequest;

import javax.persistence.EntityManager;
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

        Map xxx = apiReservationRequest.toMap();

        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        ReservationRequest reservationRequest = new ReservationRequest();

        if (true) {
            throw new FaultException(Fault.TODO_IMPLEMENT);
        }

        /*EntityMap reservationRequestMap = new EntityMap(attributes, ReservationRequest.class);

        reservationRequest.setType(reservationRequestMap.getEnumRequired("type", ReservationRequest.Type.class));
        reservationRequest.setPurpose(reservationRequestMap.getEnumRequired("purpose", ReservationRequest.Purpose.class));

        for ( Map slot : reservationRequestMap.getCollectionRequired("slots", Map.class)) {
            EntityMap slotMap = new EntityMap(slot, "DateTimeSlot");
            Object dateTime = slotMap.getAttribute("dateTime", new Class[]{String.class, Map.class});
            Period duration = Converter.stringToPeriod(slotMap.getAttribute("duration", String.class));
            DateTimeSpecification dateTimeSpecification = null;
            if ( dateTime instanceof String ) {
                dateTimeSpecification = new AbsoluteDateTimeSpecification(
                        Converter.stringToDateTime((String) dateTime));
            }
            else if ( dateTime instanceof Map) {
                EntityMap dateTimeMap = new EntityMap((Map)dateTime, "PeriodicDateTime");
                PeriodicDateTimeSpecification periodicDateTimeSpecification = new PeriodicDateTimeSpecification();
                periodicDateTimeSpecification.setStart(
                        Converter.stringToDateTime(dateTimeMap.getAttribute("start", String.class)));
                periodicDateTimeSpecification.setPeriod(
                        Converter.stringToPeriod(dateTimeMap.getAttribute("period", String.class)));
                dateTimeSpecification = periodicDateTimeSpecification;
            }
            reservationRequest.addRequestedSlot(dateTimeSpecification, duration);
        }
        for ( Map compartment : reservationRequestMap.getCollectionRequired("compartments", Map.class)) {
            EntityMap compartmentMap = new EntityMap(compartment, "Compartment");
            throw new FaultException(Fault.TODO_IMPLEMENT);
        }

        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        reservationRequestManager.create(reservationRequest);*/

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
