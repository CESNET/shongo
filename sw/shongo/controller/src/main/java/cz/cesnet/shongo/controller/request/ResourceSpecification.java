package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.api.Fault;
import cz.cesnet.shongo.api.FaultException;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.common.Person;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents a requested resource to a compartment.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class ResourceSpecification extends PersistentObject
{
    /**
     * Persons that are requested to use the device to connect into compartment.
     */
    private List<Person> requestedPersons = new ArrayList<Person>();

    /**
     * Defines who should initiate the call to this device.
     */
    private CallInitiation callInitiation;

    /**
     * @return {@link #requestedPersons}
     */
    @OneToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<Person> getRequestedPersons()
    {
        return requestedPersons;
    }

    /**
     * @param id
     * @return requested person with given {@code id}
     * @throws FaultException
     */
    public Person getRequestedPersonById(Long id) throws FaultException
    {
        for (Person person : requestedPersons) {
            if (person.getId().equals(id)) {
                return person;
            }
        }
        throw new FaultException(Fault.Common.RECORD_NOT_EXIST, Person.class, id);
    }

    /**
     * @param person person to be added to the {@link #requestedPersons}
     */
    public void addRequestedPerson(Person person)
    {
        requestedPersons.add(person);
    }

    /**
     * @param person person to be removed from the {@link #requestedPersons}
     */
    public void removeRequestedPerson(Person person)
    {
        requestedPersons.remove(person);
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
    protected void fillDescriptionMap(Map<String, String> map)
    {
        super.fillDescriptionMap(map);

        addCollectionToMap(map, "requestedPersons", requestedPersons);
        if (callInitiation != null && callInitiation != CallInitiation.DEFAULT) {
            map.put("callInitiation", callInitiation.toString());
        }
    }

    /**
     * @param domain
     * @return converted resource specification to API
     * @throws FaultException
     */
    public abstract cz.cesnet.shongo.controller.api.ResourceSpecification toApi(Domain domain) throws FaultException;

    /**
     * @param api API resource specification to be filled
     */
    protected final void toApi(cz.cesnet.shongo.controller.api.ResourceSpecification api)
    {
        api.setId(getId().intValue());
        for (Person person : requestedPersons) {
            api.addPerson(person.toApi());
        }
    }

    /**
     * Synchronize resource specification from API
     *
     *
     * @param api
     * @param entityManager
     * @param domain
     * @throws FaultException
     */
    public void fromApi(cz.cesnet.shongo.controller.api.ResourceSpecification api, EntityManager entityManager,
            Domain domain) throws FaultException
    {
        // Create/modify requested persons
        for (cz.cesnet.shongo.controller.api.Person apiPerson : api.getPersons()) {
            Person person;
            if (api.isCollectionItemMarkedAsNew(api.PERSONS, apiPerson)) {
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
                api.getCollectionItemsMarkedAsDeleted(api.PERSONS);
        for (cz.cesnet.shongo.controller.api.Person apiPerson : apiDeletedPersons) {
            removeRequestedPerson(getRequestedPersonById(apiPerson.getId().longValue()));
        }
    }

    /**
     * @param api
     * @param entityManager
     * @param domain
     * @return new instance of {@link ResourceSpecification} from API
     * @throws FaultException
     */
    public static ResourceSpecification fromAPI(cz.cesnet.shongo.controller.api.ResourceSpecification api,
            EntityManager entityManager, Domain domain) throws FaultException
    {
        ResourceSpecification resourceSpecification;
        if (api instanceof cz.cesnet.shongo.controller.api.ExistingResourceSpecification) {
            resourceSpecification = new ExistingResourceSpecification();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.LookupResourceSpecification) {
            resourceSpecification = new LookupResourceSpecification();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.ExternalEndpointSpecification) {
            resourceSpecification = new ExternalEndpointSpecification();
        }
        else {
            throw new FaultException(Fault.Common.TODO_IMPLEMENT);
        }
        resourceSpecification.fromApi(api, entityManager, domain);
        return resourceSpecification;
    }
}
