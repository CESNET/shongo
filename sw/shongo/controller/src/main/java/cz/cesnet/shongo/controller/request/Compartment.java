package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.common.PersistentObject;
import cz.cesnet.shongo.common.Person;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a group of requested resources and/or persons.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class Compartment extends PersistentObject
{
    /**
     * Reservation request for which is the compartment request created.
     */
    private ReservationRequest reservationRequest;

    /**
     * List of specification for resources which are requested to participate in compartment.
     */
    private List<ResourceSpecification> requestedResources = new ArrayList<ResourceSpecification>();

    /**
     * List of persons which are requested to participate in compartment.
     */
    private List<Person> requestedPersons = new ArrayList<Person>();

    /**
     * @return {@link #reservationRequest}
     */
    @ManyToOne
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
        // Manage bidirectional association
        if (reservationRequest != this.reservationRequest) {
            if (this.reservationRequest != null) {
                ReservationRequest oldReservationRequest = this.reservationRequest;
                this.reservationRequest = null;
                oldReservationRequest.removeRequestedCompartment(this);
            }
            if (reservationRequest != null) {
                this.reservationRequest = reservationRequest;
                this.reservationRequest.addRequestedCompartment(this);
            }
        }
    }

    /**
     * @return {@link #requestedResources}
     */
    @OneToMany(cascade = CascadeType.ALL)
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
     * @param requestedResource resource to be added to the {@link #requestedResources}
     * @param requestedPerson   person to be requested for the given resource
     */
    public void addRequestedResource(ResourceSpecification requestedResource, Person requestedPerson)
    {
        requestedResources.add(requestedResource);
        requestedResource.addRequestedPerson(requestedPerson);
    }

    /**
     * @param requestedResource resource to be removed from the {@link #requestedResources}
     */
    public void removeRequestedResource(ResourceSpecification requestedResource)
    {
        requestedResources.remove(requestedResource);
    }

    /**
     * @return {@link #requestedPersons}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<Person> getRequestedPersons()
    {
        return Collections.unmodifiableList(requestedPersons);
    }

    /**
     * @param requestedPerson person to be added to the {@link #requestedPersons}
     */
    public void addRequestedPerson(Person requestedPerson)
    {
        requestedPersons.add(requestedPerson);
    }

    /**
     * Request person to the compartment by requesting him for the given resource and add the given resource
     * to {@link #requestedResources}.
     *
     * @param requestedPerson       person to be requested to the given resource
     * @param resourceSpecification resource to be added to the {@link #requestedResources} (if not exists)
     */
    public void addRequestedPerson(Person requestedPerson, ResourceSpecification resourceSpecification)
    {
        if (!requestedResources.contains(resourceSpecification)) {
            addRequestedResource(resourceSpecification);
        }
        resourceSpecification.addRequestedPerson(requestedPerson);
    }

    /**
     * @param requestedPerson person to be removed from the {@link #requestedPersons}
     */
    public void removeRequestedPerson(Person requestedPerson)
    {
        requestedPersons.remove(requestedPerson);
    }

    @Override
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        addCollectionToMap(map, "persons", requestedPersons);
        addCollectionToMap(map, "resources", requestedResources);
    }
}
