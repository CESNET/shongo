package cz.cesnet.shongo.controller.api;


import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.Technology;
import cz.cesnet.shongo.controller.api.util.Converter;
import cz.cesnet.shongo.controller.request.ReservationRequestManager;
import org.joda.time.DateTime;

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
    public String createReservationRequest(SecurityToken token, ReservationRequest reservationRequest)
            throws FaultException
    {
        reservationRequest.checkRequiredPropertiesFilled();

        EntityManager entityManager = getEntityManager();
        entityManager.getTransaction().begin();

        cz.cesnet.shongo.controller.request.ReservationRequest reservationRequestImpl =
                new cz.cesnet.shongo.controller.request.ReservationRequest();
        reservationRequestImpl.setType(reservationRequest.getType());
        reservationRequestImpl.setPurpose(reservationRequest.getPurpose());
        for (DateTimeSlot dateTimeSlot : reservationRequest.getSlots()) {
            Object dateTime = dateTimeSlot.getStart();
            if (dateTime instanceof DateTime) {
                reservationRequestImpl.addRequestedSlot(
                        new cz.cesnet.shongo.controller.common.AbsoluteDateTimeSpecification((DateTime) dateTime),
                        dateTimeSlot.getDuration());
            }
            else if (dateTime instanceof PeriodicDateTime) {
                PeriodicDateTime periodic = (PeriodicDateTime) dateTime;
                reservationRequestImpl.addRequestedSlot(
                        new cz.cesnet.shongo.controller.common.PeriodicDateTimeSpecification(periodic.getStart(),
                                periodic.getPeriod()), dateTimeSlot.getDuration());
            }
            else {
                throw new FaultException(ControllerFault.Common.UNKNOWN_FAULT,
                        "Unknown date/time type.");
            }
        }
        for (Compartment compartment : reservationRequest.getCompartments()) {
            cz.cesnet.shongo.controller.request.Compartment compartmentImpl =
                    reservationRequestImpl.addRequestedCompartment();
            for (Person person : compartment.getPersons()) {
                compartmentImpl.addRequestedPerson(
                        new cz.cesnet.shongo.controller.common.Person(person.getName(), person.getEmail()));
            }
            for (Compartment.ResourceSpecificationMap map : compartment
                    .getResources()) {
                cz.cesnet.shongo.controller.request.ResourceSpecification resourceSpecification = null;
                if (map.containsKey("technology")) {
                    Technology technology = Converter
                            .convertStringToEnum((String) map.get("technology"), Technology.class);
                    if (map.containsKey("count")) {
                        resourceSpecification = new cz.cesnet.shongo.controller.request.ExternalEndpointSpecification(
                                technology, (Integer) map.get("count"));
                    }
                    else {
                        resourceSpecification = new cz.cesnet.shongo.controller.request.ExternalEndpointSpecification(
                                technology, (Integer) map.get("count"));
                    }
                }
                // Check resource specification existence
                if (resourceSpecification == null) {
                    throw new FaultException(ControllerFault.TODO_IMPLEMENT);
                }
                // Fill requested persons
                if (map.containsKey("persons")) {
                    for (Object object : (Object[]) map.get("persons")) {
                        cz.cesnet.shongo.controller.api.Person person =
                                Converter.convert(object, cz.cesnet.shongo.controller.api.Person.class);
                        resourceSpecification.addRequestedPerson(
                                new cz.cesnet.shongo.controller.common.Person(person.getName(), person.getEmail()));
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

    @Override
    public cz.cesnet.shongo.controller.api.ReservationRequest getReservationRequest(SecurityToken token,
            String reservationRequestIdentifier)
    {
        Long reservationRequestId = domain.parseIdentifier(reservationRequestIdentifier);

        EntityManager entityManager = getEntityManager();
        ReservationRequestManager reservationRequestManager = new ReservationRequestManager(entityManager);
        cz.cesnet.shongo.controller.request.ReservationRequest reservationRequestImpl =
                reservationRequestManager.get(reservationRequestId);

        ReservationRequest reservationRequest = new ReservationRequest();
        reservationRequest.setIdentifier(domain.formatIdentifier(reservationRequestImpl.getId()));
        reservationRequest.setType(reservationRequestImpl.getType());
        reservationRequest.setPurpose(reservationRequestImpl.getPurpose());

        // TODO: fill other attributes

        return reservationRequest;
    }
}
