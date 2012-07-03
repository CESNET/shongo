package cz.cesnet.shongo.controller.impl;

import cz.cesnet.shongo.api.ControllerFault;
import cz.cesnet.shongo.api.FaultException;
import cz.cesnet.shongo.api.SecurityToken;
import cz.cesnet.shongo.common.AbsoluteDateTimeSpecification;
import cz.cesnet.shongo.common.PeriodicDateTimeSpecification;
import cz.cesnet.shongo.common.Person;
import cz.cesnet.shongo.common.util.Converter;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.Technology;
import cz.cesnet.shongo.controller.request.*;
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
        reservationRequest.checkRequiredPropertiesFilled();

        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        ReservationRequest reservationRequestImpl = new ReservationRequest();
        reservationRequestImpl.setType(reservationRequest.getType());
        reservationRequestImpl.setPurpose(reservationRequest.getPurpose());
        for (cz.cesnet.shongo.api.DateTimeSlot dateTimeSlot : reservationRequest.getSlots()) {
            Object dateTime = dateTimeSlot.getStart();
            if (dateTime instanceof DateTime) {
                reservationRequestImpl.addRequestedSlot(new AbsoluteDateTimeSpecification((DateTime) dateTime),
                        dateTimeSlot.getDuration());
            }
            else if (dateTime instanceof cz.cesnet.shongo.api.PeriodicDateTime) {
                cz.cesnet.shongo.api.PeriodicDateTime periodic = (cz.cesnet.shongo.api.PeriodicDateTime) dateTime;
                reservationRequestImpl.addRequestedSlot(new PeriodicDateTimeSpecification(periodic.getStart(),
                        periodic.getPeriod()), dateTimeSlot.getDuration());
            }
            else {
                throw new FaultException(ControllerFault.Common.UNKNOWN_FAULT,
                        "Unknown date/time type.");
            }
        }
        for (cz.cesnet.shongo.api.Compartment compartment : reservationRequest.getCompartments()) {
            Compartment compartmentImpl = reservationRequestImpl.addRequestedCompartment();
            for ( cz.cesnet.shongo.api.Person person : compartment.getPersons()) {
                compartmentImpl.addRequestedPerson(new Person(person.getName(), person.getEmail()));
            }
            for ( cz.cesnet.shongo.api.Compartment.ResourceSpecificationMap map : compartment.getResources()) {
                ResourceSpecification resourceSpecification = null;
                if ( map.containsKey("technology") ) {
                    Technology technology = Converter.convertStringToEnum((String) map.get("technology"), Technology.class);
                    if ( map.containsKey("count")) {
                        resourceSpecification = new ExternalEndpointSpecification(technology, (Integer) map.get("count"));
                    } else {
                        resourceSpecification = new ExternalEndpointSpecification(technology, (Integer) map.get("count"));
                    }
                }
                // Check resource specification existence
                if ( resourceSpecification == null ) {
                    throw new FaultException(ControllerFault.TODO_IMPLEMENT);
                }
                // Fill requested persons
                if ( map.containsKey("persons") ) {
                    for ( Object object : (Object[])map.get("persons")) {
                        cz.cesnet.shongo.api.Person person =
                                Converter.convert(object, cz.cesnet.shongo.api.Person.class);
                        resourceSpecification.addRequestedPerson(new Person(person.getName(), person.getEmail()));
                    }
                }
                compartmentImpl.addRequestedResource(resourceSpecification);
            }
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
