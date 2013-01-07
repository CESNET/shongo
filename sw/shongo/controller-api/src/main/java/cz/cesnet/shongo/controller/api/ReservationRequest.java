package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.Required;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

/**
 * Request for reservation of resources.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequest extends NormalReservationRequest
{
    /**
     * {@link DateTimeSlot} for which the reservation is requested.
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
     * Allocated {@link Reservation} shongo-id.
     */
    private String reservationId;

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
     * @param dateTime sets the date/time from the {@link #SLOT}
     * @param duration sets the duration from the {@link #SLOT}
     */
    public void setSlot(DateTime dateTime, Period duration)
    {
        setSlot(new Interval(dateTime, duration));
    }

    /**
     * @param dateTime sets the date/time from the {@link #SLOT}
     * @param duration sets the duration from the {@link #SLOT}
     */
    public void setSlot(String dateTime, String duration)
    {
        setSlot(new Interval(DateTime.parse(dateTime), Period.parse(duration)));
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
     * @return {@link #reservationId}
     */
    public String getReservationId()
    {
        return reservationId;
    }

    /**
     * @param reservationId sets the {@link #reservationId}
     */
    public void setReservationId(String reservationId)
    {
        this.reservationId = reservationId;
    }
}
