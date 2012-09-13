package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.request.report.SpecificationNotReadyReport;
import cz.cesnet.shongo.controller.report.Report;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a request created by an user to get allocated some resources for video conference calls.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ReservationRequest extends AbstractReservationRequest
{
    /**
     * Start date/time from which the reservation is requested.
     */
    private DateTime requestedSlotStart;

    /**
     * End date/time to which the reservation is requested.
     */
    private DateTime requestedSlotEnd;

    /**
     * {@link Specification} of target which is requested for a reservation.
     */
    private Specification requestedSpecification;

    /**
     * State of the compartment request.
     */
    private State state;

    /**
     * @return {@link #requestedSlotStart}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.PROPERTY)
    public DateTime getRequestedSlotStart()
    {
        return requestedSlotStart;
    }

    /**
     * @param requestedSlotStart sets the {@link #requestedSlotStart}
     */
    public void setRequestedSlotStart(DateTime requestedSlotStart)
    {
        this.requestedSlotStart = requestedSlotStart;
    }

    /**
     * @return {@link #requestedSlotEnd}
     */
    @Column
    @Type(type = "DateTime")
    @Access(AccessType.PROPERTY)
    public DateTime getRequestedSlotEnd()
    {
        return requestedSlotEnd;
    }

    /**
     * @param requestedSlotEnd sets the {@link #requestedSlotEnd}
     */
    public void setRequestedSlotEnd(DateTime requestedSlotEnd)
    {
        this.requestedSlotEnd = requestedSlotEnd;
    }

    /**
     * @return requested slot ({@link #requestedSlotStart}, {@link #requestedSlotEnd})
     */
    @Transient
    public Interval getRequestedSlot()
    {
        return new Interval(requestedSlotStart, requestedSlotEnd);
    }

    /**
     * @param requestedSlot sets the requested slot
     */
    @Transient
    public void setRequestedSlot(Interval requestedSlot)
    {
        setRequestedSlotStart(requestedSlot.getStart());
        setRequestedSlotEnd(requestedSlot.getEnd());
    }

    /**
     * @return {@link #requestedSpecification}
     */
    @ManyToOne(cascade = CascadeType.ALL)
    public Specification getRequestedSpecification()
    {
        return requestedSpecification;
    }

    /**
     * @param requestedSpecification sets the {@link #requestedSpecification}
     */
    public void setRequestedSpecification(Specification requestedSpecification)
    {
        this.requestedSpecification = requestedSpecification;
    }

    /**
     * @return {@link #state}
     */
    @Column
    @Enumerated(EnumType.STRING)
    public State getState()
    {
        return state;
    }

    /**
     * @param state sets the {@link #state}
     */
    public void setState(State state)
    {
        this.state = state;
    }

    /**
     * Update state of the {@link ReservationRequest} based on {@link #requestedSpecification}.
     * <p/>
     * If any requested person has {@link cz.cesnet.shongo.controller.oldrequest.PersonRequest.State#NOT_ASKED} or
     * {@link cz.cesnet.shongo.controller.oldrequest.PersonRequest.State#ASKED} state the state of compartment request
     * is set to {@link State#NOT_COMPLETE}. Otherwise the state is not changed
     * or forced to {@link State#COMPLETE} in incorrect cases.
     *
     * @see State
     */
    public void updateStateBySpecifications()
    {
        State newState = getState();
        if (newState == null || newState == State.NOT_COMPLETE) {
            newState = State.COMPLETE;
        }
        List<Report> reports = new ArrayList<Report>();
        if (requestedSpecification.getCurrentState().equals(Specification.State.NOT_READY)) {
            newState = State.NOT_COMPLETE;
            reports.add(new SpecificationNotReadyReport(requestedSpecification));
        }
        if (newState != getState()) {
            setState(newState);
            setReports(reports);
        }
    }

    /**
     * Enumeration of {@link ReservationRequest} state.
     */
    public static enum State
    {
        /**
         * Some of requested persons has {@link PersonRequest.State#NOT_ASKED}
         * or {@link PersonRequest.State#ASKED} state.
         * <p/>
         * A compartment request in {@link #NOT_COMPLETE} state become {@link #COMPLETE}
         * when all requested person accepts or rejects the invitation.
         */
        NOT_COMPLETE,

        /**
         * All of the requested persons have {@link PersonRequest.State#ACCEPTED}
         * or {@link PersonRequest.State#REJECTED} state. The compartment request
         * hasn't been allocated by a scheduler yet or the request has been modified so
         * the scheduler must reallocate it.
         * <p/>
         * A scheduler processes only {@link #COMPLETE} compartment requests.
         */
        COMPLETE,

        /**
         * All requested resources by compartment request has been allocated. If the compartment
         * request becomes modified, it's state changes back to {@link #COMPLETE}.
         */
        ALLOCATED,

        /**
         * Allocation failed because some resources cannot be allocated.
         */
        ALLOCATION_FAILED
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("state", getState());
        map.put("slot", getRequestedSlot());
        map.put("specification", getRequestedSpecification());
    }

    /**
     * @return list of {@link RequestedResource}s
     */
    /*@Transient
    public List<RequestedResource> getRequestedResourcesForScheduler()
    {
        List<RequestedResource> requestedResources = new ArrayList<RequestedResource>();
        Map<Long, RequestedResource> requestedResourceById = new HashMap<Long, RequestedResource>();
        for (ResourceSpecification resourceSpecification : this.requestedResources) {
            RequestedResource requestedResource = new RequestedResource(resourceSpecification);
            requestedResources.add(requestedResource);
            requestedResourceById.put(resourceSpecification.getId(), requestedResource);
        }
        for (PersonRequest personRequest : requestedPersons) {
            if (personRequest.getState() == PersonRequest.State.ACCEPTED) {
                if (personRequest.getResourceSpecification() == null) {
                    throw new IllegalStateException("Person request '" + personRequest.getId()
                            + "' in compartment request '" + getId() + "' should have resource specified!");
                }
                RequestedResource requestedResource =
                        requestedResourceById.get(personRequest.getResourceSpecification().getId());
                if (requestedResource == null) {
                    throw new IllegalStateException("Resource '" + personRequest.getResourceSpecification().getId()
                            + "' specified in person request '" + personRequest.getId()
                            + "' doesn't exists in compartment request '" + getId() + "'!");
                }
                requestedResource.addPerson(personRequest.getPerson());
            }
        }

        // Remove all requested resources which have been requested with a not empty list of persons
        // and the current list of requested persons is empty (all persons rejected invitation)
        Iterator<RequestedResource> iterator = requestedResources.iterator();
        while (iterator.hasNext()) {
            RequestedResource requestedResource = iterator.next();
            if (requestedResource.getPersons().size() == 0
                    && requestedResource.getResourceSpecification().getRequestedPersons().size() > 0) {
                iterator.remove();
            }
        }

        // Sort requested resources by priority
        Collections.sort(requestedResources, new Comparator<RequestedResource>()
        {
            @Override
            public int compare(RequestedResource first, RequestedResource second)
            {
                return Integer.valueOf(first.getPriority()).compareTo(second.getPriority());
            }
        });

        return requestedResources;
    }*/


    /**
     * Represents a requested resource which is used in scheduling.
     */
    /*public static class RequestedResource
    {
        private ResourceSpecification resourceSpecification;

        private List<Person> persons = new ArrayList<Person>();

        public RequestedResource(ResourceSpecification resourceSpecification)
        {
            this.resourceSpecification = resourceSpecification;
        }

        public ResourceSpecification getResourceSpecification()
        {
            return resourceSpecification;
        }

        public List<Person> getPersons()
        {
            return persons;
        }

        public void addPerson(Person person)
        {
            persons.add(person);
        }

        public int getPriority()
        {
            return (resourceSpecification instanceof ExistingEndpointSpecification ? 0 : 1);
        }
    }*/
}
