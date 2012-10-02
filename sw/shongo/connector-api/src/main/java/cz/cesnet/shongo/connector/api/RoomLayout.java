package cz.cesnet.shongo.connector.api;

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

    ;

    /**
     * Gets a room layout based on the Cisco layout index.
     *
     * @param layoutIndex      index of the layout as defined by Cisco
     * @param defaultLayout    default layout to use when not recognized
     * @param voiceSwitched    whether the layout should be voice-switched
     * @return
     */
    public static RoomLayout getByCiscoId(int layoutIndex, RoomLayout defaultLayout, VoiceSwitching voiceSwitched)
    {
        switch (layoutIndex) {
            case 1:
                if (voiceSwitched == VoiceSwitching.VOICE_SWITCHED) {
                    return VOICE_SWITCHED_SINGLE_PARTICIPANT;
                }
                else {
                    return SINGLE_PARTICIPANT;
                }
            case 2:
            case 3:
            case 4:
            case 8:
            case 9:
                return GRID;
            case 5:
            case 6:
            case 7:
                if (voiceSwitched == VoiceSwitching.VOICE_SWITCHED) {
                    return VOICE_SWITCHED_SPEAKER_CORNER;
                }
                else {
                    return SPEAKER_CORNER;
                }
            default:
                return defaultLayout;
        }
    }

    public enum VoiceSwitching
    {
        VOICE_SWITCHED,
        NOT_VOICE_SWITCHED,
    }
}
