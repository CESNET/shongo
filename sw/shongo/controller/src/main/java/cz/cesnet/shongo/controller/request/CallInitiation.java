package cz.cesnet.shongo.controller.request;

/**
 * Represents enumeration of who should initiate a video conference call.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum CallInitiation
{
    /**
     * A terminal should initiate the call to a virtual room.
     */
    TERMINAL,

    /**
     * A virtual room should initiate the call to a terminal.
     */
    VIRTUAL_ROOM
}
