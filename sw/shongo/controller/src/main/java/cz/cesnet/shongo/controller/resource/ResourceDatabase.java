package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.common.Identifier;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A resource database for domain controller.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceDatabase
{
    /**
     * Entity manager that is used for loading/saving resources.
     */
    private EntityManager entityManager;

    /**
     * List of all resources in resource database by theirs id.
     */
    private Map<Identifier, Resource> resourceMap = new HashMap<Identifier, Resource>();

    /**
     * Constructor of resource database.
     *
     * @param entityManager Sets the {@link #entityManager}
     */
    public ResourceDatabase(EntityManager entityManager)
    {
        this.entityManager = entityManager;

        // Load all resources from db
        List<Resource> resourceList = entityManager
                .createQuery("SELECT resource FROM Resource resource", Resource.class).getResultList();
        for (Resource resource : resourceList) {
            addResource(resource);
        }
    }

    /**
     * Add new resource to the resource database.
     *
     * @param resource
     */
    public void addResource(Resource resource)
    {
        if (resource.getIdentifier() == null) {
            throw new IllegalArgumentException("Resource must have the identifier filled!");
        }
        if (resourceMap.containsKey(resource.getIdentifier())) {
            throw new IllegalArgumentException(
                    "Resource (" + resource.getIdentifier() + ") is already in resource database!");
        }

        // Save resource to database
        entityManager.getTransaction().begin();
        entityManager.persist(resource);
        entityManager.getTransaction().commit();

        // Add resource to list of all resources
        resourceMap.put(resource.getIdentifier(), resource);
    }

    /**
     * Update resource in the resource database.
     *
     * @param resource
     */
    public void updateResource(Resource resource)
    {
        if (resourceMap.containsKey(resource.getIdentifier()) == false) {
            throw new IllegalArgumentException(
                    "Resource (" + resource.getIdentifier() + ") is not in resource database!");
        }

        throw new RuntimeException("TODO: Implement ResourceDatabase.updateResource");
    }

    /**
     * Delete resource in the resource database
     *
     * @param resource
     */
    public void deleteResource(Resource resource)
    {
        if (resourceMap.containsKey(resource.getIdentifier()) == false) {
            throw new IllegalArgumentException(
                    "Resource (" + resource.getIdentifier() + ") is not in resource database!");
        }

        // Delete resource from database
        entityManager.getTransaction().begin();
        entityManager.remove(resource);
        entityManager.getTransaction().commit();

        // Add resource to list of all resources
        resourceMap.remove(resource.getIdentifier());
    }

    /**
     * @return list of all resource in the resource database.
     */
    public List<Resource> listResources()
    {
        return new ArrayList<Resource>(resourceMap.values());
    }
}
