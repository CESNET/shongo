package cz.cesnet.shongo.controller.allocation;

import cz.cesnet.shongo.common.AbstractManager;
import cz.cesnet.shongo.controller.request.CompartmentRequest;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

/**
 * Manager for {@link AllocatedCompartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AllocatedCompartmentManager extends AbstractManager
{
    /**
     * @param entityManager sets the {@link #entityManager}
     */
    public AllocatedCompartmentManager(EntityManager entityManager)
    {
        super(entityManager);
    }

    /**
     * @param allocatedCompartment to be created in the database
     */
    public void create(AllocatedCompartment allocatedCompartment)
    {
        super.create(allocatedCompartment);
    }

    /**
     * @param allocatedCompartment to be updated in the database
     */
    public void update(AllocatedCompartment allocatedCompartment)
    {
        super.update(allocatedCompartment);
    }

    /**
     * @param compartmentRequest
     * @return {@link AllocatedCompartment} for the given {@link CompartmentRequest} or null if doesn't exists
     */
    public AllocatedCompartment getByCompartmentRequest(CompartmentRequest compartmentRequest)
    {
        try {
            AllocatedCompartment allocatedCompartment = entityManager.createQuery(
                    "SELECT alloc FROM AllocatedCompartment alloc WHERE alloc.compartmentRequest.id = :id",
                    AllocatedCompartment.class).setParameter("id", compartmentRequest.getId())
                    .getSingleResult();
            return allocatedCompartment;
        }
        catch (NoResultException exception) {
            return null;
        }
    }
}
