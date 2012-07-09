package cz.cesnet.shongo.connector;

/**
 * Layout of a virtual room.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public enum RoomLayout
{
    /**
     * Only a single, fixed participant is displayed.
     */
    SINGLE_PARTICIPANT,

    /**
     * Only a single, currently speaking participant is viewed.
     */
    VOICE_SWITCHED_SINGLE_PARTICIPANT,

    /**
     * A fixed participant is in the upper-left corner, other participants around.
     */
    SPEAKER_CORNER,

    /**
     * The currently speaking participant is in the upper-left corner, other participants around.
     */
    VOICE_SWITCHED_SPEAKER_CORNER,

    /**
     * All participants are spread in a regular grid.
     */
    GRID,
}
