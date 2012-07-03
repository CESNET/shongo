package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.AbstractManager;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.List;

/**
 * Manager for {@link Resource}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see AbstractManager
 */
public class ResourceManager extends AbstractManager
{
    /**
     * Constructor.
     *
     * @param entityManager
     */
    private ResourceManager(EntityManager entityManager)
    {
        super(entityManager);
    }

    /**
     * @param entityManager
     * @return new instance of {@link cz.cesnet.shongo.controller.resource.ResourceManager}
     */
    public static ResourceManager createInstance(EntityManager entityManager)
    {
        return new ResourceManager(entityManager);
    }

    /**
     * Create a new resource in the database.
     *
     * @param resource
     */
    public void create(Resource resource)
    {
        super.create(resource);
    }

    /**
     * Update existing resource in the database.
     *
     * @param resource
     */
    public void update(Resource resource)
    {
        super.update(resource);
    }

    /**
     * Delete existing resource in the database
     *
     * @param resource
     */
    public void delete(Resource resource)
    {
        super.delete(resource);
    }

    /**
     * @return list of all resources in the database.
     */
    public List<Resource> list()
    {
        List<Resource> resourceList = entityManager
                .createQuery("SELECT resource FROM Resource resource", Resource.class)
                .getResultList();
        return resourceList;
    }

    /**
     * @param resourceId
     * @return {@link Resource} with given {@code resourceId} or null if the resource not exists
     */
    public Resource get(Long resourceId)
    {
        try {
            Resource resource = entityManager.createQuery(
                    "SELECT resource FROM Resource resource WHERE resource.id = :id",
                    Resource.class).setParameter("id", resourceId)
                    .getSingleResult();
            return resource;
        }
        catch (NoResultException exception) {
            return null;
        }
    }
}
