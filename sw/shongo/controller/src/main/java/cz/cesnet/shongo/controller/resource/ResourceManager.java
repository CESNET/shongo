package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.common.AbstractManager;
import cz.cesnet.shongo.common.Identifier;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.util.List;

/**
 * Manager for {@link Resource}.
 *
 * @see AbstractManager
 * @author Martin Srom <martin.srom@cesnet.cz>
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
     * @param identifier
     * @return {@link Resource} with given identifier or null if the resource not exists
     */
    public Resource get(Identifier identifier)
    {
        try {
            Resource resource = entityManager.createQuery(
                    "SELECT resource FROM Resource resource WHERE resource.identifierAsString = :identifier",
                    Resource.class).setParameter("identifier", identifier.toString())
                    .getSingleResult();
            return resource;
        }
        catch (NoResultException exception) {
            return null;
        }
    }

    /**
     * Check domain in all existing resources identifiers
     *
     * @param domain
     * @throws IllegalStateException
     */
    public void checkDomain(String domain) throws IllegalStateException
    {
        List<Resource> resourceList = entityManager
                .createQuery("SELECT resource FROM Resource resource", Resource.class)
                .getResultList();
        for (Resource resource : resourceList) {
            checkDomain(domain, resource);
        }
    }

    /**
     * Check domain in given resource identifier.
     *
     * @param domain
     * @param resource
     * @throws IllegalStateException
     */
    public void checkDomain(String domain, Resource resource) throws IllegalStateException
    {
        if (resource.getIdentifier().getDomain().startsWith(domain) == false) {
            throw new IllegalStateException("Resource has wrong domain in identifier '" +
                    resource.getIdentifier().getDomain() + "' (should start with '" + domain + "')!");
        }
    }
}
