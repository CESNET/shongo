package cz.cesnet.shongo.controller.impl;

import cz.cesnet.shongo.common.AbsoluteDateTimeSpecification;
import cz.cesnet.shongo.common.DateTimeSlot;
import cz.cesnet.shongo.common.DateTimeSpecification;
import cz.cesnet.shongo.common.PeriodicDateTimeSpecification;
import cz.cesnet.shongo.common.api.SecurityToken;
import cz.cesnet.shongo.common.util.Converter;
import cz.cesnet.shongo.common.util.EntityMap;
import cz.cesnet.shongo.common.xmlrpc.FaultException;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.api.Fault;
import cz.cesnet.shongo.controller.api.ReservationService;
import cz.cesnet.shongo.controller.request.ReservationRequest;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import org.joda.time.DateTime;
import org.joda.time.Period;

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
            throw new IllegalStateException(getClass().getName() + " doesn't have the domain  set!");
        }
    }

    @Override
    public String getServiceName()
    {
        return "Reservation";
    }

    /**
     * @param dateTime
     * @return parsed date/time from given string
     * @throws FaultException
     */
    private DateTime parseDateTime(String dateTime) throws FaultException
    {
        try {
            return DateTime.parse(dateTime);
        } catch (Exception exception) {
            throw new FaultException(Fault.Common.UNKNOWN_FAULT, "Failed to parse date/time '" + dateTime + "'.");
        }
    }

    /**
     * @param period
     * @return parsed period from given string
     * @throws FaultException
     */
    private Period parsePeriod(String period) throws FaultException
    {
        try {
            return Period.parse(period);
        } catch (Exception exception) {
            throw new FaultException(Fault.Common.UNKNOWN_FAULT, "Failed to parse duration '" + period + "'.");
        }
    }

    @Override
    public String createReservationRequest(SecurityToken token, String type, Map attributes)
            throws FaultException
    {
        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        EntityMap reservationRequestMap = new EntityMap(attributes, ReservationRequest.class);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setType(Converter.convertStringToEnum(type, ReservationRequest.Type.class));
        reservationRequest.setPurpose(reservationRequestMap.getEnumRequired("purpose", ReservationRequest.Purpose.class));

        for ( Map slot : reservationRequestMap.getCollectionRequired("slots", Map.class)) {
            EntityMap slotMap = new EntityMap(slot, "DateTimeSlot");
            Object dateTime = slotMap.getAttribute("dateTime", new Class[]{String.class, Map.class});
            Period duration = parsePeriod(slotMap.getAttribute("duration", String.class));
            DateTimeSpecification dateTimeSpecification = null;
            if ( dateTime instanceof String ) {
                dateTimeSpecification = new AbsoluteDateTimeSpecification(parseDateTime((String)dateTime));
            }
            else if ( dateTime instanceof Map) {
                EntityMap dateTimeMap = new EntityMap((Map)dateTime, "PeriodicDateTime");
                PeriodicDateTimeSpecification periodicDateTimeSpecification = new PeriodicDateTimeSpecification();
                periodicDateTimeSpecification.setStart(parseDateTime(dateTimeMap.getAttribute("start", String.class)));
                periodicDateTimeSpecification.setPeriod(parsePeriod(dateTimeMap.getAttribute("period", String.class)));
                dateTimeSpecification = periodicDateTimeSpecification;
            }
            reservationRequest.addRequestedSlot(dateTimeSpecification, duration);
        }
        for ( Object compartment : reservationRequestMap.getCollectionRequired("compartments", Map.class)) {
            throw new FaultException(Fault.TODO_IMPLEMENT);
        }

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
