package cz.cesnet.shongo.controller.api;

import org.joda.time.Interval;

/**
 * Represents an allocated item in {@link AllocatedCompartment}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AllocatedItem
{
    /**
     * Slot fot which the resource is allocated.
     */
    private Interval slot;

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
}
