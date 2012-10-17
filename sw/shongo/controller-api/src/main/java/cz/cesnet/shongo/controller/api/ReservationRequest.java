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
public class ReservationRequest extends AbstractReservationRequest
{
    /**
     * {@link DateTimeSlot} for which the reservation is requested.
     */
    public static final String SLOT = "slot";

    /**
     * {@link Specification} which is requested for the reservation.
     */
    public static final String SPECIFICATION = "specification";

    /**
     * State of processed slot.
     */
    private State state;

    /**
     * Description of state.
     */
    private String stateReport;

    /**
     * Allocated {@link Reservation} identifier.
     */
    private String reservationIdentifier;

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
    public void setSlot(String dateTime, String duration)
    {
        setSlot(new Interval(DateTime.parse(dateTime), Period.parse(duration)));
    }

    /**
     * @return {@link #SPECIFICATION}
     */
    @Required
    public Specification getSpecification()
    {
        return getPropertyStorage().getValue(SPECIFICATION);
    }

    /**
     * @param specification sets the {@link #SPECIFICATION}
     */
    public void setSpecification(Specification specification)
    {
        getPropertyStorage().setValue(SPECIFICATION, specification);
    }

    /**
     * @return {@link #state}
     */
    public State getState()
    {
        return state;
    }

    /**
     * @param state sets the {@link #state}
     */
    public void setState(State state)
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
     * @return {@link #reservationIdentifier}
     */
    public String getReservationIdentifier()
    {
        return reservationIdentifier;
    }

    /**
     * @param reservationIdentifier sets the {@link #reservationIdentifier}
     */
    public void setReservationIdentifier(String reservationIdentifier)
    {
        this.reservationIdentifier = reservationIdentifier;
    }

    /**
     * State of the {@link ReservationRequest}.
     */
    public static enum State
    {
        NOT_COMPLETE,
        COMPLETE,
        ALLOCATED,
        ALLOCATION_FAILED
    }
}
