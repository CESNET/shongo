package cz.cesnet.shongo.controller.request;

import cz.cesnet.shongo.PersistentObject;

import javax.persistence.Entity;

/**
 * Represents an abstract specification of any target for a {@link ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public abstract class Specification extends PersistentObject
{
    /**
     * State of {@link Specification}.
     */
    public static enum State
    {
        /**
         * {@link Specification} is ready for scheduling.
         */
        READY,

        /**
         * {@link Specification} is not ready for scheduling.
         */
        NOT_READY,

        /**
         * {@link Specification} should be skipped from scheduling.
         */
        SKIP
    }

    /**
     * @return current {@link State} of the {@link Specification}
     */
    public State getCurrentState()
    {
        return State.READY;
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
