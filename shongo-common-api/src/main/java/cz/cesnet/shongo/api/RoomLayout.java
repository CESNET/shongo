package cz.cesnet.shongo.api;

import jade.content.Concept;

/**
 * Layout of a virtual room.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public enum RoomLayout implements Concept
{
    /**
     * Other
     */
    OTHER,

    /**
     * Only a single, fixed participant is displayed.
     */
    SPEAKER,

    /**
     * A fixed participant is in the upper-left corner, other participants around.
     */
    SPEAKER_CORNER,

    /**
     * All participants are spread in a regular grid.
     */
    GRID
}
