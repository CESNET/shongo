package cz.cesnet.shongo.controller.api;

import org.joda.time.Interval;

/**
 * Represents summary of an allocated compartment.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CompartmentSummary
{
    /**
     * Identifier of the {@link Compartment}.
     */
    private String identifier;

    /**
     * Slot of the {@link cz.cesnet.shongo.controller.api.CompartmentSummary}.
     */
    private Interval slot;

    /**
     * Current state of the {@link cz.cesnet.shongo.controller.api.CompartmentSummary}.
     */
    private Compartment.State state;

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
     * @return {@link #state}
     */
    public Compartment.State getState()
    {
        return state;
    }

    /**
     * @param state sets the {@link #state}
     */
    public void setState(Compartment.State state)
    {
        this.state = state;
    }
}
