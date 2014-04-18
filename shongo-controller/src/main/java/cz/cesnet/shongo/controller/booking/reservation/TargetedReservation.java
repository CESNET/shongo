package cz.cesnet.shongo.controller.booking.reservation;

/**
 * Represents an {@link Reservation} for a target which can be identified by {@link #getTargetId()}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class TargetedReservation extends Reservation
{
    /**
     * @return identifier of a target
     */
    public abstract Long getTargetId();
}
