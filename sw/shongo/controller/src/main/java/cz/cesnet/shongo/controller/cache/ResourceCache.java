package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.allocation.AllocatedResource;
import cz.cesnet.shongo.controller.allocation.AllocatedVirtualRoom;
import cz.cesnet.shongo.controller.resource.*;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * Represents a cache for all resources in efficient form. It also holds
 * allocation information about resources which are used , e.g., by scheduler.
 * <p/>
 * Resources must be explicitly added by {@link #addResource(Resource, EntityManager)} or automatically loaded in
 * {@link #loadResources(EntityManager)}.
 * <p/>
 * Database holds only allocations inside the {@link #workingInterval} If the {@link #workingInterval} isn't set
 * the database of allocations will be empty (it is not desirable to load all allocations for the entire time span).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceCache extends AbstractCache
{
    private static Logger logger = LoggerFactory.getLogger(ResourceCache.class);

    /**
     * List of all resources in resource database by theirs id.
     */
    private Map<Long, Resource> resourceById = new HashMap<Long, Resource>();

    /**
     * Map of device resource states by theirs identifiers.
     */
    private Map<Long, ResourceState> resourceStateById = new HashMap<Long, ResourceState>();

    /**
     * Map of capability states by theirs types.
     */
    private Map<Class<? extends Capability>, CapabilityState> capabilityStateByType =
            new HashMap<Class<? extends Capability>, CapabilityState>();

    /**
     * @see {@link cz.cesnet.shongo.controller.cache.DeviceTopology}
     */
    private DeviceTopology deviceTopology = new DeviceTopology();

    /**
     * Constructor.
     */
    public ResourceCache()
    {
    }

    /**
     * @return {@link #deviceTopology}
     */
    public DeviceTopology getDeviceTopology()
    {
        return deviceTopology;
    }

    public void loadResources(EntityManager entityManager)
    {
        logger.debug("Loading resources...");

        // Load all resources from db
        ResourceManager resourceManager = new ResourceManager(entityManager);
        List<Resource> resourceList = resourceManager.list();

        for (Resource resource : resourceList) {
            addResource(resource, entityManager);
        }
    }

    @Override
    protected void workingIntervalChanged(EntityManager entityManager)
    {
        // Remove all allocated virtual rooms from all resources and add it again for the new interval
        for (ResourceState resourceState : resourceStateById.values()) {
            updateResourceState(resourceState, entityManager);
        }
    }

    /**
     * @param resourceId
     * @return resource with given {@code resourceId}
     */
    public Resource getResource(Long resourceId)
    {
        return resourceById.get(resourceId);
    }

    /**
     * Add new resource to the resource database. If the resource is not persisted yet it is created in the database.
     *
     * @param resource      resource to be added to the resource database
     * @param entityManager entity manager used for storing not persisted resource to the database and
     *                      for loading resource allocations
     */
    public void addResource(Resource resource, EntityManager entityManager)
    {
        resource.checkPersisted();
        Long resourceId = resource.getId();
        if (resourceById.containsKey(resourceId)) {
            throw new IllegalArgumentException("Resource '" + resourceId + "' is already in the database!");
        }

        // Add resource to list of all resources
        resourceById.put(resource.getId(), resource);

        // Get resource state
        ResourceState resourceState = resourceStateById.get(resourceId);
        if (resourceState == null) {
            resourceState = new ResourceState(resourceId);
            resourceStateById.put(resourceId, resourceState);
        }

        // If resource is a device
        if (resource instanceof DeviceResource) {
            DeviceResource deviceResource = (DeviceResource) resource;

            // Add it to device topology
            deviceTopology.addDeviceResource(deviceResource);

            // If device resource has terminal capability, add it to managed capabilities
            TerminalCapability terminalCapability = deviceResource.getCapability(TerminalCapability.class);
            if (terminalCapability != null) {
                addResourceCapability(terminalCapability);
            }

            // If device resource has virtual rooms capability, add it to managed capabilities
            VirtualRoomsCapability virtualRoomsCapability = deviceResource.getCapability(VirtualRoomsCapability.class);
            if (virtualRoomsCapability != null) {
                addResourceCapability(virtualRoomsCapability);
            }
        }

        // Update resource state
        if (entityManager != null) {
            updateResourceState(resourceState, entityManager);
        }
    }

    /**
     * Delete resource from the resource database
     *
     * @param resource
     */
    public void removeResource(Resource resource)
    {
        Long resourceId = resource.getId();
        if (resourceById.containsKey(resourceId) == false) {
            throw new IllegalArgumentException("Resource '" + resourceId + "' is not in the resource database!");
        }

        // If resource is a device
        if (resource instanceof DeviceResource) {
            DeviceResource deviceResource = (DeviceResource) resource;

            // Remove the device from the device topology
            deviceTopology.removeDeviceResource(deviceResource);

            // If also has virtual rooms, remove managed capability
            if (deviceResource.hasCapability(TerminalCapability.class)) {
                removeResourceCapability(deviceResource, TerminalCapability.class);
            }

            // If also has virtual rooms, remove managed capability
            if (deviceResource.hasCapability(VirtualRoomsCapability.class)) {
                removeResourceCapability(deviceResource, VirtualRoomsCapability.class);
            }
        }

        // Remove the resource state
        resourceStateById.remove(resourceId);

        // Remove resource from the list of all resources
        resourceById.remove(resourceId);
    }

    /**
     * @param allocatedResource to be added to the resource database
     */
    public void addAllocatedResource(AllocatedResource allocatedResource)
    {
        allocatedResource.checkPersisted();
        ResourceState resourceState = resourceStateById.get(allocatedResource.getResource().getId());
        if (resourceState == null) {
            throw new IllegalStateException("Resource is not maintained by the resource database.");
        }
        resourceState.addAllocatedResource(allocatedResource);
    }

    /**
     * @param allocatedResource to be removed from the resource database
     */
    public void removeAllocatedResource(AllocatedResource allocatedResource)
    {
        ResourceState resourceState = resourceStateById.get(allocatedResource.getResource().getId());
        if (resourceState == null) {
            throw new IllegalStateException("Resource is not maintained by the resource database.");
        }
        resourceState.removeAllocatedResource(allocatedResource);
    }

    /**
     * Add resource capability to be managed and resource can be looked up by it.
     *
     * @param capability
     */
    private void addResourceCapability(Capability capability)
    {
        Class<? extends Capability> capabilityType = capability.getClass();

        // Get capability state and add the capability to it
        CapabilityState capabilityState = capabilityStateByType.get(capabilityType);
        if (capabilityState == null) {
            capabilityState = new CapabilityState(capabilityType);
            capabilityStateByType.put(capabilityType, capabilityState);
        }
        capabilityState.addCapability(capability);
    }

    /**
     * Remove resource capability from managed capabilities.
     *
     * @param resource
     * @param capabilityType
     */
    private void removeResourceCapability(Resource resource, Class<? extends Capability> capabilityType)
    {
        // Get capability state
        CapabilityState capabilityState = capabilityStateByType.get(capabilityType);
        if (capabilityState == null) {
            return;
        }
        capabilityState.removeCapability(resource);
    }

    /**
     * @param resourceId
     * @param capabilityType
     * @return capability of given {@code capabilityType} from resource with given {@code resourceId}
     */
    private <T extends Capability> T getResourceCapability(Long resourceId, Class<T> capabilityType)
    {
        CapabilityState capabilityState = capabilityStateByType.get(capabilityType);
        if (capabilityState == null) {
            return null;
        }
        return capabilityType.cast(capabilityState.getCapability(resourceId));
    }

    /**
     * @param resourceId
     * @param capabilityType
     * @return true if resource with given {@code resourceId} has capability of given {@code capabilityType},
     *         false otherwise
     */
    private boolean hasResourceCapability(Long resourceId, Class<? extends Capability> capabilityType)
    {
        return getResourceCapability(resourceId, capabilityType) != null;
    }

    /**
     * @param capabilityType
     * @return set of resource identifiers which has capability of given {@code capabilityType}
     */
    private Set<Long> getResourcesByCapability(Class<? extends Capability> capabilityType)
    {
        CapabilityState capabilityState = capabilityStateByType.get(capabilityType);
        if (capabilityState == null) {
            return new HashSet<Long>();
        }
        return capabilityState.getResourceIds();
    }

    /**
     * @param capabilityType
     * @param technologies
     * @return set of device resource identifiers which has capability of given {@code capabilityType}
     *         supporting given {@code technologies}
     */
    public Set<Long> getDeviceResourcesByCapabilityTechnologies(Class<? extends DeviceCapability> capabilityType,
            Set<Technology> technologies)
    {
        CapabilityState capabilityState = capabilityStateByType.get(capabilityType);
        if (capabilityState == null) {
            return new HashSet<Long>();
        }
        if (technologies == null) {
            return getResourcesByCapability(capabilityType);
        }
        Set<Long> devices = null;
        for (Technology technology : technologies) {
            Set<Long> technologyDevices = capabilityState.getResourceIds(technology);
            if (devices == null) {
                devices = new HashSet<Long>();
                if (technologyDevices != null) {
                    devices.addAll(technologyDevices);
                }
            }
            else if (technologyDevices == null) {
                devices.clear();
                break;
            }
            else {
                devices.retainAll(technologyDevices);
            }
        }
        return devices;
    }

    /**
     * @param capabilityType
     * @param technologySets
     * @return set of device resource identifiers which has capability of given {@code capabilityType}
     *         supporting at least one set of given {@code technologySets}
     */
    public Set<Long> getDeviceResourcesByCapabilityTechnologies(
            Class<? extends DeviceCapability> capabilityType, Collection<Set<Technology>> technologySets)
    {
        if (technologySets == null) {
            return getDeviceResourcesByCapabilityTechnologies(capabilityType, (Set<Technology>) null);
        }
        Set<Long> devices = new HashSet<Long>();
        for (Set<Technology> technologies : technologySets) {
            devices.addAll(getDeviceResourcesByCapabilityTechnologies(capabilityType, technologies));
        }
        return devices;
    }

    /**
     * Remove all allocations for the given resource and add them again for current working interval.
     *
     * @param resourceState
     * @param entityManager
     */
    private void updateResourceState(ResourceState resourceState, EntityManager entityManager)
    {
        resourceState.clear();

        if (getWorkingInterval() != null) {
            // Get all allocated virtual rooms for the device and add them to the device state
            ResourceManager resourceManager = new ResourceManager(entityManager);
            List<AllocatedResource> allocations = resourceManager.listResourceAllocationsInInterval(
                    resourceState.getResourceId(), getWorkingInterval());
            for (AllocatedResource allocation : allocations) {
                addAllocatedResource(allocation);
            }
        }
    }

    /**
     * Checks whether {@code resource} and all it's children are available (recursive).
     * Device resources with {@link cz.cesnet.shongo.controller.resource.VirtualRoomsCapability} can be available even if theirs capacity is fully used.
     *
     * @param resource
     * @param interval
     * @return true if given {@code resource} is available, false otherwise
     */
    public boolean isResourceAndChildResourcesAvailableRecursive(Resource resource, Interval interval)
    {
        Long resourceId = resource.getId();
        if (!resource.isAllocatable() || !resource.isAvailableInFuture(interval.getEnd(), getReferenceDateTime())) {
            return false;
        }
        // Check only resources without virtual rooms
        if (!hasResourceCapability(resourceId, VirtualRoomsCapability.class)) {
            ResourceState resourceState = resourceStateById.get(resourceId);
            Set<AllocatedResource> allocatedResources = resourceState.getAllocatedResources(interval);
            if (allocatedResources.size() > 0) {
                return false;
            }
        }
        // Check child resources
        for (Resource childResource : resource.getChildResources()) {
            if (!isResourceAndChildResourcesAvailableRecursive(childResource, interval)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param resourceId
     * @param interval
     * @return collection of allocations for resource with given {@code resourceId} in given {@code interval}
     */
    public Collection<AllocatedResource> getResourceAllocations(Long resourceId, Interval interval)
    {
        ResourceState resourceState = resourceStateById.get(resourceId);
        if (resourceState == null) {
            throw new IllegalArgumentException("Resource '" + resourceId + "' isn't added to the resource database.");
        }
        return resourceState.getAllocatedResources(interval);
    }

    /**
     * @param deviceResource
     * @param interval
     * @return {@link cz.cesnet.shongo.controller.cache.AvailableVirtualRoom} for given {@code deviceResource} in given {@code interval}
     */
    public AvailableVirtualRoom getAvailableVirtualRoom(DeviceResource deviceResource, Interval interval)
    {
        ResourceState resourceState = resourceStateById.get(deviceResource.getId());
        if (resourceState == null) {
            throw new IllegalArgumentException("Resource '" + deviceResource.getId()
                    + "' is not device or isn't added to the resource database.");
        }
        VirtualRoomsCapability virtualRoomsCapability
                = getResourceCapability(deviceResource.getId(), VirtualRoomsCapability.class);
        if (virtualRoomsCapability == null) {
            throw new IllegalStateException("Device resource doesn't have VirtualRooms capability.");
        }
        Set<AllocatedResource> allocatedResources = resourceState.getAllocatedResources(interval);
        int usedPortCount = 0;
        for (AllocatedResource allocatedResource : allocatedResources) {
            if (!(allocatedResource instanceof AllocatedVirtualRoom)) {
                throw new IllegalStateException(
                        "Device resource with VirtualRooms capability should be allocated only as virtual room.");
            }
            AllocatedVirtualRoom allocatedVirtualRoom = (AllocatedVirtualRoom) allocatedResource;
            usedPortCount += allocatedVirtualRoom.getPortCount();
        }
        AvailableVirtualRoom availableVirtualRoom = new AvailableVirtualRoom();
        availableVirtualRoom.setDeviceResource(deviceResource);
        availableVirtualRoom.setMaximumPortCount(virtualRoomsCapability.getPortCount());
        availableVirtualRoom.setAvailablePortCount(virtualRoomsCapability.getPortCount() - usedPortCount);
        return availableVirtualRoom;
    }

    /**
     * Find {@link cz.cesnet.shongo.controller.cache.AvailableVirtualRoom}s in given {@code deviceResources} for given {@code interval} which have
     * at least {@code requiredPortCount} available ports.
     *
     * @param interval
     * @param requiredPortCount
     * @param deviceResources
     * @return list of {@link cz.cesnet.shongo.controller.cache.AvailableVirtualRoom}
     */
    public List<AvailableVirtualRoom> findAvailableVirtualRoomsInDeviceResources(Interval interval,
            int requiredPortCount, Set<Long> deviceResources)
    {
        List<AvailableVirtualRoom> availableVirtualRooms = new ArrayList<AvailableVirtualRoom>();
        for (Long deviceResourceId : deviceResources) {
            DeviceResource deviceResource = (DeviceResource) resourceById.get(deviceResourceId);
            if (!deviceResource.isAllocatable() || !deviceResource
                    .isAvailableInFuture(interval.getEnd(), getReferenceDateTime())) {
                continue;
            }
            ResourceState resourceState = resourceStateById.get(deviceResourceId);
            Set<AllocatedResource> allocatedResources = resourceState.getAllocatedResources(interval);
            int usedPortCount = 0;
            for (AllocatedResource allocatedResource : allocatedResources) {
                if (!(allocatedResource instanceof AllocatedVirtualRoom)) {
                    throw new IllegalStateException(
                            "Device resource with VirtualRooms capability should be allocated only as virtual room.");
                }
                AllocatedVirtualRoom allocatedVirtualRoom = (AllocatedVirtualRoom) allocatedResource;
                usedPortCount += allocatedVirtualRoom.getPortCount();
            }
            VirtualRoomsCapability virtualRoomsCapability
                    = getResourceCapability(deviceResourceId, VirtualRoomsCapability.class);
            if (virtualRoomsCapability == null) {
                throw new IllegalStateException("Device resource should have VirtualRooms capability filled.");
            }
            int availablePortCount = virtualRoomsCapability.getPortCount() - usedPortCount;
            if (availablePortCount >= requiredPortCount) {
                AvailableVirtualRoom availableVirtualRoom = new AvailableVirtualRoom();
                availableVirtualRoom.setDeviceResource(deviceResource);
                availableVirtualRoom.setMaximumPortCount(virtualRoomsCapability.getPortCount());
                availableVirtualRoom.setAvailablePortCount(availablePortCount);
                availableVirtualRooms.add(availableVirtualRoom);
            }
        }
        return availableVirtualRooms;
    }
}
