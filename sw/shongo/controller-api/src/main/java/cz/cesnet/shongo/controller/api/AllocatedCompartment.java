package cz.cesnet.shongo.controller.api;

import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AllocatedCompartment
{
    private Interval slot;

    private List<AllocatedResource> allocatedResources = new ArrayList<AllocatedResource>();

    public Interval getSlot()
    {
        return slot;
    }

    public void setSlot(Interval slot)
    {
        this.slot = slot;
    }

    public List<AllocatedResource> getAllocatedResources()
    {
        return allocatedResources;
    }

    public void setAllocatedResources(List<AllocatedResource> allocatedResources)
    {
        this.allocatedResources = allocatedResources;
    }

    public void addAllocatedResource(AllocatedResource allocatedResource)
    {
        allocatedResources.add(allocatedResource);
    }

}
