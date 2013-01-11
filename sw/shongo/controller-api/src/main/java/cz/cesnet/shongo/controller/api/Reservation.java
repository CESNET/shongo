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
     * User-id of the owner user.
     */
    private String userId;

    /**
     * Reservation request for which is {@link Reservation} allocated.
     */
    private String reservationRequestId;

    /**
     * Slot fot which the {@link Reservation} is allocated.
     */
    private Interval slot;

    /**
     * Parent {@link Reservation} shongo-id.
     */
    private String parentReservationId;

    /**
     * Child {@link Reservation} shongo-ids.
     */
    private List<String> childReservationIds = new ArrayList<String>();

    /**
     * @see Executable
     */
    private Executable executable;

    /**
     * @return {@link #userId}
     */
    public String getUserId()
    {
        return userId;
    }

    /**
     * @param userId sets the {@link #userId}
     */
    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    /**
     * @return {@link #reservationRequestId}
     */
    public String getReservationRequestId()
    {
        return reservationRequestId;
    }

    /**
     * @param reservationRequestId sets the {@link #reservationRequestId}
     */
    public void setReservationRequestId(String reservationRequestId)
    {
        this.reservationRequestId = reservationRequestId;
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
     * @return {@link #parentReservationId}
     */
    public String getParentReservationId()
    {
        return parentReservationId;
    }

    /**
     * @param parentReservationId sets the {@link #parentReservationId}
     */
    public void setParentReservationId(String parentReservationId)
    {
        this.parentReservationId = parentReservationId;
    }

    /**
     * @return {@link #childReservationIds}
     */
    public List<String> getChildReservationIds()
    {
        return childReservationIds;
    }

    /**
     * @param childReservationIds sets the {@link #childReservationIds}
     */
    public void setChildReservationIds(List<String> childReservationIds)
    {
        this.childReservationIds = childReservationIds;
    }

    /**
     * @param childReservationId to be added to the {@link #childReservationIds}
     */
    public void addChildReservationId(String childReservationId)
    {
        childReservationIds.add(childReservationId);
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
