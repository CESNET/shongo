package cz.cesnet.shongo.controller.reservation;

/**
 * Represents enumeration of who will initiate a videoconference call.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum CallInitiation
{
    /**
     * A scheduler can decide who will initiate the call.
     */
    DEFAULT,

    /**
     * Terminal should initiate the call to virtual room.
     */
    TERMINAL,

    /**
     * Virtual room should initiate the call to terminal.
     */
    VIRTUAL_ROOM
}
