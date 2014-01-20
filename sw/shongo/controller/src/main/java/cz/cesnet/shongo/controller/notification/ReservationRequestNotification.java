package cz.cesnet.shongo.controller.notification;

import cz.cesnet.shongo.controller.booking.request.AbstractReservationRequest;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 * Represents a {@link Notification} for an {@link AbstractReservationRequest}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class ReservationRequestNotification extends Notification
{
    /**
     * {@link AbstractReservationRequest} for which the {@link ReservationRequestNotification} is created.
     */
    private AbstractReservationRequest reservationRequest;

    /**
     * @return {@link #reservationRequest}
     */
    @ManyToOne
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
}
