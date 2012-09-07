package cz.cesnet.shongo.controller.api;

import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an information about allocations of a resource.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ResourceAllocation
{
    /**
     * Identifier of the resource.
     */
    private String identifier;

    /**
     * Name of the resource.
     */
    private String name;

    /**
     * Interval for the allocation information.
     */
    private Interval interval;

    /**
     * Allocations of the resource.
     */
    private List<AllocatedItem> allocations = new ArrayList<AllocatedItem>();

    /**
     * @return {@link #identifier}
     */
    public String getIdentifier()
    {
        return identifier;
    }

    /**
     * @param identifier {@link #identifier}
     */
    public void setIdentifier(String identifier)
    {
        this.identifier = identifier;
    }

    /**
     * @return {@link #name}
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name sets the {@link #name}
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return {@link #interval}
     */
    public Interval getInterval()
    {
        return interval;
    }

    /**
     * @param interval sets the {@link #interval}
     */
    public void setInterval(Interval interval)
    {
        this.interval = interval;
    }

    /**
     * @return {@link #allocations}
     */
    public List<AllocatedItem> getAllocations()
    {
        return allocations;
    }

    /**
     * @param allocations sets the {@link #allocations}
     */
    public void setAllocations(List<AllocatedItem> allocations)
    {
        this.allocations = allocations;
    }

    /**
     * @param allocation to be added to the {@link #allocations}
     */
    public void addAllocation(AllocatedItem allocation)
    {
        allocations.add(allocation);
    }
}
