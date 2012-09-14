package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.report.Report;
import cz.cesnet.shongo.controller.request.report.SpecificationNotReadyReport;
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
    private Specification specification;

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
     * @return {@link #specification}
     */
    @ManyToOne(cascade = CascadeType.ALL)
    public Specification getSpecification()
    {
        return specification;
    }

    /**
     * @param specification sets the {@link #specification}
     */
    public void setSpecification(Specification specification)
    {
        this.specification = specification;
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
     * Update state of the {@link ReservationRequest} based on {@link #specification}.
     * <p/>
     * If {@link #specification} is instance of {@link StatefulSpecification} and it's
     * {@link StatefulSpecification#getCurrentState()} is {@link StatefulSpecification.State#NOT_READY}
     * the state of {@link ReservationRequest} is set to {@link State#NOT_COMPLETE}.
     * Otherwise the state is not changed or forced to {@link State#COMPLETE} in incorrect cases.
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
        if (specification instanceof StatefulSpecification) {
            StatefulSpecification statefulSpecification = (StatefulSpecification) specification;
            if (statefulSpecification.getCurrentState().equals(StatefulSpecification.State.NOT_READY)) {
                newState = State.NOT_COMPLETE;
                reports.add(new SpecificationNotReadyReport(specification));
            }
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
         * Specification is instance of {@link StatefulSpecification} and it's
         * {@link StatefulSpecification#getCurrentState()} is {@link StatefulSpecification.State#NOT_READY}.
         * <p/>
         * A {@link ReservationRequest} in {@link #NOT_COMPLETE} state become {@link #COMPLETE} when
         * the {@link Specification} become {@link StatefulSpecification.State#READY}
         * or {@link StatefulSpecification.State#SKIP}.
         */
        NOT_COMPLETE,

        /**
         * Specification is not instance of {@link StatefulSpecification} or it has
         * {@link StatefulSpecification#getCurrentState()} {@link StatefulSpecification.State#READY} or
         * {@link StatefulSpecification.State#SKIP}.
         * <p/>
         * The {@link ReservationRequest} hasn't been allocated by the {@link Scheduler} yet or
         * the {@link ReservationRequest} has been modified so the {@link Scheduler} must reallocate it.
         * <p/>
         * The {@link Scheduler} processes only {@link #COMPLETE} {@link ReservationRequest}s.
         */
        COMPLETE,

        /**
         * {@link ReservationRequest} has been successfully allocated. If the {@link ReservationRequest} becomes
         * modified, it's state changes back to {@link #COMPLETE}.
         */
        ALLOCATED,

        /**
         * Allocation of the {@link ReservationRequest} failed. The reason can be found from
         * the {@link ReservationRequest#getReports()}
         */
        ALLOCATION_FAILED
    }

    @Override
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("state", getState());
        map.put("slot", getRequestedSlot());
        map.put("specification", getSpecification());
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
