package cz.cesnet.shongo.controller.booking.request;

import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.hibernate.PersistentDateTime;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.usertype.UserTypeLegacyBridge;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;

/**
 * Represents a {@link PreprocessorState#PREPROCESSED} state of a {@link AbstractReservationRequest}
 * for a specific interval which can be persisted.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class PreprocessedState extends SimplePersistentObject
{
    /**
     * Reservation request to which the state belongs.
     */
    private AbstractReservationRequest reservationRequest;

    /**
     * Interval start date/time.
     */
    private DateTime start;

    /**
     * Interval end date/time.
     */
    private DateTime end;

    /**
     * @return {@link #reservationRequest}
     */
    @OneToOne
    public AbstractReservationRequest getReservationRequest()
    {
        return reservationRequest;
    }

    /**
     * @param reservationRequest sets the {@link #reservationRequest}
     */
    public void setReservationRequest(AbstractReservationRequest reservationRequest)
    {
        this.reservationRequest = reservationRequest;
    }

    /**
     * @return interval ({@link #start}, {@link #end})
     */
    @Transient
    public Interval getInterval()
    {
        return new Interval(start, end);
    }

    /**
     * @param interval sets the interval ({@link #start}, {@link #end})
     */
    public void setInterval(Interval interval)
    {
        setStart(interval.getStart());
        setEnd(interval.getEnd());
    }

    /**
     * @return {@link #start}
     */
    @Column(name = "interval_start")
    @Type(value = PersistentDateTime.class, parameters = {@Parameter(name = UserTypeLegacyBridge.TYPE_NAME_PARAM_KEY, value = "PersistentDateTime")})
    public DateTime getStart()
    {
        return start;
    }

    /**
     * @param start sets the {@link #start}
     */
    public void setStart(DateTime start)
    {
        this.start = start;
    }

    /**
     * @return {@link #end}
     */
    @Column(name = "interval_end")
    @Type(value = PersistentDateTime.class, parameters = {@Parameter(name = UserTypeLegacyBridge.TYPE_NAME_PARAM_KEY, value = "PersistentDateTime")})
    public DateTime getEnd()
    {
        return end;
    }

    /**
     * @param end sets the {@link #end}
     */
    public void setEnd(DateTime end)
    {
        this.end = end;
    }
}
