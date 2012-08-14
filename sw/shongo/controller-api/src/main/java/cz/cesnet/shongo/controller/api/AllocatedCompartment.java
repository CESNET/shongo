package cz.cesnet.shongo.controller.api;

import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an allocated {@link Compartment} from {@link ReservationRequest} for a single time slot.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AllocatedCompartment
{
    /**
     * Slot fot which the compartment is allocated.
     */
    private Interval slot;

    /**
     * Resource which are allocated for a compartment.
     */
    private List<AllocatedResource> allocatedResources = new ArrayList<AllocatedResource>();

    /**
     * @return {@link #slot}
     */
    public Interval getSlot()
    {
        return slot;
    }

    /**
     * @param slot sets the {@link #slot}
     */
    public void setSlot(Interval slot)
    {
        this.slot = slot;
    }

    /**
     * @return {@link #allocatedResources}
     */
    public List<AllocatedResource> getAllocatedResources()
    {
        return allocatedResources;
    }

    /**
     * @param allocatedResources sets the {@link #allocatedResources}
     */
    public void setAllocatedResources(List<AllocatedResource> allocatedResources)
    {
        this.allocatedResources = allocatedResources;
    }

    /**
     * @param allocatedResource to be added to the {@link #allocatedResources}
     */
    public void addAllocatedResource(AllocatedResource allocatedResource)
    {
        allocatedResources.add(allocatedResource);
    }
}
