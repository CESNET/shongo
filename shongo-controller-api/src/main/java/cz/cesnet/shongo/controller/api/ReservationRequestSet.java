package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;
import org.joda.time.*;

import java.util.*;

/**
 * Request for reservation of resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequestSet extends AbstractReservationRequest
{
    /**
     * Collection of date/time slots for which the reservation is requested.
     */
    private List<Object> slots = new LinkedList<>();

    /**
     * Constructor.
     */
    public ReservationRequestSet()
    {
    }

    /**
     * @return {@link #SLOTS}
     */
    public List<Object> getSlots()
    {
        return slots;
    }

    /**
     * @param slots sets the {@link #SLOTS}
     */
    public void setSlots(List<Object> slots)
    {
        this.slots = slots;
    }

    public void addAllSlots(Collection<PeriodicDateTimeSlot> slots) {
        this.slots.addAll(slots);
    }

    /**
     * Add new slot to the {@link #SLOTS}.
     *
     * @param slot
     */
    public void addSlot(Object slot)
    {
        slots.add(slot);
    }

    /**
     * Add new slot to the {@link #SLOTS}.
     *
     * @param startDateTime
     * @param endDateTimeOrDuration
     */
    public void addSlot(String startDateTime, String endDateTimeOrDuration)
    {
        addSlot(DateTime.parse(startDateTime), endDateTimeOrDuration);
    }

    /**
     * Add new slot to the {@link #SLOTS}.
     *
     * @param startDateTime
     * @param endDateTimeOrDuration
     */
    public void addSlot(DateTime startDateTime, String endDateTimeOrDuration)
    {
        Interval interval;
        try {
            interval = new Interval(startDateTime, DateTime.parse(endDateTimeOrDuration));
        }
        catch (IllegalArgumentException exception) {
            interval = new Interval(startDateTime, Period.parse(endDateTimeOrDuration));
        }
        addSlot(interval);
    }

    /**
     * @param dateTimeSlot slot to be removed from the {@link #SLOTS}
     */
    public void removeSlot(Object dateTimeSlot)
    {
        slots.remove(dateTimeSlot);
    }

    public static final String SLOTS = "slots";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(SLOTS, slots);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        slots = dataMap.getListRequired(SLOTS, Interval.class, PeriodicDateTimeSlot.class);
    }
}
