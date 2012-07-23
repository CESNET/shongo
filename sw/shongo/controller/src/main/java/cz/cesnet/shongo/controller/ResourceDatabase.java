package cz.cesnet.shongo.controller;

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
     * List of all resources in resource database by theirs id.
     */
    private Map<Long, Resource> resourceMap = new HashMap<Long, Resource>();

    /**
     * Topology of device resources.
     */
    private DeviceTopology deviceTopology = new DeviceTopology();

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

        logger.debug("Loading resource database...");

        // Load all resources from db
        ResourceManager resourceManager = new ResourceManager(getEntityManager());
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
            //throw new RuntimeException("TODO: Implement ResourceDatabase.updateResource");
        }

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
