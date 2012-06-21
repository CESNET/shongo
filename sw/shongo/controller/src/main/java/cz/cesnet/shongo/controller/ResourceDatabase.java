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
    private Map<Long, Resource> resourceMap = new HashMap<Long, Resource>();

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

        if (resourceMap.containsKey(resource.getId())) {
            throw new IllegalArgumentException(
                    "Resource '" + resource.getId() + "' is already in the database!");
        }

        // Save only resource that has not been saved yet
        if (resource.isPersisted() == false) {
            resourceManager.create(resource);
        }

        // Add resource to list of all resources
        resourceMap.put(resource.getId(), resource);

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

        if (resourceMap.containsKey(resource.getId()) == false) {
            throw new IllegalArgumentException(
                    "Resource '" + resource.getId() + "' is not in the database!");
        }

        if (true) {
            throw new RuntimeException("TODO: Implement ResourceDatabase.updateResource");
        }

        // Update it
        resourceManager.update(resource);

        // If resource is a device update it in the device topology
        if (resource instanceof DeviceResource) {
            deviceTopology.updateDeviceResource((DeviceResource) resource);
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

        if (resourceMap.containsKey(resource.getId()) == false) {
            throw new IllegalArgumentException(
                    "Resource '" + resource.getId() + "' is not in the database!");
        }

        // If resource is a device remove it from the device topology
        if (resource instanceof DeviceResource) {
            deviceTopology.removeDeviceResource((DeviceResource) resource);
        }

        // Remove resource from the list of all resources
        resourceMap.remove(resource.getId());

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
