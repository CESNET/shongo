package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.common.Identifier;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.controller.resource.topology.DeviceTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a component for a domain controller that holds all resources in memory in efficient form.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceDatabase extends Component
{
    private static Logger logger = LoggerFactory.getLogger(ResourceDatabase.class);

    /**
     * Domain to which all resources belongs.
     */
    private Domain domain;

    /**
     * Entity manager that is used for loading/saving resources.
     */
    private EntityManager entityManager;

    /**
     * @see cz.cesnet.shongo.controller.resource.ResourceManager
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
     * @param domain sets the {@link #domain}
     */
    public void setDomain(Domain domain)
    {
        this.domain = domain;
    }

    /**
     * @return {@link #deviceTopology}
     */
    public DeviceTopology getDeviceTopology()
    {
        return deviceTopology;
    }

    @Override
    public void init()
    {
        super.init();

        if (domain == null) {
            throw new IllegalStateException("Resource database doesn't have the domain set!");
        }
        entityManager = getEntityManager();
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

    @Override
    public void destroy()
    {
        logger.debug("Closing resource database...");

        resourceMap.clear();

        super.init();
    }

    /**
     * Add new resource to the resource database.
     *
     * @param resource
     */
    public void addResource(Resource resource)
    {
        checkInitialized();

        if (resource.getIdentifier() == null) {
            throw new IllegalArgumentException("Resource must have the identifier filled!");
        }
        if (resourceMap.containsKey(resource.getIdentifier())) {
            throw new IllegalArgumentException(
                    "Resource (" + resource.getIdentifier() + ") is already in the database!");
        }
        resourceManager.checkDomain(domain.getCodeName(), resource);

        // Save only resource that has not been saved yet
        if (resource.isPersisted() == false) {
            resourceManager.create(resource);
        }

        // Add resource to list of all resources
        resourceMap.put(resource.getIdentifier(), resource);

        // If resource is a device add it to the device topology
        if (resource instanceof DeviceResource) {
            deviceTopology.addDeviceResource((DeviceResource) resource);
        }
    }

    /**
     * Update resource in the resource database.
     *
     * @param resource
     */
    public void updateResource(Resource resource)
    {
        checkInitialized();

        if (resourceMap.containsKey(resource.getIdentifier()) == false) {
            throw new IllegalArgumentException(
                    "Resource (" + resource.getIdentifier() + ") is not in the database!");
        }

        if (true) {
            throw new RuntimeException("TODO: Implement ResourceDatabase.updateResource");
        }

        // Update it
        resourceManager.update(resource);

        // If resource is a device update it in the device topology
        if (resource instanceof DeviceResource) {
            deviceTopology.addDeviceResource((DeviceResource) resource);
        }
    }

    /**
     * Delete resource in the resource database
     *
     * @param resource
     */
    public void removeResource(Resource resource)
    {
        checkInitialized();

        if (resourceMap.containsKey(resource.getIdentifier()) == false) {
            throw new IllegalArgumentException(
                    "Resource (" + resource.getIdentifier() + ") is not in the database!");
        }

        // If resource is a device remove it from the device topology
        if (resource instanceof DeviceResource) {
            deviceTopology.addDeviceResource((DeviceResource) resource);
        }

        // Remove resource from the list of all resources
        resourceMap.remove(resource.getIdentifier());

        // Delete it
        resourceManager.delete(resource);
    }

    /**
     * @return list of all resource in the resource database.
     */
    public List<Resource> listResources()
    {
        checkInitialized();

        return new ArrayList<Resource>(resourceMap.values());
    }
}
