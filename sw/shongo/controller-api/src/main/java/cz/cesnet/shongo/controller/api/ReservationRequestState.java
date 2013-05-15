package cz.cesnet.shongo.controller.api;

/**
 * State of reservation request.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum ReservationRequestState
{
    /**
     * Reservation request is not completely filled.
     */
    NOT_COMPLETE,

    /**
     * None reservation has been allocated for the request (but the request is complete).
     */
    NOT_ALLOCATED,

    /**
     * The earliest reservation has been allocated for the request.
     */
    ALLOCATED,

    /**
     * The earliest reservation has failed to be allocated for the request.
     */
    ALLOCATION_FAILED,

    /**
     * The earliest reservation has been started.
     */
    STARTED,

    /**
     * The earliest reservation failed to start.
     */
    STARTING_FAILED,

    /**
     * The earliest reservation has been finished.
     */
    FINISHED
}
