package cz.cesnet.shongo.controller.allocation;

import cz.cesnet.shongo.api.FaultException;
import cz.cesnet.shongo.api.Technology;
import cz.cesnet.shongo.controller.Component;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.ResourceManager;
import cz.cesnet.shongo.controller.resource.VirtualRoomsCapability;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.*;

/**
 * Holds database of allocated virtual rooms for device resources with {@link VirtualRoomsCapability}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class VirtualRoomDatabase
{
    private static Logger logger = LoggerFactory.getLogger(VirtualRoomDatabase.class);

    /**
     * Working interval for which are loaded allocated virtual rooms.
     */
    private Interval workingInterval;

    /**
     * Map of device resource states by theirs identifiers.
     */
    private Map<Long, DeviceResourceState> deviceStateById = new HashMap<Long, DeviceResourceState>();

    /**
     * Map of device identifiers by theirs technologies
     */
    private Map<Technology, Set<Long>> devicesByTechnology = new HashMap<Technology, Set<Long>>();

    /**
     * Load all devices with {@link VirtualRoomsCapability} and all allocated virtual rooms for them.
     *
     * @param entityManager entity manager for loading allocated virtual rooms
     */
    public void loadDeviceResources(EntityManager entityManager)
    {
        deviceStateById.clear();
        devicesByTechnology.clear();

        ResourceManager resourceManager = new ResourceManager(entityManager);
        List<DeviceResource> deviceResources = resourceManager.listDevicesWithCapability(VirtualRoomsCapability.class);
        for (DeviceResource deviceResource : deviceResources) {
            addDeviceResource(deviceResource, entityManager);
        }
    }

    /**
     * Set new working interval
     *
     * @param workingInterval
     * @param entityManager
     */
    public void setWorkingInterval(Interval workingInterval, EntityManager entityManager)
    {
        workingInterval = new Interval(workingInterval.getStart().minus(AllocatedVirtualRoom.MAXIMUM_DURATION),
                workingInterval.getEnd().plus(AllocatedVirtualRoom.MAXIMUM_DURATION));
        if (!workingInterval.equals(this.workingInterval)) {
            logger.info("Setting new working interval '{}' to database of virtual rooms...",
                    Component.formatInterval(workingInterval));
            this.workingInterval = workingInterval;
            // Remove all allocated virtual rooms from all resources and add it again for the new interval
            for (DeviceResourceState deviceResourceState : deviceStateById.values()) {
                updateDeviceResourceState(deviceResourceState, entityManager);
            }
        }
    }

    /**
     * @param deviceResource device resource with {@link VirtualRoomsCapability} whose all
     *                       {@link AllocatedVirtualRoom}s should be added to the database
     * @param entityManager  entity manager for loading allocated virtual rooms
     */
    public void addDeviceResource(DeviceResource deviceResource, EntityManager entityManager)
    {
        VirtualRoomsCapability virtualRoomsCapability = deviceResource.getCapability(VirtualRoomsCapability.class);
        if (virtualRoomsCapability == null) {
            throw new IllegalArgumentException("Device resource doesn't have the VirtualRooms capability.");
        }
        DeviceResourceState deviceResourceState = deviceStateById.get(deviceResource.getId());
        if (deviceResourceState == null) {
            deviceResourceState = new DeviceResourceState(deviceResource.getId());
            deviceStateById.put(deviceResource.getId(), deviceResourceState);
        }

        // Add the device to map by technology
        Set<Technology> technologies = virtualRoomsCapability.getTechnologies();
        if (technologies.size() == 0) {
            technologies = deviceResource.getTechnologies();
        }
        for (Technology technology : technologies) {
            Set<Long> devices = devicesByTechnology.get(technology);
            if (devices == null) {
                devices = new HashSet<Long>();
                devicesByTechnology.put(technology, devices);
            }
            devices.add(deviceResource.getId());
        }

        // Set the capability to the device state
        deviceResourceState.virtualRoomsCapability = virtualRoomsCapability;

        updateDeviceResourceState(deviceResourceState, entityManager);
    }

    /**
     * Remove all allocated virtual rooms for the given device and add them again for current working interval.
     *
     * @param deviceResourceState
     * @param entityManager
     */
    private void updateDeviceResourceState(DeviceResourceState deviceResourceState, EntityManager entityManager)
    {
        deviceResourceState.allocatedVirtualRooms.clear();

        if (workingInterval != null) {
            // Get all allocated virtual rooms for the device and add them to the device state
            ResourceManager resourceManager = new ResourceManager(entityManager);
            List<AllocatedResource> allocations =
                    resourceManager.listResourceAllocationsInInterval(deviceResourceState.deviceResourceId,
                            workingInterval);
            for (AllocatedResource allocation : allocations) {
                if (!(allocation instanceof AllocatedVirtualRoom)) {
                    throw new IllegalStateException("Device resource with VirtualRooms capability can be allocated only"
                            + " as allocated virtual rooms.");
                }
                addAllocatedVirtualRoom((AllocatedVirtualRoom) allocation);
            }
        }
    }

    /**
     * @param allocatedVirtualRoom
     */
    public void addAllocatedVirtualRoom(AllocatedVirtualRoom allocatedVirtualRoom)
    {
        DeviceResourceState deviceResourceState = deviceStateById.get(allocatedVirtualRoom.getResource().getId());
        if (deviceResourceState == null) {
            throw new IllegalStateException("Device in which the virtual rooms is allocated is not maintained.");
        }
        Interval slot = allocatedVirtualRoom.getSlot();
        deviceResourceState.allocatedVirtualRooms.add(allocatedVirtualRoom, slot.getStart(), slot.getEnd());

        allocatedVirtualRoom.checkPersisted();
        deviceResourceState.allocatedVirtualRoomById.put(allocatedVirtualRoom.getId(), allocatedVirtualRoom);
    }

    /**
     * @param deviceResource device resource whose allocations should be updated
     * @param entityManager  entity manager for loading allocated virtual rooms
     */
    public void updateDeviceResource(DeviceResource deviceResource, EntityManager entityManager)
    {
        removeDeviceResource(deviceResource, entityManager);
        addDeviceResource(deviceResource, entityManager);
    }

    /**
     * @param deviceResource device resource for which all it's {@link AllocatedVirtualRoom}s should be removed
     * @param entityManager  reserved for future use
     */
    public void removeDeviceResource(DeviceResource deviceResource, EntityManager entityManager)
    {
        // Remove the device to map by technology
        for (Technology technology : deviceResource.getTechnologies()) {
            Set<Long> devices = devicesByTechnology.get(technology);
            if (devices != null) {
                devices.remove(deviceResource.getId());
            }
        }

        // Remove the device state
        deviceStateById.remove(deviceResource.getId());
    }

    /**
     * @param allocatedVirtualRoom
     */
    public void removeAllocatedVirtualRoom(AllocatedVirtualRoom allocatedVirtualRoom)
    {
        DeviceResourceState deviceResourceState = deviceStateById.get(allocatedVirtualRoom.getResource().getId());
        if (deviceResourceState == null) {
            throw new IllegalStateException("Device in which the virtual rooms is allocated is not maintained.");
        }

        allocatedVirtualRoom = deviceResourceState.allocatedVirtualRoomById.get(allocatedVirtualRoom.getId());
        if (allocatedVirtualRoom == null ) {
            throw new IllegalStateException("Allocated virtual rooms doesn't exist in the virtual rooms database.");
        }
        deviceResourceState.allocatedVirtualRooms.remove(allocatedVirtualRoom);
    }

    /**
     * Find available devices in given {@code interval} which have at least {@code requiredPortCount} available ports
     * and which supports given {@code technologies}.
     *
     * @param interval
     * @param requiredPortCount
     * @param technologies
     * @param entityManager
     * @return list of available devices
     */
    public List<AvailableVirtualRoom> findAvailableVirtualRooms(Interval interval, int requiredPortCount,
            Technology[] technologies, EntityManager entityManager)
    {
        Set<Long> devices = null;
        for (Technology technology : technologies) {
            Set<Long> technologyDevices = devicesByTechnology.get(technology);
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
        if (devices == null) {
            devices = deviceStateById.keySet();
        }

        ResourceManager resourceManager = new ResourceManager(entityManager);
        List<AvailableVirtualRoom> availableVirtualRooms = new ArrayList<AvailableVirtualRoom>();
        for (Long deviceId : devices) {
            DeviceResourceState deviceResourceState = deviceStateById.get(deviceId);
            Set<AllocatedVirtualRoom> allocatedVirtualRooms = deviceResourceState.allocatedVirtualRooms
                    .getValues(interval.getStart(), interval.getEnd());
            int usedPortCount = 0;
            for (AllocatedVirtualRoom allocatedVirtualRoom : allocatedVirtualRooms) {
                usedPortCount += allocatedVirtualRoom.getPortCount();
            }
            int availablePortCount = deviceResourceState.virtualRoomsCapability.getPortCount() - usedPortCount;
            if (availablePortCount >= requiredPortCount) {
                AvailableVirtualRoom availableVirtualRoom = new AvailableVirtualRoom();
                try {
                    availableVirtualRoom.setDeviceResource(resourceManager.getDevice(deviceId));
                }
                catch (FaultException exception) {
                    throw new IllegalStateException("Cannot find device resource for available virtual room",
                            exception);
                }
                availableVirtualRoom.setAvailablePortCount(availablePortCount);
                availableVirtualRooms.add(availableVirtualRoom);
            }
        }
        return availableVirtualRooms;
    }

    /**
     * @see {@link #findAvailableVirtualRooms}
     */
    public List<AvailableVirtualRoom> findAvailableVirtualRooms(Interval interval, int requiredPortCount,
            EntityManager entityManager)
    {
        return findAvailableVirtualRooms(interval, requiredPortCount, new Technology[0], entityManager);
    }

    /**
     * Current state of a device resource with {@link VirtualRoomsCapability}.
     */
    private static class DeviceResourceState
    {
        /**
         * Device resource identifier.
         */
        private Long deviceResourceId;

        /**
         * {@link VirtualRoomsCapability} for the device.
         */
        private VirtualRoomsCapability virtualRoomsCapability;

        /**
         * Already allocated {@link AllocatedVirtualRoom} for the device.
         */
        private RangeSet<AllocatedVirtualRoom, DateTime> allocatedVirtualRooms = new RangeSet<AllocatedVirtualRoom, DateTime>();

        /**
         * Map of allocated virtual rooms by id.
         */
        private Map<Long, AllocatedVirtualRoom> allocatedVirtualRoomById = new HashMap<Long, AllocatedVirtualRoom>();

        /**
         * Constructor.
         *
         * @param deviceResourceId sets the {@link #deviceResourceId}
         */
        public DeviceResourceState(Long deviceResourceId)
        {
            this.deviceResourceId = deviceResourceId;
        }
    }
}
