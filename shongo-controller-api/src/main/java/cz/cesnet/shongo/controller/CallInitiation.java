package cz.cesnet.shongo.controller;

/**
 * Represents enumeration of who should initiate a conference call.
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
