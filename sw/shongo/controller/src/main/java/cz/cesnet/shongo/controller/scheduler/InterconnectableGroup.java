package cz.cesnet.shongo.controller.scheduler;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.controller.request.ExternalEndpointSpecification;
import cz.cesnet.shongo.controller.resource.DeviceCapability;
import cz.cesnet.shongo.controller.resource.DeviceResource;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.resource.TerminalCapability;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a group of elements (devices or ports) which can be interconnected.
 */
public class InterconnectableGroup
{
    /**
     * Technologies which are supported by all elements in the group.
     */
    private Set<Technology> technologies = new HashSet<Technology>();

    /**
     * Number of ports which are requested to interconnect all elements in the group.
     */
    private int portCount = 0;

    /**
     * Set of {@link cz.cesnet.shongo.controller.resource.DeviceResource} elements in the group.
     */
    private Set<DeviceResource> deviceResources = new HashSet<DeviceResource>();

    /**
     * Constructor.
     *
     * @param technologies
     */
    public InterconnectableGroup(Technology[] technologies)
    {
        this.technologies.addAll(Arrays.asList(technologies));
    }

    /**
     * Constructor.
     *
     * @param externalEndpointSpecification
     */
    public InterconnectableGroup(ExternalEndpointSpecification externalEndpointSpecification)
    {
        portCount = externalEndpointSpecification.getCount();
        technologies.addAll(externalEndpointSpecification.getTechnologies());
    }

    /**
     * Constructor.
     *
     * @param resource
     */
    public InterconnectableGroup(Resource resource) throws DeviceResourceIsNotTerminalException
    {
        if (resource instanceof DeviceResource) {
            DeviceResource deviceResource = (DeviceResource) resource;
            if (deviceResource.isTerminal()) {
                portCount = 1;
                technologies.addAll(deviceResource.getCapabilityTechnologies(TerminalCapability.class));
                deviceResources.add(deviceResource);
            }
            else {
                throw new DeviceResourceIsNotTerminalException(resource);
            }
        }
    }

    /**
     * @return {@link #technologies}
     */
    public Set<Technology> getTechnologies()
    {
        return technologies;
    }

    /**
     * @return {@link #portCount}
     */
    public int getPortCount()
    {
        return portCount;
    }

    /**
     * @return {@link #deviceResources}
     */
    public Set<DeviceResource> getDeviceResources()
    {
        return deviceResources;
    }

    /**
     * @param deviceCapabilityType
     * @return number of device resources which have capability of the given {@code deviceCapabilityType}
     */
    public int getDeviceResourcesCount(Class<? extends DeviceCapability> deviceCapabilityType)
    {
        int count = 0;
        for (DeviceResource deviceResource : deviceResources) {
            if (deviceResource.hasCapability(deviceCapabilityType)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Try to merge given {@code interconnectableElements} to this {@link InterconnectableGroup}.
     *
     * @param interconnectableElements
     * @return true if merge was done,
     *         false otherwise
     */
    public boolean merge(InterconnectableGroup interconnectableElements)
    {
        Set<Technology> newTechnologies = new HashSet<Technology>(technologies);
        newTechnologies.retainAll(interconnectableElements.technologies);
        if (newTechnologies.size() == 0) {
            return false;
        }
        technologies = newTechnologies;
        portCount += interconnectableElements.portCount;
        deviceResources.addAll(interconnectableElements.deviceResources);
        return true;
    }

    /**
     * Exception which is thrown when {@link cz.cesnet.shongo.controller.resource.Resource} cannot be added to the {@link Task}
     */
    public static class DeviceResourceIsNotTerminalException extends Exception
    {
        /**
         * Specifies which {@link cz.cesnet.shongo.controller.resource.Resource}.
         */
        private Resource resource;

        /**
         * Constructor.
         *
         * @param resource sets the {@link #resource}
         */
        DeviceResourceIsNotTerminalException(Resource resource)
        {
            this.resource = resource;
        }

        /**
         * @return {@link #resource}
         */
        public Resource getResource()
        {
            return resource;
        }
    }
}
