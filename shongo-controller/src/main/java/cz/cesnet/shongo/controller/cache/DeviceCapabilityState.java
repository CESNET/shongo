package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.booking.resource.Capability;
import cz.cesnet.shongo.controller.booking.resource.DeviceCapability;
import cz.cesnet.shongo.controller.booking.resource.DeviceResource;
import cz.cesnet.shongo.controller.booking.resource.Resource;

import java.util.*;

/**
 * Current state of a single {@link cz.cesnet.shongo.controller.booking.resource.Capability} type for all resources which have it.
 */
public class DeviceCapabilityState extends CapabilityState
{
    /**
     * Ordered map of {@link DeviceResource} ids by theirs technology.
     */
    private Map<Technology, Set<Long>> tmpDeviceResourcesByTechnology;

    /**
     * Constructor.
     *
     * @param capabilityType sets the {@link #capabilityType}
     */
    public DeviceCapabilityState(Class<? extends DeviceCapability> capabilityType)
    {
        super(capabilityType);
    }

    /**
     * @param technology
     * @return set of resource ids which supports given {@code technology}
     */
    public synchronized Set<Long> getDeviceResourceIds(Technology technology)
    {
        if (tmpDeviceResourcesByTechnology == null) {
            tmpDeviceResourcesByTechnology = new HashMap<Technology, Set<Long>>();
            for (Capability capability : getCapabilities()) {
                DeviceCapability deviceCapability = (DeviceCapability) capability;
                DeviceResource deviceResource = deviceCapability.getDeviceResource();
                for (Technology deviceResourceTechnology : deviceResource.getTechnologies()) {
                    Set<Long> deviceResources = tmpDeviceResourcesByTechnology.get(deviceResourceTechnology);
                    if (deviceResources == null) {
                        deviceResources = new LinkedHashSet<Long>();
                        tmpDeviceResourcesByTechnology.put(deviceResourceTechnology, deviceResources);
                    }
                    deviceResources.add(deviceResource.getId());
                }
            }
        }
        Set<Long> deviceResourceIds = tmpDeviceResourcesByTechnology.get(technology);
        if (deviceResourceIds != null) {
            return deviceResourceIds;
        }
        else {
            return Collections.emptySet();
        }
    }

    @Override
    public synchronized void addCapability(Capability capability)
    {
        super.addCapability(capability);

        tmpDeviceResourcesByTechnology = null;
    }

    @Override
    public synchronized void removeCapability(Resource resource)
    {
        super.removeCapability(resource);

        tmpDeviceResourcesByTechnology = null;
    }
}
