package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.api.util.ClassHelper;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import java.util.Map;

/**
 * Represents an abstract specification of any target for a {@link ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Specification extends PersistentObject
{
    /**
     * Synchronize properties from given {@code specification}.
     *
     * @param specification from which will be copied all properties values to
     *                      this {@link Specification}
     * @return true if some modification was made
     */
    public boolean synchronizeFrom(Specification specification)
    {
        return false;
    }

    /**
     * @param originalSpecifications map of original {@link Specification} instances by the cloned instances which should
     *                               be populated by this cloning
     * @return cloned instance of {@link StatefulSpecification}. If the {@link StatefulSpecification} contains some
     *         child {@link StatefulSpecification} they should be recursively cloned too.
     */
    public Specification clone(Map<Specification, Specification> originalSpecifications)
    {
        Specification newSpecification = ClassHelper.createInstanceFromClassRuntime(getClass());
        newSpecification.synchronizeFrom(this);
        if (this instanceof CompositeSpecification) {
            CompositeSpecification compositeSpecification = (CompositeSpecification) this;
            CompositeSpecification newCompositeSpecification = (CompositeSpecification) newSpecification;
            for (Specification childSpecification : compositeSpecification.getSpecifications()) {
                newCompositeSpecification.addSpecification(childSpecification.clone(originalSpecifications));
            }
        }

        originalSpecifications.put(newSpecification, this);

        return newSpecification;
    }

    /*public abstract cz.cesnet.shongo.controller.api.ResourceSpecification toApi(Domain domain) throws FaultException;

    protected final void toApi(cz.cesnet.shongo.controller.api.ResourceSpecification api)
    {
        api.setId(getId().intValue());
        for (Person person : requestedPersons) {
            api.addPerson(person.toApi());
        }
    }

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

    public static ResourceSpecification fromAPI(cz.cesnet.shongo.controller.api.ResourceSpecification api,
            EntityManager entityManager, Domain domain) throws FaultException
    {
        ResourceSpecification resourceSpecification;
        if (api instanceof cz.cesnet.shongo.controller.api.ExistingResourceSpecification) {
            resourceSpecification = new ExistingEndpointSpecification();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.LookupResourceSpecification) {
            resourceSpecification = new LookupEndpointSpecification();
        }
        else if (api instanceof cz.cesnet.shongo.controller.api.ExternalEndpointSpecification) {
            resourceSpecification = new ExternalEndpointSpecification();
        }
        else {
            throw new TodoImplementException();
        }
        resourceSpecification.fromApi(api, entityManager, domain);
        return resourceSpecification;
    }*/
}
