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
     * @return {@link #requestedResources}
     */
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "compartment")
    public List<ResourceSpecification> getRequestedResources()
    {
        return Collections.unmodifiableList(requestedResources);
    }

    /**
     * @param requestedResources sets the {@link #requestedResources}
     */
    private void setRequestedResources(List<ResourceSpecification> requestedResources)
    {
        this.requestedResources = requestedResources;
    }

    /**
     * @param requestedResource resource to be added to the list of requested resources
     */
    public void addRequestedResource(ResourceSpecification requestedResource)
    {
        this.requestedResources.add(requestedResource);
    }

    /**
     * @return {@link #requestedPersons}
     */
    @OneToMany(cascade = CascadeType.ALL)
    public List<Person> getRequestedPersons()
    {
        return Collections.unmodifiableList(requestedPersons);
    }

    /**
     * @param requestedPersons sets the {@link #requestedPersons}
     */
    private void setRequestedPersons(List<Person> requestedPersons)
    {
        this.requestedPersons = requestedPersons;
    }

    /**
     * @param requestedPerson person to be added to the list of requested persons
     */
    public void addRequestedPerson(Person requestedPerson)
    {
        this.requestedPersons.add(requestedPerson);
    }

    @Override
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        addCollectionToMap(map, "persons", requestedPersons);
        addCollectionToMap(map, "resources", requestedResources);
    }
}
