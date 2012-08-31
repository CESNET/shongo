package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.PersistentObject;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.allocation.AllocatedResource;
import cz.cesnet.shongo.controller.allocation.AllocatedVirtualRoom;
import cz.cesnet.shongo.controller.allocation.AvailableVirtualRoom;
import cz.cesnet.shongo.controller.resource.*;
import cz.cesnet.shongo.controller.resource.topology.DeviceTopology;
import cz.cesnet.shongo.controller.util.RangeSet;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.util.TemporalHelper;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.*;

/**
 * Represents a component for a domain controller that holds all resources in memory in efficient form. It also holds
 * allocation information about resources which are used , e.g., by scheduler.
 * <p/>
 * Resources must be explicitly added by {@link #addResource(Resource, EntityManager)} or automatically loaded in
 * {@link Component#init(Configuration)}.
 * <p/>
 * Database holds only allocations inside the {@link #workingInterval} (automatically extended by
 * {@link #deviceAllocationMaximumDuration}). If the {@link #workingInterval} isn't set
 * the database of allocations will be empty (it is not desirable to load all allocations for the entire time span).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceDatabase extends Component implements Component.EntityManagerFactoryAware
{
    private static Logger logger = LoggerFactory.getLogger(ResourceDatabase.class);

    /**
     * List of all resources in resource database by theirs id.
     */
    private Map<Long, Resource> resourceById = new HashMap<Long, Resource>();

    /**
     * Maximum duration of a device allocation.
     */
    private Duration deviceAllocationMaximumDuration = Duration.standardDays(1);

    /**
     * Working interval for which are loaded allocated virtual rooms.
     */
    private Interval workingInterval;

    /**
     * Represents a reference data time which is a rounded now().
     */
    private DateTime referenceDateTime;

    /**
     * Map of device resource states by theirs identifiers.
     */
    private Map<Long, ResourceState> resourceStateById = new HashMap<Long, ResourceState>();

    /**
     * {@link EntityManagerFactory} used to load resources in {@link #init(Configuration)} method.
     */
    private EntityManagerFactory entityManagerFactory;

    /**
     * Specifies whether resources and allocations don't have to be persisted before are added to the resource database
     * (useful for testing purposes).
     */
    private boolean disabledPersistedRequirement = false;

    /**
     * Map of capability states by theirs types.
     */
    private Map<Class<? extends Capability>, CapabilityState> capabilityStateByType =
            new HashMap<Class<? extends Capability>, CapabilityState>();

    /**
     * @see {@link DeviceTopology}
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
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
    {
        this.entityManagerFactory = entityManagerFactory;
    }

    /**
     * Sets the {@link #disabledPersistedRequirement} option to {@code true}.
     */
    public void disablePersistedRequirement()
    {
        disabledPersistedRequirement = true;
    }

    @Override
    public void init(Configuration configuration)
    {
        super.init(configuration);

        Duration duration = configuration.getDuration(Configuration.RESOURCE_DEVICE_ALLOCATION_MAX_DURATION);
        if (duration != null) {
            deviceAllocationMaximumDuration = duration;
        }

        if (entityManagerFactory != null) {
            EntityManager entityManager = entityManagerFactory.createEntityManager();

            logger.debug("Loading resource database...");

            // Load all resources from db
            ResourceManager resourceManager = new ResourceManager(entityManager);
            List<Resource> resourceList = resourceManager.list();

            for (Resource resource : resourceList) {
                try {

                    addResource(resource, entityManager);
                }
                catch (FaultException exception) {
                    throw new IllegalStateException("Fail to add resource.", exception);
                }
            }
        }
    }

    @Override
    public void destroy()
    {
        logger.debug("Closing resource database...");

        workingInterval = null;
        resourceById.clear();
        resourceStateById.clear();
        capabilityStateByType.clear();
        deviceTopology.clear();

        super.destroy();
    }

    /**
     * @return {@link #deviceAllocationMaximumDuration}
     */
    public Duration getDeviceAllocationMaximumDuration()
    {
        return deviceAllocationMaximumDuration;
    }

    /**
     * @param deviceAllocationMaximumDuration
     *         sets the {@link #deviceAllocationMaximumDuration}
     */
    public void setDeviceAllocationMaximumDuration(Duration deviceAllocationMaximumDuration)
    {
        this.deviceAllocationMaximumDuration = deviceAllocationMaximumDuration;
    }

    /**
     * @return {@link #workingInterval}
     */
    public Interval getWorkingInterval()
    {
        return workingInterval;
    }

    /**
     * @param workingInterval sets the {@link #workingInterval}
     * @param entityManager   used for reloading allocations of resources for the new interval
     */
    public void setWorkingInterval(Interval workingInterval, EntityManager entityManager)
    {
        Interval adjustedWorkingInterval = new Interval(
                workingInterval.getStart().minus(deviceAllocationMaximumDuration),
                workingInterval.getEnd().plus(deviceAllocationMaximumDuration));
        if (!adjustedWorkingInterval.equals(this.workingInterval)) {
            logger.info("Setting new working interval '{}' to resource database...",
                    TemporalHelper.formatInterval(adjustedWorkingInterval));
            this.workingInterval = adjustedWorkingInterval;
            this.referenceDateTime = workingInterval.getStart();
            // Remove all allocated virtual rooms from all resources and add it again for the new interval
            for (ResourceState resourceState : resourceStateById.values()) {
                updateResourceState(resourceState, entityManager);
            }
        }
    }

    private void checkPersisted(PersistentObject persistentObject)
    {
        if (!persistentObject.isPersisted()) {
            if (disabledPersistedRequirement) {
                persistentObject.setDebugId();
                return;
            }
            persistentObject.checkPersisted();
        }
    }

    /**
     * Add new resource to the resource database. If the resource is not persisted yet it is created in the database.
     *
     * @param resource      resource to be added to the resource database
     * @param entityManager entity manager used for storing not persisted resource to the database and
     *                      for loading resource allocations
     * @throws FaultException when the creating in the database fails
     */
    public void addResource(Resource resource, EntityManager entityManager) throws FaultException
    {
        // Create resource in the database if it wasn't created yet
        if (entityManager != null && !resource.isPersisted()) {
            ResourceManager resourceManager = new ResourceManager(entityManager);
            resourceManager.create(resource);
        }

        checkPersisted(resource);
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
                addResourceCapability(deviceResource, terminalCapability);
            }

            // If device resource has virtual rooms capability, add it to managed capabilities
            VirtualRoomsCapability virtualRoomsCapability = deviceResource.getCapability(VirtualRoomsCapability.class);
            if (virtualRoomsCapability != null) {
                addResourceCapability(deviceResource, virtualRoomsCapability);
            }
        }

        // Update resource state
        if (entityManager != null) {
            updateResourceState(resourceState, entityManager);
        }
    }

    /**
     * @see {@link #addResource(Resource, EntityManager)}
     */
    public void addResource(Resource resource) throws FaultException
    {
        addResource(resource, null);
    }

    /**
     * Update resource in the resource database.
     *
     * @param resource
     * @param entityManager entity manager used for loading of resource allocations from the database
     */
    public void updateResource(Resource resource, EntityManager entityManager)
    {
        removeResource(resource);
        try {
            addResource(resource, entityManager);
        }
        catch (FaultException exception) {
            throw new IllegalStateException("Failed to update resource in the resource database.", exception);
        }
    }

    /**
     * @see {@link #updateResource(Resource, EntityManager)}
     */
    public void updateResource(Resource resource)
    {
        updateResource(resource, null);
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
        ResourceState resourceState = resourceStateById.get(allocatedResource.getResource().getId());
        if (resourceState == null) {
            throw new IllegalStateException("Resource which is allocated is not maintained by the resource database.");
        }
        Interval slot = allocatedResource.getSlot();
        // TODO: check if allocation doesn't collide
        resourceState.allocations.add(allocatedResource, slot.getStart(), slot.getEnd());

        checkPersisted(allocatedResource);
        resourceState.allocationsById.put(allocatedResource.getId(), allocatedResource);
    }

    /**
     * @param allocatedResource to be removed from the resource database
     */
    public void removeAllocatedResource(AllocatedResource allocatedResource)
    {
        ResourceState resourceState = resourceStateById.get(allocatedResource.getResource().getId());
        if (resourceState == null) {
            throw new IllegalStateException("Resource which is allocated is not maintained by the resource database.");
        }

        allocatedResource = resourceState.allocationsById.get(allocatedResource.getId());
        if (allocatedResource == null) {
            throw new IllegalStateException("Allocated resource doesn't exist in the resource database.");
        }
        resourceState.allocations.remove(allocatedResource);
    }

    /**
     * Add resource capability to be managed and resource can be looked up by it.
     *
     * @param resource
     * @param capability
     */
    private void addResourceCapability(Resource resource, Capability capability)
    {
        Long resourceId = resource.getId();
        Class<? extends Capability> capabilityType = capability.getClass();

        // Get capability state
        CapabilityState capabilityState = capabilityStateByType.get(capabilityType);
        if (capabilityState == null) {
            capabilityState = new CapabilityState(capabilityType);
            capabilityStateByType.put(capabilityType, capabilityState);
        }

        // Add the capability by it's resource to the map
        capabilityState.capabilityByResourceId.put(resourceId, capability);

        if (capability instanceof DeviceCapability) {
            DeviceResource deviceResource = (DeviceResource) resource;
            DeviceCapability deviceCapability = (DeviceCapability) capability;

            // Add the device resource to map of virtual room resources by technology
            for (Technology technology : deviceResource.getCapabilityTechnologies(deviceCapability)) {
                Set<Long> deviceResources = capabilityState.deviceResourcesByTechnology.get(technology);
                if (deviceResources == null) {
                    deviceResources = new HashSet<Long>();
                    capabilityState.deviceResourcesByTechnology.put(technology, deviceResources);
                }
                deviceResources.add(resourceId);
            }
        }
    }

    /**
     * Remove resource capability from managed capabilities.
     *
     * @param resource
     * @param capabilityType
     */
    private void removeResourceCapability(Resource resource, Class<? extends Capability> capabilityType)
    {
        Long resourceId = resource.getId();

        // Get capability state
        CapabilityState capabilityState = capabilityStateByType.get(capabilityType);
        if (capabilityState == null) {
            return;
        }

        // Remove the device resource from the set of virtual room resources
        capabilityState.capabilityByResourceId.remove(resourceId);

        if (capabilityType.isAssignableFrom(DeviceCapability.class)) {
            DeviceResource deviceResource = (DeviceResource) resource;

            // Remove the device resource from map by technology
            for (Technology technology : deviceResource.getTechnologies()) {
                Set<Long> devices = capabilityState.deviceResourcesByTechnology.get(technology);
                if (devices != null) {
                    devices.remove(resourceId);
                }
            }
        }
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
        return capabilityType.cast(capabilityState.capabilityByResourceId.get(resourceId));
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
        return capabilityState.capabilityByResourceId.keySet();
    }

    /**
     * @param capabilityType
     * @param technologies
     * @return set of device resource identifiers which has capability of given {@code capabilityType}
     *         supporting given {@code technologies}
     */
    private Set<Long> getDeviceResourcesByCapabilityTechnologies(Class<? extends DeviceCapability> capabilityType,
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
            Set<Long> technologyDevices = capabilityState.deviceResourcesByTechnology.get(technology);
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
    private Set<Long> getDeviceResourcesByCapabilityTechnologies(
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
        resourceState.allocations.clear();

        if (workingInterval != null) {
            // Get all allocated virtual rooms for the device and add them to the device state
            ResourceManager resourceManager = new ResourceManager(entityManager);
            List<AllocatedResource> allocations =
                    resourceManager.listResourceAllocationsInInterval(resourceState.resourceId, workingInterval);
            for (AllocatedResource allocation : allocations) {
                addAllocatedResource(allocation);
            }
        }
    }

    /**
     * Checks whether given {@code resource} and all it's dependent resource are available.
     * Device resources with {@link VirtualRoomsCapability} are always available (if theirs capacity is fully used).
     *
     * @param resource
     * @param interval
     * @return true if given {@code resource} is available, false otherwise
     */
    public boolean isResourceAvailable(Resource resource, Interval interval)
    {
        // Get top parent resource and checks whether it is available
        Resource parentResource = resource;
        while (parentResource.getParentResource() != null) {
            parentResource = parentResource.getParentResource();
        }
        return isResourceAndChildResourcesAvailableRecursive(parentResource, interval);
    }

    /**
     * Checks whether {@code resource} and all it's children are available (recursive).
     * Device resources with {@link VirtualRoomsCapability} can be available even if theirs capacity is fully used.
     *
     * @param resource
     * @param interval
     * @return true if given {@code resource} is available, false otherwise
     */
    private boolean isResourceAndChildResourcesAvailableRecursive(Resource resource, Interval interval)
    {
        Long resourceId = resource.getId();
        if (!resource.isSchedulable() || !resource.isAvailableInFuture(interval.getEnd(), referenceDateTime)) {
            return false;
        }
        // Check only resources without virtual rooms
        if (!hasResourceCapability(resourceId, VirtualRoomsCapability.class)) {
            ResourceState resourceState = resourceStateById.get(resourceId);
            Set<AllocatedResource> allocatedResources =
                    resourceState.allocations.getValues(interval.getStart(), interval.getEnd());
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
        return resourceState.allocations.getValues(interval.getStart(), interval.getEnd());
    }

    /**
     * @param interval
     * @param technologies
     * @return list of available terminals
     */
    public List<DeviceResource> findAvailableTerminal(Interval interval, Set<Technology> technologies)
    {
        Set<Long> terminals = getDeviceResourcesByCapabilityTechnologies(TerminalCapability.class, technologies);

        List<DeviceResource> deviceResources = new ArrayList<DeviceResource>();
        for (Long terminalId : terminals) {
            DeviceResource deviceResource = (DeviceResource) resourceById.get(terminalId);
            if (deviceResource == null) {
                throw new IllegalStateException("Device resource should be added to the resource database.");
            }
            if (isResourceAvailable(deviceResource, interval)) {
                deviceResources.add(deviceResource);
            }
        }
        return deviceResources;
    }

    /**
     * @see {@link #findAvailableTerminal}
     */
    public List<DeviceResource> findAvailableTerminal(Interval interval, Technology[] technologies)
    {
        Set<Technology> technologySet = new HashSet<Technology>();
        Collections.addAll(technologySet, technologies);
        return findAvailableTerminal(interval, technologySet);
    }

    /**
     * @param deviceResource
     * @param interval
     * @return {@link AvailableVirtualRoom} for given {@code deviceResource} in given {@code interval}
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
        Set<AllocatedResource> allocatedResources =
                resourceState.allocations.getValues(interval.getStart(), interval.getEnd());
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
     * Find {@link AvailableVirtualRoom}s in given {@code deviceResources} for given {@code interval} which have
     * at least {@code requiredPortCount} available ports.
     *
     * @param interval
     * @param requiredPortCount
     * @param deviceResources
     * @return list of {@link AvailableVirtualRoom}
     */
    private List<AvailableVirtualRoom> findAvailableVirtualRoomsInDeviceResources(Interval interval,
            int requiredPortCount, Set<Long> deviceResources)
    {
        List<AvailableVirtualRoom> availableVirtualRooms = new ArrayList<AvailableVirtualRoom>();
        for (Long deviceResourceId : deviceResources) {
            DeviceResource deviceResource = (DeviceResource) resourceById.get(deviceResourceId);
            if (!deviceResource.isSchedulable() || !deviceResource
                    .isAvailableInFuture(interval.getEnd(), referenceDateTime)) {
                continue;
            }
            ResourceState resourceState = resourceStateById.get(deviceResourceId);
            Set<AllocatedResource> allocatedResources =
                    resourceState.allocations.getValues(interval.getStart(), interval.getEnd());
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
     * Find {@link AvailableVirtualRoom}s in given {@code interval} which have at least {@code requiredPortCount}
     * available ports and which supports given {@code technologies}.
     *
     * @param interval
     * @param requiredPortCount
     * @param technologies
     * @return list of {@link AvailableVirtualRoom}
     */
    public List<AvailableVirtualRoom> findAvailableVirtualRooms(Interval interval, int requiredPortCount,
            Set<Technology> technologies)
    {
        Set<Long> deviceResources = getDeviceResourcesByCapabilityTechnologies(VirtualRoomsCapability.class,
                technologies);
        return findAvailableVirtualRoomsInDeviceResources(interval, requiredPortCount, deviceResources);
    }

    /**
     * @see {@link #findAvailableVirtualRooms}
     */
    public List<AvailableVirtualRoom> findAvailableVirtualRooms(Interval interval, int requiredPortCount,
            Technology[] technologies)
    {
        Set<Technology> technologySet = new HashSet<Technology>();
        Collections.addAll(technologySet, technologies);
        return findAvailableVirtualRooms(interval, requiredPortCount, technologySet);
    }

    /**
     * @see {@link #findAvailableVirtualRooms}
     */
    public List<AvailableVirtualRoom> findAvailableVirtualRooms(Interval interval, int requiredPortCount)
    {
        return findAvailableVirtualRooms(interval, requiredPortCount, (Set<Technology>) null);
    }

    /**
     * Find {@link AvailableVirtualRoom}s in given {@code interval} which have at least {@code requiredPortCount}
     * available ports and which supports given {@code technologies}.
     *
     * @param interval
     * @param requiredPortCount
     * @param technologiesVariants
     * @return list of {@link AvailableVirtualRoom}
     */
    public List<AvailableVirtualRoom> findAvailableVirtualRoomsByVariants(Interval interval, int requiredPortCount,
            Collection<Set<Technology>> technologiesVariants)
    {
        Set<Long> deviceResources = getDeviceResourcesByCapabilityTechnologies(VirtualRoomsCapability.class,
                technologiesVariants);
        return findAvailableVirtualRoomsInDeviceResources(interval, requiredPortCount, deviceResources);
    }

    /**
     * Current state of a resource.
     */
    private static class ResourceState
    {
        /**
         * Resource identifier.
         */
        private Long resourceId;

        /**
         * Already allocated {@link AllocatedResource} for the resource.
         */
        private RangeSet<AllocatedResource, DateTime> allocations = new RangeSet<AllocatedResource, DateTime>();

        /**
         * Map of {@link AllocatedResource}s by the resource's identifier.
         */
        private Map<Long, AllocatedResource> allocationsById = new HashMap<Long, AllocatedResource>();

        /**
         * Constructor.
         *
         * @param resourceId sets the {@link #resourceId}
         */
        public ResourceState(Long resourceId)
        {
            this.resourceId = resourceId;
        }
    }

    /**
     * Current state of capability for all resources which have it.
     */
    public static class CapabilityState
    {
        /**
         * Type of capability for which the state is managed.
         */
        Class<? extends Capability> capabilityType;

        /**
         * Constructor.
         *
         * @param capabilityType sets the {@link #capabilityType}
         */
        public CapabilityState(Class<? extends Capability> capabilityType)
        {
            this.capabilityType = capabilityType;
        }

        /**
         * Map of instances of {@link Capability} by resource identifier.
         */
        private Map<Long, Capability> capabilityByResourceId = new HashMap<Long, Capability>();

        /**
         * Map of device resources by theirs technology.
         */
        private Map<Technology, Set<Long>> deviceResourcesByTechnology = new HashMap<Technology, Set<Long>>();
    }
}
