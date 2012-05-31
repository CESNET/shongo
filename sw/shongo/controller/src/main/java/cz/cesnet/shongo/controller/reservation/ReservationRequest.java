package cz.cesnet.shongo.controller.reservation;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ReservationRequest
{
    /**
     * Type of reservation.
     */
    public static enum Type
    {
        /**
         * Reservation that can be created by any user.
         */
        DEFAULT,

        /**
         * Reservation that can be created only by owner of resources,
         * and the reservation can request only owned resources.
         */
        PERMANENT
    }
}
