package cz.cesnet.shongo.controller.resource.database;

import cz.cesnet.shongo.controller.allocation.AllocatedResource;
import cz.cesnet.shongo.controller.resource.Resource;
import cz.cesnet.shongo.controller.util.RangeSet;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Current state of a {@link Resource}.
 */
public class ResourceState
{
    /**
     * Resource identifier.
     */
    private Long resourceId;

    /**
     * Already allocated {@link cz.cesnet.shongo.controller.allocation.AllocatedResource}s for the resource.
     */
    private RangeSet<AllocatedResource, DateTime> allocations = new RangeSet<AllocatedResource, DateTime>();

    /**
     * Map of {@link cz.cesnet.shongo.controller.allocation.AllocatedResource}s by the identifier.
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

    /**
     * @return {@link #resourceId}
     */
    public Long getResourceId()
    {
        return resourceId;
    }

    /**
     * @param interval
     * @return list of {@link AllocatedResource} for managed {@link Resource} in given {@code interval}
     */
    public Set<AllocatedResource> getAllocatedResources(Interval interval)
    {
        return allocations.getValues(interval.getStart(), interval.getEnd());
    }

    /**
     * @param allocatedResource to be added to the {@link ResourceState}
     */
    public void addAllocatedResource(AllocatedResource allocatedResource)
    {
        // TODO: check if allocation doesn't collide

        Interval slot = allocatedResource.getSlot();
        allocationsById.put(allocatedResource.getId(), allocatedResource);
        allocations.add(allocatedResource, slot.getStart(), slot.getEnd());
    }

    /**
     * @param allocatedResourceId to be removed from the {@link ResourceState}
     */
    public void removeAllocatedResource(Long allocatedResourceId)
    {
        AllocatedResource allocatedResource = allocationsById.get(allocatedResourceId);
        if (allocatedResource == null) {
            throw new IllegalStateException("Allocated resource doesn't exist in the resource database.");
        }
        allocations.remove(allocatedResource);
        allocationsById.remove(allocatedResourceId);
    }

    /**
     * Clear all {@link AllocatedResource} from the {@link ResourceState}.
     */
    public void clear()
    {
        allocations.clear();
        allocationsById.clear();
    }
}
