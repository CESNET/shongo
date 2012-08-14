package cz.cesnet.shongo.controller.api;

import org.joda.time.Interval;

/**
 * Represents an allocation of a {@link Resource} for a {@link Compartment} in the {@link ReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AllocatedResource
{
    /**
     * Unique identifier of the resource.
     */
    private String identifier;

    /**
     * Name of the resource.
     */
    private String name;

    /**
     * Slot fot which the resource is allocated.
     */
    private Interval slot;

    /**
     * @return {@link #identifier}
     */
    public String getIdentifier()
    {
        return identifier;
    }

    /**
     * @param identifier sets the {@link #identifier}
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
