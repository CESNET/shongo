package cz.cesnet.shongo.controller.reservation;

import cz.cesnet.shongo.common.AbstractManager;
import cz.cesnet.shongo.common.Person;

import javax.persistence.EntityManager;

/**
 * Manager for {@link PersonRequest}.
 *
 * @see AbstractManager
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PersonRequestManager extends AbstractManager
{
    /**
     * Constructor.
     *
     * @param entityManager
     */
    public PersonRequestManager(EntityManager entityManager)
    {
        super(entityManager);
    }

    /**
     * Create a new person request.
     *
     * @param person                Person that is requested
     * @param resourceSpecification Specification of resource that the person use
     * @return created person request
     */
    public PersonRequest create(Person person, ResourceSpecification resourceSpecification)
    {
        PersonRequest personRequest = new PersonRequest();
        personRequest.setPerson(person);
        personRequest.setState(PersonRequest.State.NOT_ASKED);
        personRequest.setResourceSpecification(resourceSpecification);

        super.create(personRequest);

        return personRequest;
    }
}
