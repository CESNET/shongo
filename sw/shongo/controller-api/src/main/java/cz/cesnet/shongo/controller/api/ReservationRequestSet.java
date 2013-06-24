package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
    private List<Object> slots = new LinkedList<Object>();

    /**
     * List of {@link ReservationRequest} which have been already created for the {@link ReservationRequestSet}.
     */
    private List<ReservationRequest> reservationRequests = new ArrayList<ReservationRequest>();

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

    /**
     * @return {@link #reservationRequests}
     */
    public List<ReservationRequest> getReservationRequests()
    {
        return reservationRequests;
    }

    /**
     * @param request slot to be added to the {@link #reservationRequests}
     */
    public void addReservationRequest(ReservationRequest request)
    {
        reservationRequests.add(request);
    }

    public static final String SLOTS = "slots";
    public static final String RESERVATION_REQUESTS = "reservationRequests";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(SLOTS, slots);
        dataMap.set(RESERVATION_REQUESTS, reservationRequests);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        slots = dataMap.getListRequired(SLOTS, Interval.class, PeriodicDateTimeSlot.class);
        reservationRequests = dataMap.getList(RESERVATION_REQUESTS, ReservationRequest.class);
    }
}
