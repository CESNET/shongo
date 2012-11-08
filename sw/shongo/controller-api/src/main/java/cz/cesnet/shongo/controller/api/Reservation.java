package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.util.IdentifiedObject;
import cz.cesnet.shongo.api.xmlrpc.StructType;
import org.joda.time.Interval;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an allocation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Reservation extends IdentifiedObject implements StructType
{
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
     * @see {@link Executable}
     */
    private Executable executable;

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
     * @return {@link #executable}
     */
    public Executable getExecutable()
    {
        return executable;
    }

    /**
     * @param executable sets the {@link #executable}
     */
    public void setExecutable(Executable executable)
    {
        this.executable = executable;
    }
}
