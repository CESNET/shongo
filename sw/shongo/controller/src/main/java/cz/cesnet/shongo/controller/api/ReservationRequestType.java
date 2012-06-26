package cz.cesnet.shongo.controller.api;

/**
 * Represents a type of a reservation request.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum ReservationRequestType
{
    /**
     * Reservation that can be created by any user.
     */
    NORMAL,

    /**
     * Reservation that can be created only by owner of resources,
     * and the reservation can request only owned resources.
     */
    PERMANENT
}
