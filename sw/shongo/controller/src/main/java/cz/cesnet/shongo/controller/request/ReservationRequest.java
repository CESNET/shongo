package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.api.Fault;
import cz.cesnet.shongo.api.FaultException;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.ReservationRequestPurpose;
import cz.cesnet.shongo.controller.ReservationRequestType;
import cz.cesnet.shongo.controller.allocation.AllocatedCompartmentManager;
import cz.cesnet.shongo.controller.api.ControllerFault;
import cz.cesnet.shongo.controller.api.PeriodicDateTime;
import cz.cesnet.shongo.controller.common.AbsoluteDateTimeSpecification;
import cz.cesnet.shongo.controller.common.DateTimeSlot;
import cz.cesnet.shongo.controller.common.DateTimeSpecification;
import cz.cesnet.shongo.controller.common.PeriodicDateTimeSpecification;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import javax.persistence.*;
import java.util.*;

/**
 * Represents a request created by an user to get allocated some resources for videoconference calls.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ReservationRequest extends PersistentObject
{
    /**
     * State of reservation request.
     */
    public static enum State
    {
        /**
         * State tells that reservation request hasn't corresponding compartment requests created
         * or the reservation request has changed they are out-of-sync.
         */
        NOT_PREPROCESSED,

        /**
         * State tells that reservation request has corresponding compartment requests synced.
         */
        PREPROCESSED
    }

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
     * List of date/time slots for which the reservation is requested.
     */
    private List<DateTimeSlot> requestedSlots = new ArrayList<DateTimeSlot>();

    /**
     * List of compartments that are requested for a reservation. Each
     * compartment represents a group of resources/persons that will
     * be used/participate in a separate videoconference call.
     */
    private List<Compartment> requestedCompartments = new ArrayList<Compartment>();

    /**
     * Specifies the default option who should initiate the call for all requested resources.
     */
    private CallInitiation callInitiation;

    /**
     * Option that specifies whether inter-domain resource lookup can be performed.
     */
    private boolean interDomain;

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
     * @return {@link #requestedSlots}
     */
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @Access(AccessType.FIELD)
    public List<DateTimeSlot> getRequestedSlots()
    {
        return Collections.unmodifiableList(requestedSlots);
    }

    /**
     * @param id
     * @return requested slot with given {@code id}
     * @throws FaultException
     */
    public DateTimeSlot getRequestedSlotById(Long id) throws FaultException
    {
        for (DateTimeSlot dateTimeSlot : requestedSlots) {
            if (dateTimeSlot.getId().equals(id)) {
                return dateTimeSlot;
            }
        }
        throw new FaultException(Fault.Common.RECORD_NOT_EXIST, DateTimeSlot.class, id);
    }

    /**
     * @param requestedSlot slot to be added to the list of requested slots
     */
    public void addRequestedSlot(DateTimeSlot requestedSlot)
    {
        requestedSlots.add(requestedSlot);
    }

    /**
     * Add slot to the list of requested slots
     *
     * @param dateTime slot date/time
     * @param duration slot duration
     */
    public void addRequestedSlot(DateTimeSpecification dateTime, Period duration)
    {
        requestedSlots.add(new DateTimeSlot(dateTime, duration));
    }

    /**
     * @param requestedSlot slot to be removed from the {@link #requestedSlots}
     */
    public void removeRequestedSlot(DateTimeSlot requestedSlot)
    {
        requestedSlots.remove(requestedSlot);
    }

    /**
     * @return {@link #requestedCompartments}
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "reservationRequest")
    @Access(AccessType.FIELD)
    public List<Compartment> getRequestedCompartments()
    {
        return Collections.unmodifiableList(requestedCompartments);
    }

    /**
     * @param id
     * @return requested compartment with given {@code id}
     * @throws FaultException
     */
    public Compartment getRequestedCompartmentById(Long id) throws FaultException
    {
        for (Compartment compartment : requestedCompartments) {
            if (compartment.getId().equals(id)) {
                return compartment;
            }
        }
        throw new FaultException(Fault.Common.RECORD_NOT_EXIST, Compartment.class, id);
    }

    /**
     * @param compartment compartment to be added to the {@link #requestedCompartments}
     */
    public void addRequestedCompartment(Compartment compartment)
    {
        // Manage bidirectional association
        if (requestedCompartments.contains(compartment) == false) {
            requestedCompartments.add(compartment);
            compartment.setReservationRequest(this);
        }
    }

    /**
     * @param compartment compartment to be removed from the {@link #requestedCompartments}
     */
    public void removeRequestedCompartment(Compartment compartment)
    {
        // Manage bidirectional association
        if (requestedCompartments.contains(compartment)) {
            requestedCompartments.remove(compartment);
            compartment.setReservationRequest(null);
        }
    }

    /**
     * @return a new compartment that was added to the list of requested resources
     */
    public Compartment addRequestedCompartment()
    {
        Compartment compartment = new Compartment();
        addRequestedCompartment(compartment);
        return compartment;
    }

    /**
     * @return {@link #callInitiation}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public CallInitiation getCallInitiation()
    {
        return callInitiation;
    }

    /**
     * @param callInitiation sets the {@link #callInitiation}
     */
    public void setCallInitiation(CallInitiation callInitiation)
    {
        this.callInitiation = callInitiation;
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

    @Override
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        map.put("type", getType().toString());
        if (getPurpose() != null) {
            map.put("purpose", getPurpose().toString());
        }
        addCollectionToMap(map, "slots", requestedSlots);
        addCollectionToMap(map, "compartments", requestedCompartments);
    }

    /**
     * Enumerate requested date/time slots in a specific interval.
     *
     * @param interval
     * @return list of all requested absolute date/time slots for given interval
     */
    public List<Interval> enumerateRequestedSlots(Interval interval)
    {
        List<Interval> enumeratedSlots = new ArrayList<Interval>();
        for (DateTimeSlot slot : requestedSlots) {
            enumeratedSlots.addAll(slot.enumerate(interval));
        }
        return enumeratedSlots;
    }

    /**
     * @param referenceDateTime
     * @return true whether reservation request has any requested slot after given reference date/time,
     *         false otherwise
     */
    public boolean hasRequestedSlotAfter(DateTime referenceDateTime)
    {
        for (DateTimeSlot slot : requestedSlots) {
            if (slot.getStart().willOccur(referenceDateTime)) {
                return true;
            }
        }
        return false;
    }

    private static Class ReservationRequestApi = cz.cesnet.shongo.controller.api.ReservationRequest.class;

    /**
     * @param entityManager
     * @param domain
     * @return converted reservation request to API
     * @throws FaultException
     */
    public cz.cesnet.shongo.controller.api.ReservationRequest toApi(EntityManager entityManager, Domain domain)
            throws FaultException
    {
        cz.cesnet.shongo.controller.api.ReservationRequest reservationRequest =
                new cz.cesnet.shongo.controller.api.ReservationRequest();

        reservationRequest.setIdentifier(domain.formatIdentifier(getId()));
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
            request.setStart(compartmentRequest.getRequestedSlot().getStart());
            request.setDuration(compartmentRequest.getRequestedSlot().toPeriod());
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
            request.setStateDescription(compartmentRequest.getStateDescription());
            reservationRequest.addRequest(request);
        }

        return reservationRequest;
    }

    /**
     * Synchronize reservation request from API
     *
     * @param api
     * @param entityManager
     * @param domain
     * @throws FaultException
     */
    public <API extends cz.cesnet.shongo.controller.api.ReservationRequest>
    void fromApi(API api, EntityManager entityManager, Domain domain) throws FaultException
    {
        // Modify attributes
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
                            periodic.getPeriod()));
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
        for (cz.cesnet.shongo.controller.api.Compartment apiCompartment : api.getCompartments()) {
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
        }

        // TODO: Delete from db deleted compartments that aren't referenced from compartment requests
        // TODO: Think up how to delete all other objects (e.g. slots)
    }

    /**
     * Create new requested slot in given reservation request from the given
     * {@link cz.cesnet.shongo.controller.api.DateTimeSlot}.
     *
     * @param dateTimeSlot
     * @throws FaultException
     */
    private void fromApiCreateRequestedSlot(cz.cesnet.shongo.controller.api.DateTimeSlot dateTimeSlot)
            throws FaultException
    {
        Object dateTime = dateTimeSlot.getStart();
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
            throw new FaultException(ControllerFault.Common.UNKNOWN_FAULT,
                    "Unknown date/time type.");
        }
    }
}
