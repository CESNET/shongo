package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.Required;
import cz.cesnet.shongo.api.util.Converter;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import java.util.LinkedList;
import java.util.List;

/**
 * Request for reservation of resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequest extends AbstractReservationRequest
{
    /**
     * Date/time slot for which the reservation is requested.
     */
    public static final String SLOT = "slot";

    /**
     * State of the request.
     */
    private ReservationRequestState state;

    /**
     * Description of state.
     */
    private String stateReport;

    /**
     * Allocated {@link Reservation}s.
     */
    private List<String> reservationIds = new LinkedList<String>();

    /**
     * Constructor.
     */
    public ReservationRequest()
    {
    }

    /**
     * @return {@link #SLOT}
     */
    @Required
    public Interval getSlot()
    {
        return getPropertyStorage().getValue(SLOT);
    }

    /**
     * @param slot sets the {@link #SLOT}
     */
    public void setSlot(Interval slot)
    {
        getPropertyStorage().setValue(SLOT, slot);
    }

    /**
     * @param slot sets the {@link #SLOT}
     */
    public void setSlot(String slot)
    {
        getPropertyStorage().setValue(SLOT, Converter.Atomic.convertStringToInterval(slot));
    }

    /**
     * @param start sets the starting date/time from the {@link #SLOT}
     * @param end   sets the ending date/time from the {@link #SLOT}
     */
    public void setSlot(DateTime start, DateTime end)
    {
        setSlot(new Interval(start, end));
    }

    /**
     * @param dateTime sets the date/time from the {@link #SLOT}
     * @param duration sets the duration from the {@link #SLOT}
     */
    public void setSlot(DateTime dateTime, Period duration)
    {
        setSlot(new Interval(dateTime, duration));
    }

    /**
     * @param startDateTime         sets the starting date/time for the {@link #SLOT}
     * @param endDateTimeOrDuration sets the ending date/time or duration for the {@link #SLOT}
     */
    public void setSlot(String startDateTime, String endDateTimeOrDuration)
    {
        Interval interval;
        try {
            interval = new Interval(DateTime.parse(startDateTime), DateTime.parse(endDateTimeOrDuration));
        }
        catch (IllegalArgumentException exception) {
            interval = new Interval(DateTime.parse(startDateTime), Period.parse(endDateTimeOrDuration));
        }
        setSlot(interval);
    }

    /**
     * @return {@link #state}
     */
    public ReservationRequestState getState()
    {
        return state;
    }

    /**
     * @param state sets the {@link #state}
     */
    public void setState(ReservationRequestState state)
    {
        this.state = state;
    }

    /**
     * @return {@link #stateReport}
     */
    public String getStateReport()
    {
        return stateReport;
    }

    /**
     * @param stateReport sets the {@link #stateReport}
     */
    public void setStateReport(String stateReport)
    {
        this.stateReport = stateReport;
    }

    /**
     * @return {@link #reservationIds}
     */
    public List<String> getReservationIds()
    {
        return reservationIds;
    }

    /**
     * @param reservationIds sets the {@link #reservationIds}
     */
    public void setReservationIds(List<String> reservationIds)
    {
        this.reservationIds = reservationIds;
    }

    /**
     * @param reservationId to be added to the {@link #reservationIds}
     */
    public void addReservationId(String reservationId)
    {
        this.reservationIds.add(reservationId);
    }
}
