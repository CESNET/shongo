package cz.cesnet.shongo.controller.request;

/**
 * Represents enumeration of who should initiate a video conference call.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public enum CallInitiation
{
    /**
     * Terminal should initiate the call to virtual room.
     */
    TERMINAL,

    /**
     * Virtual room should initiate the call to terminal.
     */
    VIRTUAL_ROOM
}
