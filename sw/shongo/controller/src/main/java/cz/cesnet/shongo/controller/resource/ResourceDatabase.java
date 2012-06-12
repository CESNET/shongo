package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.common.Identifier;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.reservation.ReservationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static Logger logger = LoggerFactory.getLogger(ResourceDatabase.class);

    /**
     * Domain for which the reservation database is used.
     */
    private Domain domain;

    /**
     * Entity manager that is used for loading/saving resources.
     */
    private EntityManager entityManager;

    /**
     * List of all resources in resource database by theirs id.
     */
    private Map<Identifier, Resource> resourceMap = new HashMap<Identifier, Resource>();

    /**
     * Constructor of reservation database.
     */
    public ResourceDatabase()
    {
    }

    /**
     * Constructor of resource database.
     *
     * @param domain        sets the {@link #domain}
     * @param entityManager sets the {@link #entityManager}
     */
    public ResourceDatabase(Domain domain, EntityManager entityManager)
    {
        setDomain(domain);
        setEntityManager(entityManager);
        init();
    }

    /**
     * @param domain sets the {@link #domain}
     */
    public void setDomain(Domain domain)
    {
        this.domain = domain;
    }

    /**
     * @param entityManager sets the {@link #entityManager}
     */
    public void setEntityManager(EntityManager entityManager)
    {
        this.entityManager = entityManager;
    }

    /**
     * Initialize reservation database.
     */
    public void init()
    {
        if (domain == null) {
            throw new IllegalStateException("Resource database doesn't have the domain set!");
        }
        if (entityManager == null) {
            throw new IllegalStateException("Resource database doesn't have the entity manager set!");
        }

        logger.debug("Loading resource database...");

        // Load all resources from db
        List<Resource> resourceList = entityManager
                .createQuery("SELECT resource FROM Resource resource", Resource.class).getResultList();
        for (Resource resource : resourceList) {
            if(resource.getIdentifier().getDomain().equals(domain.getCodeName()) == false) {
                throw new IllegalStateException("Resource has wrong domain in identifier '" +
                        resource.getIdentifier().getDomain() + "' (should be '" + domain.getCodeName() + "')!");
            }
            addResource(resource);
        }
    }

    /**
     * Destroy resource database.
     */
    public void destroy()
    {
        logger.debug("Closing resource database...");
        resourceMap.clear();
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
                    "Resource (" + resource.getIdentifier() + ") is already in the database!");
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
                    "Resource (" + resource.getIdentifier() + ") is not in the database!");
        }

        throw new RuntimeException("TODO: Implement ResourceDatabase.updateResource");
    }

    /**
     * Delete resource in the resource database
     *
     * @param resource
     */
    public void removeResource(Resource resource)
    {
        if (resourceMap.containsKey(resource.getIdentifier()) == false) {
            throw new IllegalArgumentException(
                    "Resource (" + resource.getIdentifier() + ") is not in the database!");
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
