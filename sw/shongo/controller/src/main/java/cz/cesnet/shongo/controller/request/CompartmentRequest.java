package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.common.PersistentObject;
import cz.cesnet.shongo.common.Person;
import org.joda.time.Interval;

import javax.persistence.*;
import java.util.*;

/**
 * Represents a {@link Compartment} that is requested for a specific date/time slot.
 * The compartment should be copied to compartment request(s), because each
 * request can be filled by different additional information.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class CompartmentRequest extends PersistentObject
{
    /**
     * Enumeration of compartment request state.
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
        ALLOCATED
    }

    /**
     * Reservation request for which the request is made.
     */
    private ReservationRequest reservationRequest;

    /**
     * Compartment for which the request is made.
     */
    private Compartment compartment;

    /**
     * Date/time slot for which the compartment is requested.
     */
    @Embedded
    @Access(AccessType.FIELD)
    private Slot requestedSlot = new Slot();

    /**
     * List of persons which are requested to participate in compartment.
     * <p/>
     * Content of this list comes from {@link #compartment} requested resources and persons.
     */
    private List<PersonRequest> requestedPersons = new ArrayList<PersonRequest>();

    /**
     * List of specifications for resources which are requested to participate in compartment.
     * <p/>
     * One resource specification can be referenced from multiple compartment requests so
     * {@link ManyToMany} association must be used. It happens when resource is specified
     * in {@link Compartment} and multiple {@link cz.cesnet.shongo.common.DateTimeSlot} are requested.
     * Resource specification can be added by user which selects a device resource which he will use
     * for connecting to compartment.
     */
    private List<ResourceSpecification> requestedResources = new ArrayList<ResourceSpecification>();

    /**
     * State of the compartment request.
     */
    private State state;

    /**
     * @return {@link #compartment}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public Compartment getCompartment()
    {
        return compartment;
    }

    /**
     * @param compartment sets the {@link #compartment}
     */
    public void setCompartment(Compartment compartment)
    {
        this.compartment = compartment;
    }

    /**
     * @return {@link #reservationRequest}
     */
    @OneToOne
    @Access(AccessType.FIELD)
    public ReservationRequest getReservationRequest()
    {
        return reservationRequest;
    }

    /**
     * @param reservationRequest sets the {@link #reservationRequest}
     */
    public void setReservationRequest(ReservationRequest reservationRequest)
    {
        this.reservationRequest = reservationRequest;
    }

    /**
     * @return interval from {@link #requestedSlot}
     */
    @Transient
    public Interval getRequestedSlot()
    {
        return requestedSlot.getInterval();
    }

    /**
     * @param requestedSlot sets the interval for {@link #requestedSlot}
     */
    public void setRequestedSlot(Interval requestedSlot)
    {
        this.requestedSlot.setInterval(requestedSlot);
        ;
    }

    /**
     * @return {@link #requestedPersons}
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "compartmentRequest")
    @Access(AccessType.FIELD)
    public List<PersonRequest> getRequestedPersons()
    {
        return Collections.unmodifiableList(requestedPersons);
    }

    /**
     * @param requestedPerson person to be added to the {@link #requestedPersons}
     */
    public void addRequestedPerson(PersonRequest requestedPerson)
    {
        // Manage bidirectional association
        if (requestedPersons.contains(requestedPerson) == false) {
            requestedPersons.add(requestedPerson);
            requestedPerson.setCompartmentRequest(this);
        }
    }

    /**
     * @param requestedPerson person to be removed from the {@link #requestedPersons}
     */
    public void removeRequestedPerson(PersonRequest requestedPerson)
    {
        // Manage bidirectional association
        if (requestedPersons.contains(requestedPerson)) {
            requestedPersons.remove(requestedPerson);
            requestedPerson.setCompartmentRequest(null);
        }
    }

    /**
     * @return {@link #requestedResources}
     */
    @ManyToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<ResourceSpecification> getRequestedResources()
    {
        return Collections.unmodifiableList(requestedResources);
    }

    /**
     * @param requestedResource resource to be added to the {@link #requestedResources}
     */
    public void addRequestedResource(ResourceSpecification requestedResource)
    {
        requestedResources.add(requestedResource);
    }

    /**
     * @param requestedResource resource to be removed from the {@link #requestedResources}
     */
    public void removeRequestedResource(ResourceSpecification requestedResource)
    {
        requestedResources.remove(requestedResource);
    }

    /**
     * @param resourceSpecification
     * @return true if given resource is present in the {@link #requestedResources}, false otherwise
     */
    public boolean containsRequestedResource(ResourceSpecification resourceSpecification)
    {
        return requestedResources.contains(resourceSpecification);
    }

    /**
     * @return map of requested resources as keys and list of requested persons for each resource as value
     */
    @Transient
    public Map<ResourceSpecification, List<Person>> getRequestedResourcesWithPersons()
    {
        HashMap<ResourceSpecification, List<Person>> mapRequestedResourcesWithPersons =
                new HashMap<ResourceSpecification, List<Person>>();
        for (ResourceSpecification resourceSpecification : requestedResources) {
            mapRequestedResourcesWithPersons.put(resourceSpecification, new ArrayList<Person>());
        }
        for (PersonRequest personRequest : requestedPersons) {
            if (personRequest.getState() == PersonRequest.State.ACCEPTED) {
                if (personRequest.getResourceSpecification() == null) {
                    throw new IllegalStateException("Person request '" + personRequest.getId()
                            + "' in compartment request '" + getId() + "' should have resource specified!");
                }
                List<Person> persons = mapRequestedResourcesWithPersons.get(personRequest.getResourceSpecification());
                if (persons == null) {
                    throw new IllegalStateException("Resource '" + personRequest.getResourceSpecification().getId()
                            + "' specified in person request '" + personRequest.getId()
                            + "' doesn't exists in compartment request '" + getId() + "'!");
                }
                persons.add(personRequest.getPerson());
            }
        }

        // Remove all resources that were requested with a list of persons and the list is empty
        Iterator<Map.Entry<ResourceSpecification, List<Person>>> iterator =
                mapRequestedResourcesWithPersons.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ResourceSpecification, List<Person>> entry = iterator.next();
            if (entry.getValue().size() == 0 && entry.getKey().getRequestedPersons().size() > 0) {
                iterator.remove();
            }
        }

        return mapRequestedResourcesWithPersons;
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
     * Clear the state of the compartment request (set it to null). Should be followed by
     * calling {@link #updateStateByRequestedPersons()} to restore proper state.
     * <p/>
     * It is useful for removing {@link State#ALLOCATED} state of the compartment request.
     */
    public void clearState()
    {
        state = null;
    }

    /**
     * Update state of the compartment request based on requested persons.
     * <p/>
     * If any requested person has {@link PersonRequest.State#NOT_ASKED} or
     * {@link PersonRequest.State#ASKED} state the state of compartment request
     * is set to {@link State#NOT_COMPLETE}. Otherwise the state is not changed
     * or forced to {@link State#COMPLETE} in incorrect cases.
     *
     * @see State
     */
    public void updateStateByRequestedPersons()
    {
        State state = getState();
        if (state == null || state == State.NOT_COMPLETE) {
            state = State.COMPLETE;
        }
        for (PersonRequest personRequest : requestedPersons) {
            PersonRequest.State personRequestState = personRequest.getState();
            if (personRequestState == PersonRequest.State.NOT_ASKED
                    || personRequestState == PersonRequest.State.ASKED) {
                state = State.NOT_COMPLETE;
            }
        }
        setState(state);
    }

    @Override
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        map.put("compartment", compartment.getId().toString());
        map.put("slot", requestedSlot.toString());
        map.put("state", state.toString());
        addCollectionToMap(map, "persons", requestedPersons);
        addCollectionToMap(map, "resources", requestedResources);
    }
}
