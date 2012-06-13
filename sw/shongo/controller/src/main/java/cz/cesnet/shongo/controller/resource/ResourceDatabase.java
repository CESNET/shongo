package cz.cesnet.shongo.controller.resource;

import cz.cesnet.shongo.common.Identifier;
import cz.cesnet.shongo.controller.Domain;
import cz.cesnet.shongo.controller.reservation.ReservationRequest;
import cz.cesnet.shongo.controller.resource.topology.DeviceTopology;
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
     * @see ResourceManager
     */
    private ResourceManager resourceManager;

    /**
     * List of all resources in resource database by theirs id.
     */
    private Map<Identifier, Resource> resourceMap = new HashMap<Identifier, Resource>();

    /**
     * Topology of device resources.
     */
    private DeviceTopology deviceTopology = new DeviceTopology();

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
     * @return {@link #deviceTopology}
     */
    public DeviceTopology getDeviceTopology()
    {
        return deviceTopology;
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
        resourceManager = ResourceManager.createInstance(entityManager);

        logger.debug("Checking resource database...");

        resourceManager.checkDomain(domain.getCodeName());

        logger.debug("Loading resource database...");

        // Load all resources from db
        List<Resource> resourceList = resourceManager.list();
        for (Resource resource : resourceList) {
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
        resourceManager.checkDomain(domain.getCodeName(), resource);

        entityManager.getTransaction().begin();
        resourceManager.create(resource);
        entityManager.getTransaction().commit();

        // Add resource to list of all resources
        resourceMap.put(resource.getIdentifier(), resource);

        // If resource is device add it to the device topology
        if ( resource instanceof DeviceResource ) {
            deviceTopology.addDeviceResource((DeviceResource)resource);
        }
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

        if ( true ) {
            throw new RuntimeException("TODO: Implement ResourceDatabase.updateResource");
        }

        entityManager.getTransaction().begin();
        resourceManager.update(resource);
        entityManager.getTransaction().commit();

        // If resource is device update it in the device topology
        if ( resource instanceof DeviceResource ) {
            deviceTopology.addDeviceResource((DeviceResource)resource);
        }
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

        // If resource is device remove it from the device topology
        if ( resource instanceof DeviceResource ) {
            deviceTopology.addDeviceResource((DeviceResource)resource);
        }

        // Add resource to list of all resources
        resourceMap.remove(resource.getIdentifier());

        entityManager.getTransaction().begin();
        resourceManager.delete(resource);
        entityManager.getTransaction().commit();
    }

    /**
     * @return list of all resource in the resource database.
     */
    public List<Resource> listResources()
    {
        return new ArrayList<Resource>(resourceMap.values());
    }
}
