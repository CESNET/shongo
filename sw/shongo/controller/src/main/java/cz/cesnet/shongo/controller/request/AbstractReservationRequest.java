package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestType;
import cz.cesnet.shongo.controller.report.ReportablePersistentObject;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.apache.commons.lang.ObjectUtils;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import javax.persistence.*;
import java.util.Map;

/**
 * Represents a common attributes for all types of reservation requests.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class AbstractReservationRequest extends ReportablePersistentObject
{
    /**
     * Date/time when the reservation request was created.
     */
    private DateTime created;

    /**
     * Type of the reservation. Permanent reservation are created by resource owners to
     * allocate the resource for theirs activity.
     */
    private ReservationRequestType type;

    /**
     * Purpose for the reservation (science/education).
     */
    private ReservationRequestPurpose purpose;

    /**
     * Name of the reservation that is shown to users.
     */
    private String name;

    /**
     * Description of the reservation that is shown to users.
     */
    private String description;

    /**
     * Option that specifies whether inter-domain resource lookup can be performed.
     */
    private boolean interDomain;

    /**
     * @return {@link #created}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.FIELD)
    public DateTime getCreated()
    {
        return created;
    }

    /**
     * @return {@link #type}
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public ReservationRequestType getType()
    {
        return type;
    }

    /**
     * @param type sets the {@link #type}
     */
    public void setType(ReservationRequestType type)
    {
        this.type = type;
    }

    /**
     * @return {@link #purpose}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public ReservationRequestPurpose getPurpose()
    {
        return purpose;
    }

    /**
     * @param purpose sets the {@link #purpose}
     */
    public void setPurpose(ReservationRequestPurpose purpose)
    {
        this.purpose = purpose;
    }

    /**
     * @return {@link #name}
     */
    @Column
    public String getName()
    {
        return name;
    }

    /**
     * @param name sets the {@link #name}
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return {@link #name}
     */
    @Column
    public String getDescription()
    {
        return description;
    }

    /**
     * @param description sets the {@link #description}
     */
    public void setDescription(String description)
    {
        this.description = description;
    }


    /**
     * @return {@link #interDomain}
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    public boolean isInterDomain()
    {
        return interDomain;
    }

    /**
     * @param interDomain sets the {@link #interDomain}
     */
    public void setInterDomain(boolean interDomain)
    {
        this.interDomain = interDomain;
    }

    /**
     * Synchronize properties from given {@code abstractReservationRequest}.
     *
     * @param abstractReservationRequest from which will be copied all properties values to
     *                                   this {@link AbstractReservationRequest}
     * @return true if some modification was made
     */
    public boolean synchronizeFrom(AbstractReservationRequest abstractReservationRequest)
    {
        boolean modified = !ObjectUtils.equals(getType(), abstractReservationRequest.getType())
                || !ObjectUtils.equals(getPurpose(), abstractReservationRequest.getPurpose())
                || !ObjectUtils.equals(getName(), abstractReservationRequest.getName())
                || !ObjectUtils.equals(getDescription(), abstractReservationRequest.getDescription())
                || !ObjectUtils.equals(isInterDomain(), abstractReservationRequest.isInterDomain());
        setType(abstractReservationRequest.getType());
        setPurpose(abstractReservationRequest.getPurpose());
        setName(abstractReservationRequest.getName());
        setDescription(abstractReservationRequest.getDescription());
        setInterDomain(abstractReservationRequest.isInterDomain());
        return modified;
    }

    @PrePersist
    protected void onCreate()
    {
        created = DateTime.now();
    }

    /**
     * @param entityManager
     * @param domain
     * @return converted reservation request to API
     * @throws cz.cesnet.shongo.fault.FaultException
     *
     */
    public cz.cesnet.shongo.controller.api.ReservationRequest toApi(EntityManager entityManager, Domain domain)
            throws FaultException
    {
        throw new TodoImplementException();
        /*cz.cesnet.shongo.controller.api.ReservationRequest reservationRequest =
                new cz.cesnet.shongo.controller.api.ReservationRequest();

        reservationRequest.setIdentifier(domain.formatIdentifier(getId()));
        reservationRequest.setCreated(getCreated());
        reservationRequest.setType(getType());
        reservationRequest.setName(getName());
        reservationRequest.setDescription(getDescription());
        reservationRequest.setPurpose(getPurpose());
        reservationRequest.setInterDomain(isInterDomain());

        for (DateTimeSlot dateTimeSlot : getRequestedSlots()) {
            reservationRequest.addSlot(dateTimeSlot.toApi());
        }

        for (Compartment compartment : getRequestedCompartments()) {
            reservationRequest.addCompartment(compartment.toApi(domain));
        }

        CompartmentRequestManager compartmentRequestManager = new CompartmentRequestManager(entityManager);
        AllocatedCompartmentManager allocatedCompartmentManager = new AllocatedCompartmentManager(entityManager);
        List<CompartmentRequest> compartmentRequestList = compartmentRequestManager.listByReservationRequest(this);
        for (CompartmentRequest compartmentRequest : compartmentRequestList) {
            cz.cesnet.shongo.controller.api.ReservationRequest.Request request =
                    new cz.cesnet.shongo.controller.api.ReservationRequest.Request();
            request.setSlot(compartmentRequest.getRequestedSlot());
            switch (compartmentRequest.getState()) {
                case NOT_COMPLETE:
                    request.setState(cz.cesnet.shongo.controller.api.ReservationRequest.Request.State.NOT_COMPLETE);
                    break;
                case ALLOCATED:
                    request.setState(cz.cesnet.shongo.controller.api.ReservationRequest.Request.State.ALLOCATED);
                    break;
                case ALLOCATION_FAILED:
                    request.setState(
                            cz.cesnet.shongo.controller.api.ReservationRequest.Request.State.ALLOCATION_FAILED);
                    break;
                default:
                    request.setState(cz.cesnet.shongo.controller.api.ReservationRequest.Request.State.NOT_ALLOCATED);
                    break;
            }
            request.setStateReport(compartmentRequest.getReportText());
            reservationRequest.addRequest(request);
        }

        return reservationRequest;*/
    }

    /**
     * Synchronize reservation request from API
     *
     * @param api
     * @param entityManager
     * @param domain
     * @throws cz.cesnet.shongo.fault.FaultException
     *
     */
    public <API extends cz.cesnet.shongo.controller.api.ReservationRequest>
    void fromApi(API api, EntityManager entityManager, Domain domain) throws FaultException
    {
        throw new TodoImplementException();
        /*// Modify attributes
        if (api.isPropertyFilled(API.TYPE)) {
            setType(api.getType());
        }
        if (api.isPropertyFilled(API.NAME)) {
            setName(api.getName());
        }
        if (api.isPropertyFilled(API.DESCRIPTION)) {
            setDescription(api.getDescription());
        }
        if (api.isPropertyFilled(API.PURPOSE)) {
            setPurpose(api.getPurpose());
        }
        if (api.isPropertyFilled(API.INTER_DOMAIN)) {
            setInterDomain(api.getInterDomain());
        }

        // Create/modify requested slots
        for (cz.cesnet.shongo.controller.api.DateTimeSlot apiSlot : api.getSlots()) {
            // Create new requested slot
            if (api.isCollectionItemMarkedAsNew(API.SLOTS, apiSlot)) {
                fromApiCreateRequestedSlot(apiSlot);
            }
            else {
                // Modify existing requested slot
                DateTimeSlot dateTimeSlot = getRequestedSlotById(apiSlot.getId().longValue());
                dateTimeSlot.setDuration(apiSlot.getDuration());

                entityManager.remove(dateTimeSlot.getStart());

                Object dateTime = apiSlot.getStart();
                if (dateTime instanceof DateTime) {
                    if (!(dateTimeSlot.getStart() instanceof AbsoluteDateTimeSpecification)
                            || !((DateTime) dateTime).isEqual(((AbsoluteDateTimeSpecification) dateTimeSlot
                            .getStart()).getDateTime())) {
                        dateTimeSlot.setStart(new AbsoluteDateTimeSpecification((DateTime) dateTime));
                    }
                }
                else if (dateTime instanceof PeriodicDateTime) {
                    PeriodicDateTime periodic = (PeriodicDateTime) dateTime;
                    dateTimeSlot.setStart(new PeriodicDateTimeSpecification(periodic.getStart(),
                            periodic.getPeriod(), periodic.getEnd()));
                }
            }
        }
        // Delete requested slots
        Set<cz.cesnet.shongo.controller.api.DateTimeSlot> apiDeletedSlots =
                api.getCollectionItemsMarkedAsDeleted(API.SLOTS);
        for (cz.cesnet.shongo.controller.api.DateTimeSlot apiSlot : apiDeletedSlots) {
            removeRequestedSlot(getRequestedSlotById(apiSlot.getId().longValue()));
        }

        // Create/modify requested compartments
        for (cz.cesnet.shongo.controller.api.Compartment apiCompartment : api.getSpecifications()) {
            // Create/modify requested compartment
            Compartment compartment = null;
            if (api.isCollectionItemMarkedAsNew(API.COMPARTMENTS, apiCompartment)) {
                compartment = addRequestedCompartment();
            }
            else {
                compartment = getRequestedCompartmentById(apiCompartment.getId().longValue());
            }
            compartment.fromApi(apiCompartment, entityManager, domain);
        }
        // Delete requested compartments
        Set<cz.cesnet.shongo.controller.api.Compartment> apiDeletedCompartments =
                api.getCollectionItemsMarkedAsDeleted(API.COMPARTMENTS);
        for (cz.cesnet.shongo.controller.api.Compartment apiCompartment : apiDeletedCompartments) {
            removeRequestedCompartment(getRequestedCompartmentById(apiCompartment.getId().longValue()));
        }*/

        // TODO: Delete from db deleted compartments that aren't referenced from compartment requests
        // TODO: Think up how to delete all other objects (e.g. slots)
    }

    /**
     * Create new requested slot in given reservation request from the given
     * {@link cz.cesnet.shongo.controller.api.DateTimeSlot}.
     *
     * @param dateTimeSlot
     * @throws cz.cesnet.shongo.fault.FaultException
     *
     */
    private void fromApiCreateRequestedSlot(cz.cesnet.shongo.controller.api.DateTimeSlot dateTimeSlot)
            throws FaultException
    {
        throw new TodoImplementException();
        /*Object dateTime = dateTimeSlot.getStart();
        if (dateTime instanceof DateTime) {
            addRequestedSlot(
                    new AbsoluteDateTimeSpecification((DateTime) dateTime),
                    dateTimeSlot.getDuration());
        }
        else if (dateTime instanceof PeriodicDateTime) {
            PeriodicDateTime periodic = (PeriodicDateTime) dateTime;
            addRequestedSlot(
                    new PeriodicDateTimeSpecification(periodic.getStart(),
                            periodic.getPeriod()), dateTimeSlot.getDuration());
        }
        else {
            throw new FaultException("Unknown date/time type.");
        }*/
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("type", getType());
        map.put("purpose", getPurpose());
    }
}
