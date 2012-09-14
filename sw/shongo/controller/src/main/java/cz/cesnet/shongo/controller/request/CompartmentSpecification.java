package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.Scheduler;
import cz.cesnet.shongo.controller.scheduler.CompartmentReservationTask;
import cz.cesnet.shongo.controller.scheduler.ReservationTask;
import cz.cesnet.shongo.fault.EntityNotFoundException;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.TodoImplementException;
import org.apache.commons.lang.ObjectUtils;

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
public class CompartmentSpecification extends Specification implements StatefulSpecification, CompositeSpecification
{
    /**
     * List of specifications for targets which are requested to participate in compartment.
     */
    private List<Specification> specifications = new ArrayList<Specification>();

    /**
     * Specifies the default option who should initiate the call ({@code null} means
     * that {@link Scheduler} can decide it).
     */
    private CallInitiation callInitiation;

    /**
     * Constructor.
     */
    public CompartmentSpecification()
    {
    }

    /**
     * Constructor.
     *
     * @param callInitiation sets the {@link #callInitiation}
     */
    public CompartmentSpecification(CallInitiation callInitiation)
    {
        this.callInitiation = callInitiation;
    }

    /**
     * @return {@link #specifications}
     */
    @ManyToMany(cascade = CascadeType.ALL)
    @Access(AccessType.FIELD)
    public List<Specification> getSpecifications()
    {
        return Collections.unmodifiableList(specifications);
    }

    /**
     * @param id of the requested {@link Specification}
     * @return {@link Specification} with given {@code id}
     * @throws EntityNotFoundException when the {@link Specification} doesn't exist
     */
    private Specification getSpecificationById(Long id) throws EntityNotFoundException
    {
        for (Specification specification : specifications) {
            if (specification.getId().equals(id)) {
                return specification;
            }
        }
        throw new EntityNotFoundException(Specification.class, id);
    }

    /**
     * @param specification to be searched in the {@link CompartmentSpecification}
     * @return true if the {@link CompartmentSpecification} contains given {@code specification},
     *         false otherwise
     */
    public boolean containsSpecification(Specification specification)
    {
        Long specificationId = specification.getId();
        for (Specification possibleSpecification : specifications) {
            if (possibleSpecification.getId().equals(specificationId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param specification to be added to the {@link #specifications}
     */
    public void addSpecification(Specification specification)
    {
        specifications.add(specification);
    }

    /**
     * @param specification to be removed from the {@link #specifications}
     */
    public void removeSpecification(Specification specification)
    {
        specifications.remove(specification);
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
    @Transient
    public State getCurrentState()
    {
        State state = State.READY;
        for (Specification specification : specifications) {
            if (specification instanceof StatefulSpecification) {
                StatefulSpecification statefulSpecification = (StatefulSpecification) specification;
                if (statefulSpecification.getCurrentState().equals(State.NOT_READY)) {
                    state = State.NOT_READY;
                    break;
                }
            }
        }
        return state;
    }

    @Override
    public CompartmentSpecification clone(Map<Specification, Specification> originalSpecifications)
    {
        CompartmentSpecification compartmentSpecification = new CompartmentSpecification();
        compartmentSpecification.setCallInitiation(getCallInitiation());
        for (Specification specification : specifications) {
            if (specification instanceof StatefulSpecification) {
                StatefulSpecification statefulSpecification = (StatefulSpecification) specification;
                compartmentSpecification.addSpecification(statefulSpecification.clone(originalSpecifications));
            }
            else {
                compartmentSpecification.addSpecification(specification);
            }
        }

        originalSpecifications.put(compartmentSpecification, this);

        return compartmentSpecification;
    }

    @Override
    public boolean synchronizeFrom(Specification specification)
    {
        CompartmentSpecification compartmentSpecification = (CompartmentSpecification) specification;

        boolean modified = false;
        modified |= !ObjectUtils.equals(getCallInitiation(), compartmentSpecification.getCallInitiation());

        setCallInitiation(compartmentSpecification.getCallInitiation());

        return modified;
    }

    @Override
    public CompartmentReservationTask createReservationTask(ReservationTask.Context context)
    {
        return new CompartmentReservationTask(this, context);
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
    protected void fillDescriptionMap(Map<String, Object> map)
    {
        super.fillDescriptionMap(map);

        map.put("callInitiation", callInitiation);
        map.put("specifications", specifications);
    }
}
