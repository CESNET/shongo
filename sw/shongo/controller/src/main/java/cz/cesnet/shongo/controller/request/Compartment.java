package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.common.Person;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import cz.cesnet.shongo.fault.FaultException;

import javax.persistence.*;
import java.util.*;

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
     * @param id
     * @return requested resource with given {@code id}
     * @throws EntityNotFoundException when the requested resource doesn't exist
     */
    private ResourceSpecification getRequestedResourceById(Long id) throws EntityNotFoundException
    {
        for (ResourceSpecification resourceSpecification : requestedResources) {
            if (resourceSpecification.getId().equals(id)) {
                return resourceSpecification;
            }
        }
        throw new EntityNotFoundException(ResourceSpecification.class, id);
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
     * @param personId
     * @return requested person with given {@code id}
     * @throws EntityNotFoundException when the requested person doesn't exist
     */
    public Person getRequestedPersonById(Long personId) throws EntityNotFoundException
    {
        for (Person person : requestedPersons) {
            if (person.getId().equals(personId)) {
                return person;
            }
        }
        throw new EntityNotFoundException(Person.class, personId);
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

    /**
     * @param domain
     * @return compartment converted to API
     */
    public cz.cesnet.shongo.controller.api.Compartment toApi(Domain domain) throws FaultException
    {
        cz.cesnet.shongo.controller.api.Compartment compartment = new cz.cesnet.shongo.controller.api.Compartment();
        compartment.setId(getId().intValue());
        for (Person person : getRequestedPersons()) {
            compartment.addPerson(person.toApi());
        }
        for (ResourceSpecification resourceSpecification : getRequestedResources()) {
            compartment.addResource(resourceSpecification.toApi(domain));
        }
        return compartment;
    }

    /**
     * Synchronize compartment from API
     *
     * @param api
     * @param entityManager
     * @param domain
     * @throws FaultException
     */
    public <API extends cz.cesnet.shongo.controller.api.Compartment>
    void fromApi(API api, EntityManager entityManager, Domain domain) throws FaultException
    {
        // Create/modify requested persons
        for (cz.cesnet.shongo.controller.api.Person apiPerson : api.getPersons()) {
            Person person = null;
            if (api.isCollectionItemMarkedAsNew(API.PERSONS, apiPerson)) {
                person = new Person();
                addRequestedPerson(person);
            }
            else {
                person = getRequestedPersonById(apiPerson.getId().longValue());
            }
            person.fromApi(apiPerson);
        }
        // Delete requested persons
        Set<cz.cesnet.shongo.controller.api.Person> apiDeletedPersons =
                api.getCollectionItemsMarkedAsDeleted(API.PERSONS);
        for (cz.cesnet.shongo.controller.api.Person apiPerson : apiDeletedPersons) {
            removeRequestedPerson(getRequestedPersonById(apiPerson.getId().longValue()));
        }

        // Create/modify requested resources
        for (cz.cesnet.shongo.controller.api.ResourceSpecification apiResource : api.getResources()) {
            if (api.isCollectionItemMarkedAsNew(API.RESOURCES, apiResource)) {
                addRequestedResource(ResourceSpecification.fromAPI(apiResource, entityManager, domain));
            }
            else {
                ResourceSpecification resourceSpecification = getRequestedResourceById(apiResource.getId().longValue());
                resourceSpecification.fromApi(apiResource, entityManager, domain);
            }
        }
        // Delete requested resources
        Set<cz.cesnet.shongo.controller.api.ResourceSpecification> apiDeletedResources =
                api.getCollectionItemsMarkedAsDeleted(API.RESOURCES);
        for (cz.cesnet.shongo.controller.api.ResourceSpecification apiResource : apiDeletedResources) {
            removeRequestedResource(getRequestedResourceById(apiResource.getId().longValue()));
        }
    }
}
