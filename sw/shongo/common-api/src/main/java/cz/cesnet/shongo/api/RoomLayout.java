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
     * Only a single, fixed participant is displayed.
     */
    SINGLE_PARTICIPANT(VoiceSwitching.NOT_VOICE_SWITCHED),

    /**
     * Only a single, currently speaking participant is viewed.
     */
    VOICE_SWITCHED_SINGLE_PARTICIPANT(VoiceSwitching.VOICE_SWITCHED),

    /**
     * A fixed participant is in the upper-left corner, other participants around.
     */
    SPEAKER_CORNER(VoiceSwitching.NOT_VOICE_SWITCHED),

    /**
     * The currently speaking participant is in the upper-left corner, other participants around.
     */
    VOICE_SWITCHED_SPEAKER_CORNER(VoiceSwitching.VOICE_SWITCHED),

    /**
     * All participants are spread in a regular grid.
     */
    GRID(VoiceSwitching.NOT_VOICE_SWITCHED),;

    private VoiceSwitching voiceSwitching;

    private RoomLayout(VoiceSwitching voiceSwitching)
    {
        this.voiceSwitching = voiceSwitching;
    }

    /**
     * @return voice-switching mode of this layout
     */
    public VoiceSwitching getVoiceSwitching()
    {
        return voiceSwitching;
    }


    /**
     * Gets a room layout based on the Cisco layout index.
     *
     * @param layoutIndex   index of the layout as defined by Cisco
     * @param defaultLayout default layout to use when not recognized
     * @param voiceSwitched whether the layout should be voice-switched
     * @return room layout according to the given layout index
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
