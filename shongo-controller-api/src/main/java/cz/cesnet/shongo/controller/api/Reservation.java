package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;
import org.joda.time.Interval;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Represents an allocation.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Reservation extends IdentifiedComplexType
{
    /**
     * Reservation request for which is {@link Reservation} allocated.
     */
    private String reservationRequestId;

    /**
     * Slot for which the {@link Reservation} is allocated.
     */
    private Interval slot;

    /**
     * Parent {@link Reservation} shongo-id.
     */
    private String parentReservationId;

    /**
     * Child {@link Reservation} shongo-ids.
     */
    private List<String> childReservationIds = new LinkedList<String>();

    /**
     * @see Executable
     */
    private Executable executable;

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
     * Sort {@link #childReservationIds}.
     */
    public void sortChildReservationIds()
    {
        Collections.sort(childReservationIds);
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


    private static final String RESERVATION_REQUEST_ID = "reservationRequestId";
    private static final String SLOT = "slot";
    private static final String PARENT_RESERVATION_ID = "parentReservationId";
    private static final String CHILD_RESERVATION_IDS = "childReservationIds";
    private static final String EXECUTABLE = "executable";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RESERVATION_REQUEST_ID, reservationRequestId);
        dataMap.set(SLOT, slot);
        dataMap.set(PARENT_RESERVATION_ID, parentReservationId);
        dataMap.set(CHILD_RESERVATION_IDS, childReservationIds);
        dataMap.set(EXECUTABLE, executable);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        reservationRequestId = dataMap.getString(RESERVATION_REQUEST_ID);
        slot = dataMap.getInterval(SLOT);
        parentReservationId = dataMap.getString(PARENT_RESERVATION_ID);
        childReservationIds = dataMap.getList(CHILD_RESERVATION_IDS, String.class);
        executable = dataMap.getComplexType(EXECUTABLE, Executable.class);
    }
}
