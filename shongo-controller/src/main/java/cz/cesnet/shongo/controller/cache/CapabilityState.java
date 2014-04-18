package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.booking.resource.Capability;
import cz.cesnet.shongo.controller.booking.resource.DeviceCapability;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.Resource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Current state of a single {@link cz.cesnet.shongo.controller.booking.resource.Capability} type for all resources which have it.
 */
public class CapabilityState
{
    /**
     * Type of {@link cz.cesnet.shongo.controller.booking.resource.Capability} for which the state is managed.
     */
    Class<? extends Capability> capabilityType;

    /**
     * Map of instances of {@link #capabilityType} by the resource ids which have them.
     */
    private Map<Long, Capability> capabilityByResourceId = new HashMap<Long, Capability>();

    /**
     * Map of {@link cz.cesnet.shongo.controller.booking.resource.DeviceResource} ids by theirs technology.
     */
    private Map<Technology, Set<Long>> deviceResourcesByTechnology = new HashMap<Technology, Set<Long>>();

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
     * @param resourceId
     * @return capability of {@link #capabilityType} for resource with given {@code resourceId}
     */
    public Capability getCapability(Long resourceId)
    {
        return capabilityByResourceId.get(resourceId);
    }

    /**
     * @return set of resource ids which have capability of {@link #capabilityType}
     */
    public Set<Long> getResourceIds()
    {
        return capabilityByResourceId.keySet();
    }

    /**
     * @param technology
     * @return set of resource ids which have capability of {@link #capabilityType} and given {@code technology}
     */
    public Set<Long> getResourceIds(Technology technology)
    {
        return deviceResourcesByTechnology.get(technology);
    }

    /**
     * @param capability to be added to the {@link CapabilityState}
     */
    public void addCapability(Capability capability)
    {
        if (!capabilityType.isInstance(capability)) {
            throw new IllegalArgumentException("Capability '" + capability.getClass().getSimpleName()
                    + "'is not instance of '" + capabilityType.getSimpleName() + "'.");
        }

        Resource resource = capability.getResource();
        Long resourceId = resource.getId();

        // Add the capability by it's resource to the map
        capabilityByResourceId.put(resourceId, capability);

        if (capability instanceof DeviceCapability) {
            DeviceResource deviceResource = (DeviceResource) resource;
            DeviceCapability deviceCapability = (DeviceCapability) capability;

            // Add the device resource to map of virtual room resources by technology
            for (Technology technology : deviceResource.getTechnologies()) {
                Set<Long> deviceResources = deviceResourcesByTechnology.get(technology);
                if (deviceResources == null) {
                    deviceResources = new HashSet<Long>();
                    deviceResourcesByTechnology.put(technology, deviceResources);
                }
                deviceResources.add(resourceId);
            }
        }
    }

    /**
     * @param resource for which the capability should be removed from the {@link CapabilityState}
     */
    public void removeCapability(Resource resource)
    {
        Long resourceId = resource.getId();

        // Remove the device resource from the set of virtual room resources
        capabilityByResourceId.remove(resourceId);

        if (capabilityType.isAssignableFrom(DeviceCapability.class)) {
            DeviceResource deviceResource = (DeviceResource) resource;

            // Remove the device resource from map by technology
            for (Technology technology : deviceResource.getTechnologies()) {
                Set<Long> devices = deviceResourcesByTechnology.get(technology);
                if (devices != null) {
                    devices.remove(resourceId);
                }
            }
        }
    }
}
