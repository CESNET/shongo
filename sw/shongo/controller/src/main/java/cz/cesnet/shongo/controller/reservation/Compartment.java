package cz.cesnet.shongo.controller.reservation;

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
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "compartment")
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
        // Manage bidirectional association
        if (requestedResources.contains(requestedResource) == false) {
            requestedResources.add(requestedResource);
            requestedResource.setCompartment(this);
        }
    }

    /**
     * @param requestedResource resource to be removed from the {@link #requestedResources}
     */
    public void removeRequestedResource(ResourceSpecification requestedResource)
    {
        // Manage bidirectional association
        if (requestedResources.contains(requestedResource)) {
            requestedResources.remove(requestedResource);
            requestedResource.setCompartment(null);
        }

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
