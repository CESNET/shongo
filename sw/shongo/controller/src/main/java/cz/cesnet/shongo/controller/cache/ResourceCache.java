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
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceCache extends AbstractAllocationCache<Resource, AllocatedResource>
{
    private static Logger logger = LoggerFactory.getLogger(ResourceCache.class);

    /**
     * Map of capability states by theirs types.
     */
    private Map<Class<? extends Capability>, CapabilityState> capabilityStateByType =
            new HashMap<Class<? extends Capability>, CapabilityState>();

    /**
     * @see {@link cz.cesnet.shongo.controller.cache.DeviceTopology}
     */
    private DeviceTopology deviceTopology;

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

    /**
     * Load resources from the database.
     *
     * @param entityManager
     */
    @Override
    public void loadObjects(EntityManager entityManager)
    {
        logger.debug("Loading resources...");

        ResourceManager resourceManager = new ResourceManager(entityManager);
        List<Resource> resourceList = resourceManager.list();
        for (Resource resource : resourceList) {
            addObject(resource, entityManager);
        }
    }

    @Override
    public void addObject(Resource resource, EntityManager entityManager)
    {
        super.addObject(resource, entityManager);

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
    }

    @Override
    public void removeObject(Resource resource)
    {
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
        super.removeObject(resource);
    }

    @Override
    public void clear()
    {
        deviceTopology = new DeviceTopology();
        capabilityStateByType.clear();
        super.clear();
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

    @Override
    protected void updateObjectState(Resource resource, Interval workingInterval,
            EntityManager entityManager)
    {
        // Get all allocated virtual rooms for the device and add them to the device state
        ResourceManager resourceManager = new ResourceManager(entityManager);
        List<AllocatedResource> allocations = resourceManager.listAllocatedResourcesInInterval(
                resource.getId(), getWorkingInterval());
        for (AllocatedResource allocatedResource : allocations) {
            addAllocation(resource, allocatedResource);
        }
    }

    /**
     * Checks whether {@code resource} is available. Device resources with {@link VirtualRoomsCapability} can
     * be available even if theirs capacity is fully used.
     *
     * @param resource
     * @param interval
     * @param transaction
     * @return true if given {@code resource} is available,
     *         false otherwise
     */
    public boolean isResourceAvailable(Resource resource, Interval interval, Transaction transaction)
    {
        // Check if resource can be allocated and if it is available in the future
        if (!resource.isAllocatable() || !resource.isAvailableInFuture(interval.getEnd(), getReferenceDateTime())) {
            return false;
        }
        // Check if resource is not already allocated (only resources without virtual rooms capability, resources with
        // virtual rooms capability are available always)
        if (!hasResourceCapability(resource.getId(), VirtualRoomsCapability.class)) {
            ObjectState<AllocatedResource> resourceState = getObjectState(resource);
            Set<AllocatedResource> allocatedResources = resourceState.getAllocations(interval, transaction);
            if (allocatedResources.size() > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether all children for {@code resource} are available (recursive).
     *
     * @param resource
     * @param interval
     * @param transaction
     * @return true if children from given {@code resource} are available (recursive),
     *         false otherwise
     */
    public boolean isChildResourcesAvailable(Resource resource, Interval interval, Transaction transaction)
    {
        for (Resource childResource : resource.getChildResources()) {
            if (!isResourceAvailable(childResource, interval, transaction)) {
                return false;
            }
            if (!isChildResourcesAvailable(childResource, interval, transaction)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param resource
     * @param interval
     * @return collection of allocations for resource with given {@code resourceId} in given {@code interval}
     */
    public Collection<AllocatedResource> getResourceAllocations(Resource resource, Interval interval)
    {
        ObjectState<AllocatedResource> resourceState = getObjectState(resource);
        return resourceState.getAllocations(interval);
    }

    /**
     * @param deviceResource
     * @param interval
     * @return {@link cz.cesnet.shongo.controller.cache.AvailableVirtualRoom} for given {@code deviceResource} in given {@code interval}
     */
    public AvailableVirtualRoom getAvailableVirtualRoom(DeviceResource deviceResource, Interval interval)
    {
        ObjectState<AllocatedResource> resourceState = getObjectState(deviceResource);
        VirtualRoomsCapability virtualRoomsCapability
                = getResourceCapability(deviceResource.getId(), VirtualRoomsCapability.class);
        if (virtualRoomsCapability == null) {
            throw new IllegalStateException("Device resource doesn't have VirtualRooms capability.");
        }
        Set<AllocatedResource> allocatedResources = resourceState.getAllocations(interval);
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
            DeviceResource deviceResource = (DeviceResource) getObject(deviceResourceId);
            if (!deviceResource.isAllocatable() || !deviceResource
                    .isAvailableInFuture(interval.getEnd(), getReferenceDateTime())) {
                continue;
            }
            ObjectState<AllocatedResource> resourceState = getObjectState(deviceResource);
            Set<AllocatedResource> allocatedResources = resourceState.getAllocations(interval);
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

    /**
     * Transaction for {@link ResourceCache}.
     */
    public static class Transaction
            extends AbstractAllocationCache.Transaction<AllocatedResource>
    {
    }
}
