package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.AllowedTypes;
import cz.cesnet.shongo.api.annotation.ReadOnly;
import cz.cesnet.shongo.api.annotation.Required;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import java.util.ArrayList;
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
    public static final String SLOTS = "slots";

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
    @Required
    @AllowedTypes({Interval.class, PeriodicDateTimeSlot.class})
    public List<Object> getSlots()
    {
        return getPropertyStorage().getCollection(SLOTS, List.class);
    }

    /**
     * @param slots sets the {@link #SLOTS}
     */
    public void setSlots(List<Object> slots)
    {
        getPropertyStorage().setCollection(SLOTS, slots);
    }

    /**
     * Add new slot to the {@link #SLOTS}.
     *
     * @param slot
     */
    public void addSlot(Object slot)
    {
        getPropertyStorage().addCollectionItem(SLOTS, slot, List.class);
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
        getPropertyStorage().removeCollectionItem(SLOTS, dateTimeSlot);
    }

    /**
     * @return {@link #reservationRequests}
     */
    @ReadOnly
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
}
