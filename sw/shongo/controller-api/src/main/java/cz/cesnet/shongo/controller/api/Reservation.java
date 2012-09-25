package cz.cesnet.shongo.controller.api;

import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an allocation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Reservation
{
    /**
     * Identifier of the {@link Reservation}.
     */
    private String identifier;

    /**
     * Slot fot which the {@link Reservation} is allocated.
     */
    private Interval slot;

    /**
     * Parent {@link Reservation} identifier.
     */
    private String parentReservationIdentifier;

    /**
     * Child {@link Reservation} identifiers.
     */
    private List<String> childReservationIdentifiers = new ArrayList<String>();

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
     * @return {@link #parentReservationIdentifier}
     */
    public String getParentReservationIdentifier()
    {
        return parentReservationIdentifier;
    }

    /**
     * @param parentReservationIdentifier sets the {@link #parentReservationIdentifier}
     */
    public void setParentReservationIdentifier(String parentReservationIdentifier)
    {
        this.parentReservationIdentifier = parentReservationIdentifier;
    }

    /**
     * @return {@link #childReservationIdentifiers}
     */
    public List<String> getChildReservationIdentifiers()
    {
        return childReservationIdentifiers;
    }

    /**
     * @param childReservationIdentifiers sets the {@link #childReservationIdentifiers}
     */
    public void setChildReservationIdentifiers(List<String> childReservationIdentifiers)
    {
        this.childReservationIdentifiers = childReservationIdentifiers;
    }

    /**
     * @param childReservationIdentifier to be added to the {@link #childReservationIdentifiers}
     */
    public void addChildReservationIdentifier(String childReservationIdentifier)
    {
        childReservationIdentifiers.add(childReservationIdentifier);
    }

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
}
