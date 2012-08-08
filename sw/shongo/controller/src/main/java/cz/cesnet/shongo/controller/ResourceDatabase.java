package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.controller.allocation.VirtualRoomDatabase;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.controller.resource.VirtualRoomsCapability;
import cz.cesnet.shongo.controller.resource.topology.DeviceTopology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
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
     * @see {@link DeviceTopology}
     */
    private DeviceTopology deviceTopology = new DeviceTopology();

    /**
     * @see {@link cz.cesnet.shongo.controller.allocation.VirtualRoomDatabase}
     */
    private VirtualRoomDatabase virtualRoomDatabase = new VirtualRoomDatabase();

    /**
     * @return {@link #deviceTopology}
     */
    public DeviceTopology getDeviceTopology()
    {
        return deviceTopology;
    }

    /**
     * @return {@link #virtualRoomDatabase}
     */
    public VirtualRoomDatabase getVirtualRoomDatabase()
    {
        return virtualRoomDatabase;
    }

    @Override
    public void init()
    {
        super.init();

        logger.debug("Loading resource database...");

        EntityManager entityManager = getEntityManager();

        // Load all resources from db
        ResourceManager resourceManager = new ResourceManager(entityManager);
        List<Resource> resourceList = resourceManager.list();
        for (Resource resource : resourceList) {
            addResource(resource, entityManager);
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
    public void addResource(Resource resource, EntityManager entityManager)
    {
        checkInitialized();

        // Create resource in the database if it wasn't created yet
        if (!resource.isPersisted()) {
            ResourceManager resourceManager = new ResourceManager(entityManager);
            resourceManager.create(resource);
        }

        if (resourceMap.containsKey(resource.getId())) {
            throw new IllegalArgumentException(
                    "Resource '" + resource.getId() + "' is already in the database!");
        }

        // Add resource to list of all resources
        resourceMap.put(resource.getId(), resource);

        // If resource is a device
        if (resource instanceof DeviceResource) {
            DeviceResource deviceResource = (DeviceResource) resource;

            // Add it to device toplogy
            deviceTopology.addDeviceResource(deviceResource);

            // And if also has virtual rooms, add it to virtual rooms manager
            if (deviceResource.hasCapability(VirtualRoomsCapability.class)) {
                virtualRoomDatabase.addDeviceResource(deviceResource, entityManager);
            }
        }
    }

    /**
     * Update resource in the resource database.
     *
     * @param resource
     */
    public void updateResource(Resource resource, EntityManager entityManager)
    {
        checkInitialized();

        if (resourceMap.containsKey(resource.getId()) == false) {
            throw new IllegalArgumentException(
                    "Resource '" + resource.getId() + "' is not in the database!");
        }

        // If resource is a device
        if (resource instanceof DeviceResource) {
            DeviceResource deviceResource = (DeviceResource) resource;

            // Update it in the device topology
            deviceTopology.updateDeviceResource((DeviceResource) resource);

            // And if also has virtual rooms, update it in virtual rooms manager
            if (deviceResource.hasCapability(VirtualRoomsCapability.class)) {
                virtualRoomDatabase.updateDeviceResource(deviceResource, entityManager);
            }
        }
    }

    /**
     * Delete resource in the resource database
     *
     * @param resource
     */
    public void removeResource(Resource resource, EntityManager entityManager)
    {
        checkInitialized();

        if (resourceMap.containsKey(resource.getId()) == false) {
            throw new IllegalArgumentException(
                    "Resource '" + resource.getId() + "' is not in the database!");
        }

        // If resource is a device
        if (resource instanceof DeviceResource) {
            DeviceResource deviceResource = (DeviceResource) resource;

            // Remove it from the device topology
            deviceTopology.removeDeviceResource((DeviceResource) resource);

            // And if also has virtual rooms, remove it from virtual rooms manager
            if (deviceResource.hasCapability(VirtualRoomsCapability.class)) {
                virtualRoomDatabase.removeDeviceResource(deviceResource, entityManager);
            }
        }

        // Remove resource from the list of all resources
        resourceMap.remove(resource.getId());
    }
}
