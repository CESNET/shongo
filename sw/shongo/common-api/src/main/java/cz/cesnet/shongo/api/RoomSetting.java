package cz.cesnet.shongo.api;

import cz.cesnet.shongo.api.util.IdentifiedChangeableObject;

import java.util.List;

/**
 * Represents a setting for a virtual room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class RoomSetting extends IdentifiedChangeableObject
{
    /**
     * Represents a {@link RoomSetting} for a {@link cz.cesnet.shongo.Technology#H323}.
     */
    public static class H323 extends RoomSetting
    {
        /**
         * The PIN that must be entered to get to the room.
         */
        public static final String PIN = "pin";

        /**
         * A boolean option whether to list the room in public lists. Defaults to false.
         */
        public static final String LISTED_PUBLICLY = "listedPublicly";

        /**
         *  A boolean option whether participants may contribute content. Defaults to true.
         */
        public static final String ALLOW_CONTENT = "allowContent";

        /**
         * A boolean option whether guests should be allowed to join. Defaults to true.
         */
        public static final String ALLOW_GUESTS = "allowGuests";

        /**
         * A boolean option whether audio should be muted on join. Defaults to false.
         */
        public static final String JOIN_AUDIO_MUTED = "joinAudioMuted";

        /**
         * A boolean option whether video should be muted on join. Defaults to false.
         */
        public static final String JOIN_VIDEO_MUTED = "joinVideoMuted";

        /**
         * A boolean option whether to register the aliases with the gatekeeper. Defaults to false.
         */
        public static final String REGISTER_WITH_GATEKEEPER = "registerWithGatekeeper";

        /**
         * A boolean option whether to register the aliases with the SIP registrar. Defaults to false.
         */
        public static final String REGISTER_WITH_REGISTRAR = "registerWithRegistrar";

        /**
         * A boolean option whether the room should be locked when started. Defaults to false.
         */
        public static final String START_LOCKED = "startLocked";

        /**
         * A boolean option whether the ConferenceMe should be enabled for the room. Defaults to false.
         */
        public static final String CONFERENCE_ME_ENABLED = "conferenceMeEnabled";

        /**
         * @param pin sets the {@link #PIN}
         * @return this {@link H323}
         */
        public H323 withPin(String pin)
        {
            setPin(pin);
            return this;
        }

        /**
         * @return {@link #PIN}
         */
        public String getPin()
        {
            return getPropertyStorage().getValue(PIN);
        }

        /**
         * @param pin sets the {@link #PIN}
         */
        public void setPin(String pin)
        {
            getPropertyStorage().setValue(PIN, pin);
        }

        /**
         * @return {@link #LISTED_PUBLICLY}
         */
        public Boolean getListedPublicly()
        {
            return getPropertyStorage().getValueAsBoolean(LISTED_PUBLICLY);
        }

        /**
         * @param listedPublicly sets the {@link #LISTED_PUBLICLY}
         */
        public void setListedPublicly(Boolean listedPublicly)
        {
            getPropertyStorage().setValue(LISTED_PUBLICLY, listedPublicly);
        }

        /**
         * @return {@link #ALLOW_CONTENT}
         */
        public Boolean getAllowContent()
        {
            return getPropertyStorage().getValueAsBoolean(ALLOW_CONTENT);
        }

        /**
         * @param allowContent sets the {@link #ALLOW_CONTENT}
         */
        public void setAllowContent(Boolean allowContent)
        {
            getPropertyStorage().setValue(ALLOW_CONTENT, allowContent);
        }

        /**
         * @return {@link #ALLOW_GUESTS}
         */
        public Boolean getAllowGuests()
        {
            return getPropertyStorage().getValueAsBoolean(ALLOW_GUESTS);
        }

        /**
         * @param allowGuests sets the {@link #ALLOW_GUESTS}
         */
        public void setAllowGuests(Boolean allowGuests)
        {
            getPropertyStorage().setValue(ALLOW_GUESTS, allowGuests);
        }

        /**
         * @return {@link #JOIN_AUDIO_MUTED}
         */
        public Boolean getJoinAudioMuted()
        {
            return getPropertyStorage().getValueAsBoolean(JOIN_AUDIO_MUTED);
        }

        /**
         * @param joinAudioMuted sets the {@link #JOIN_AUDIO_MUTED}
         */
        public void setJoinAudioMuted(Boolean joinAudioMuted)
        {
            getPropertyStorage().setValue(JOIN_AUDIO_MUTED, joinAudioMuted);
        }

        /**
         * @return {@link #JOIN_VIDEO_MUTED}
         */
        public Boolean getJoinVideoMuted()
        {
            return getPropertyStorage().getValueAsBoolean(JOIN_VIDEO_MUTED);
        }

        /**
         * @param joinVideoMuted sets the {@link #JOIN_VIDEO_MUTED}
         */
        public void setJoinVideoMuted(Boolean joinVideoMuted)
        {
            getPropertyStorage().setValue(JOIN_VIDEO_MUTED, joinVideoMuted);
        }

        /**
         * @return {@link #REGISTER_WITH_GATEKEEPER}
         */
        public Boolean getRegisterWithGatekeeper()
        {
            return getPropertyStorage().getValueAsBoolean(REGISTER_WITH_GATEKEEPER);
        }

        /**
         * @param registerWithGatekeeper sets the {@link #REGISTER_WITH_GATEKEEPER}
         */
        public void setRegisterWithGatekeeper(Boolean registerWithGatekeeper)
        {
            getPropertyStorage().setValue(REGISTER_WITH_GATEKEEPER, registerWithGatekeeper);
        }

        /**
         * @return {@link #REGISTER_WITH_REGISTRAR}
         */
        public Boolean getRegisterWithRegistrar()
        {
            return getPropertyStorage().getValueAsBoolean(REGISTER_WITH_REGISTRAR);
        }

        /**
         * @param registerWithRegistrar sets the {@link #REGISTER_WITH_REGISTRAR}
         */
        public void setRegisterWithRegistrar(Boolean registerWithRegistrar)
        {
            getPropertyStorage().setValue(REGISTER_WITH_REGISTRAR, registerWithRegistrar);
        }

        /**
         * @return {@link #START_LOCKED}
         */
        public Boolean getStartLocked()
        {
            return getPropertyStorage().getValueAsBoolean(START_LOCKED);
        }

        /**
         * @param startLocked sets the {@link #START_LOCKED}
         */
        public void setStartLocked(Boolean startLocked)
        {
            getPropertyStorage().setValue(START_LOCKED, startLocked);
        }

        /**
         * @return {@link #CONFERENCE_ME_ENABLED}
         */
        public Boolean getConferenceMeEnabled()
        {
            return getPropertyStorage().getValueAsBoolean(CONFERENCE_ME_ENABLED);
        }

        /**
         * @param conferenceMeEnabled sets the {@link #CONFERENCE_ME_ENABLED}
         */
        public void setConferenceMeEnabled(Boolean conferenceMeEnabled)
        {
            getPropertyStorage().setValue(CONFERENCE_ME_ENABLED, conferenceMeEnabled);
        }
    }

    /**
     * Represents a {@link RoomSetting} for a {@link cz.cesnet.shongo.Technology#ADOBE_CONNECT}.
     */
    public static class AdobeConnect extends RoomSetting
    {
    }
}
