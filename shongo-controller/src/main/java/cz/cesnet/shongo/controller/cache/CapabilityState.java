package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.controller.booking.resource.Capability;
import cz.cesnet.shongo.controller.booking.resource.Resource;

import java.util.*;

/**
 * Current state of a single {@link Capability} type for all resources which have it.
 */
public class CapabilityState
{
    /**
     * Type of {@link Capability} for which the state is managed.
     */
    protected Class<? extends Capability> capabilityType;

    /**
     * Map of instances of {@link #capabilityType} by the resource ids which have them.
     */
    protected Map<Long, Collection<Capability>> capabilityByResourceId =
            new LinkedHashMap<Long, Collection<Capability>>();

    /**
     * Ordered list of capabilities.
     */
    private List<Capability> tmpCapabilities;

    /**
     * Ordered list of resource-ids.
     */
    private Set<Long> tmpResourceIds;

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
     * @return collection of capabilities
     */
    public synchronized List<Capability> getCapabilities()
    {
        if (tmpCapabilities == null) {
            tmpCapabilities = new LinkedList<Capability>();
            for (Collection<Capability> capabilities : capabilityByResourceId.values()) {
                for (Capability capability : capabilities) {
                    tmpCapabilities.add(capability);
                }
            }
            Collections.sort(tmpCapabilities, new Comparator<Capability>()
            {
                @Override
                public int compare(Capability capability1, Capability capability2)
                {
                    Resource resource1 = capability1.getResource();
                    Resource resource2 = capability2.getResource();
                    // Allocation order
                    Integer allocationOrder1 = resource1.getAllocationOrder();
                    Integer allocationOrder2 = resource2.getAllocationOrder();
                    if (allocationOrder1 != null && allocationOrder2 != null) {
                        return allocationOrder1.compareTo(allocationOrder2);
                    }
                    else if (allocationOrder1 != null) {
                        return -1;
                    }
                    else if (allocationOrder2 != null) {
                        return 1;
                    }
                    // Identifiers
                    Long id1 = resource1.getId();
                    Long id2 = resource2.getId();
                    if (id1 != null && id2 != null) {
                        return id1.compareTo(id2);
                    }
                    return 0;
                }
            });
        }
        return tmpCapabilities;
    }

    /**
     * @return set of resource ids which have capability of {@link #capabilityType}
     */
    public synchronized Set<Long> getResourceIds()
    {
        if (tmpResourceIds == null) {
            tmpResourceIds = new LinkedHashSet<Long>();
            for (Capability capability : getCapabilities()) {
                Resource resource = capability.getResource();
                tmpResourceIds.add(resource.getId());
            }
        }
        return tmpResourceIds;
    }

    /**
     * @param capability to be added to the {@link CapabilityState}
     */
    public synchronized void addCapability(Capability capability)
    {
        if (!capabilityType.isInstance(capability)) {
            throw new IllegalArgumentException("Capability '" + capability.getClass().getSimpleName()
                    + "'is not instance of '" + capabilityType.getSimpleName() + "'.");
        }

        Resource resource = capability.getResource();
        Long resourceId = resource.getId();

        // Add the capability by it's resource to the map
        Collection<Capability> capabilities = capabilityByResourceId.get(resourceId);
        if (capabilities == null) {
            capabilities = new LinkedList<Capability>();
            capabilityByResourceId.put(resourceId, capabilities);
        }
        capabilities.add(capability);

        // Clear order
        tmpCapabilities = null;
        tmpResourceIds = null;
    }

    /**
     * @param resource for which the capability should be removed from the {@link CapabilityState}
     */
    public synchronized void removeCapability(Resource resource)
    {
        Long resourceId = resource.getId();

        // Remove the device resource from the set of virtual room resources
        capabilityByResourceId.remove(resourceId);

        // Clear order
        tmpCapabilities = null;
        tmpResourceIds = null;
    }
}
