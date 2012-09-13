package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a group of specifications. Every endpoint which will be allocated from the specifications should be
 * interconnected to each other.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class CompartmentSpecification extends Specification
{
    /**
     * List of specifications for targets which are requested to participate in compartment.
     */
    private List<Specification> requestedSpecifications = new ArrayList<Specification>();

    /**
     * Specifies the default option who should initiate the call ({@code null} means
     * that {@link Scheduler} can decide it).
     */
    private CallInitiation callInitiation;

    /**
     * @return {@link #requestedSpecifications}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<Specification> getRequestedSpecifications()
    {
        return Collections.unmodifiableList(requestedSpecifications);
    }

    /**
     * @param id of the requested {@link Specification}
     * @return {@link Specification} with given {@code id}
     * @throws EntityNotFoundException when the {@link Specification} doesn't exist
     */
    private Specification getRequestedSpecificationById(Long id) throws EntityNotFoundException
    {
        for (Specification requestedSpecification : requestedSpecifications) {
            if (requestedSpecification.getId().equals(id)) {
                return requestedSpecification;
            }
        }
        throw new EntityNotFoundException(Specification.class, id);
    }

    /**
     * @param requestedSpecification to be added to the {@link #requestedSpecifications}
     */
    public void addRequestedSpecification(Specification requestedSpecification)
    {
        requestedSpecifications.add(requestedSpecification);
    }

    /**
     * @param requestedSpecification to be removed from the {@link #requestedSpecifications}
     */
    public void removeRequestedSpecification(Specification requestedSpecification)
    {
        requestedSpecifications.remove(requestedSpecification);
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

    @Override
    public State getCurrentState()
    {
        State state = State.READY;
        for (Specification requestedSpecification : requestedSpecifications) {
            if (requestedSpecification.getCurrentState().equals(State.NOT_READY)) {
                state = State.NOT_READY;
                break;
            }
        }
        return state;
    }

    /**
     * @param domain
     * @return compartment converted to API
     */
    public cz.cesnet.shongo.controller.api.Compartment toApi(Domain domain) throws FaultException
    {
        throw new TodoImplementException();
        /*cz.cesnet.shongo.controller.api.Compartment compartment = new cz.cesnet.shongo.controller.api.Compartment();
        compartment.setId(getId().intValue());
        for (Person person : getRequestedPersons()) {
            compartment.addPerson(person.toApi());
        }
        for (ResourceSpecification resourceSpecification : getRequestedResources()) {
            compartment.addResource(resourceSpecification.toApi(domain));
        }
        return compartment;*/
    }

    /**
     * Synchronize compartment from API
     *
     * @param api
     * @param entityManager
     * @param domain
     * @throws cz.cesnet.shongo.fault.FaultException
     *
     */
    public <API extends cz.cesnet.shongo.controller.api.Compartment>
    void fromApi(API api, EntityManager entityManager, Domain domain) throws FaultException
    {
        throw new TodoImplementException();
        /*// Create/modify requested persons
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
        }*/
    }

    @Override
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        map.put("callInitiation", callInitiation.toString());
        addCollectionToMap(map, "specifications", requestedSpecifications);
    }
}
