package cz.cesnet.shongo.api;

/**
 * Represents a {@link RoomSetting} for a {@link cz.cesnet.shongo.Technology#H323}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class H323RoomSetting extends RoomSetting
{
    /**
     * The PIN that must be entered to get to the room.
     */
    private String pin;

    /**
     * A boolean option whether to list the room in public lists. Defaults to false.
     */
    private Boolean listedPublicly;

    /**
     * A boolean option whether participants may contribute content. Defaults to true.
     */
    private Boolean allowContent;

    /**
     * A boolean option whether guests should be allowed to join. Defaults to true.
     */
    private Boolean allowGuests;

    /**
     * A boolean option whether audio should be muted on join. Defaults to false.
     */
    private Boolean joinMicrophoneDisabled;

    /**
     * A boolean option whether video should be muted on join. Defaults to false.
     */
    private Boolean joinVideoDisabled;


    /**
     * A boolean option whether to register the aliases with the gatekeeper. Defaults to false.
     */
    private Boolean registerWithGatekeeper;

    /**
     * A boolean option whether to register the aliases with the SIP registrar. Defaults to false.
     */
    private Boolean registerWithRegistrar;

    /**
     * A boolean option whether the room should be locked when started. Defaults to false.
     */
    private Boolean startLocked;

    /**
     * A boolean option whether the ConferenceMe should be enabled for the room. Defaults to false.
     */
    private Boolean conferenceMeEnabled;

    /**
     * Is room content important (shown in video channel).
     */
    private Boolean contentImportant;

    /**
     * @param pin sets the {@link #pin}
     * @return this {@link H323RoomSetting}
     */
    public H323RoomSetting withPin(String pin)
    {
        setPin(pin);
        return this;
    }

    /**
     * @return {@link #pin}
     */
    public String getPin()
    {
        return pin;
    }

    /**
     * @param pin sets the {@link #pin}
     */
    public void setPin(String pin)
    {
        this.pin = pin;
    }

    /**
     * @return {@link #listedPublicly}
     */
    public Boolean getListedPublicly()
    {
        return listedPublicly;
    }

    /**
     * @param listedPublicly sets the {@link #listedPublicly}
     */
    public void setListedPublicly(Boolean listedPublicly)
    {
        this.listedPublicly = listedPublicly;
    }

    /**
     * @return {@link #allowContent}
     */
    public Boolean getAllowContent()
    {
        return allowContent;
    }

    /**
     * @param allowContent sets the {@link #allowContent}
     */
    public void setAllowContent(Boolean allowContent)
    {
        this.allowContent = allowContent;
    }

    /**
     * @return {@link #allowGuests}
     */
    public Boolean getAllowGuests()
    {
        return allowGuests;
    }

    /**
     * @param allowGuests sets the {@link #allowGuests}
     */
    public void setAllowGuests(Boolean allowGuests)
    {
        this.allowGuests = allowGuests;
    }

    /**
     * @return {@link #joinMicrophoneDisabled}
     */
    public Boolean getJoinMicrophoneDisabled()
    {
        return joinMicrophoneDisabled;
    }

    /**
     * @param joinMicrophoneDisabled sets the {@link #joinMicrophoneDisabled}
     */
    public void setJoinMicrophoneDisabled(Boolean joinMicrophoneDisabled)
    {
        this.joinMicrophoneDisabled = joinMicrophoneDisabled;
    }

    /**
     * @return {@link #joinVideoDisabled}
     */
    public Boolean getJoinVideoDisabled()
    {
        return joinVideoDisabled;
    }

    /**
     * @param joinVideoDisabled sets the {@link #joinVideoDisabled}
     */
    public void setJoinVideoDisabled(Boolean joinVideoDisabled)
    {
        this.joinVideoDisabled = joinVideoDisabled;
    }

    /**
     * @return {@link #registerWithGatekeeper}
     */
    public Boolean getRegisterWithGatekeeper()
    {
        return registerWithGatekeeper;
    }

    /**
     * @param registerWithGatekeeper sets the {@link #registerWithGatekeeper}
     */
    public void setRegisterWithGatekeeper(Boolean registerWithGatekeeper)
    {
        this.registerWithGatekeeper = registerWithGatekeeper;
    }

    /**
     * @return {@link #registerWithRegistrar}
     */
    public Boolean getRegisterWithRegistrar()
    {
        return registerWithRegistrar;
    }

    /**
     * @param registerWithRegistrar sets the {@link #registerWithRegistrar}
     */
    public void setRegisterWithRegistrar(Boolean registerWithRegistrar)
    {
        this.registerWithRegistrar = registerWithRegistrar;
    }

    /**
     * @return {@link #startLocked}
     */
    public Boolean getStartLocked()
    {
        return startLocked;
    }

    /**
     * @param startLocked sets the {@link #startLocked}
     */
    public void setStartLocked(Boolean startLocked)
    {
        this.startLocked = startLocked;
    }

    /**
     * @return {@link #conferenceMeEnabled}
     */
    public Boolean getConferenceMeEnabled()
    {
        return conferenceMeEnabled;
    }

    /**
     * @return {@link #contentImportant}
     */
    public Boolean getContentImportant()
    {
        return contentImportant;
    }

    /**
     * @param contentImportant sets the {@link #contentImportant}
     */
    public void setContentImportant(Boolean contentImportant)
    {
        this.contentImportant = contentImportant;
    }

    /**
     * @param conferenceMeEnabled sets the {@link #conferenceMeEnabled}
     */
    public void setConferenceMeEnabled(Boolean conferenceMeEnabled)
    {
        this.conferenceMeEnabled = conferenceMeEnabled;
    }

    public static final String PIN = "pin";
    public static final String LISTED_PUBLICLY = "listedPublicly";
    public static final String ALLOW_CONTENT = "allowContent";
    public static final String ALLOW_GUESTS = "allowGuests";
    public static final String JOIN_MICROPHONE_DISABLED = "joinMicrophoneDisabled";
    public static final String JOIN_VIDEO_DISABLED = "joinVideoDisabled";
    public static final String REGISTER_WITH_GATEKEEPER = "registerWithGatekeeper";
    public static final String REGISTER_WITH_REGISTRAR = "registerWithRegistrar";
    public static final String START_LOCKED = "startLocked";
    public static final String CONFERENCE_ME_ENABLED = "conferenceMeEnabled";
    public static final String CONTENT_IMPORTANT = "contentImportant";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(PIN, pin);
        dataMap.set(LISTED_PUBLICLY, listedPublicly);
        dataMap.set(ALLOW_CONTENT, allowContent);
        dataMap.set(ALLOW_GUESTS, allowGuests);
        dataMap.set(JOIN_MICROPHONE_DISABLED, joinMicrophoneDisabled);
        dataMap.set(JOIN_VIDEO_DISABLED, joinVideoDisabled);
        dataMap.set(REGISTER_WITH_GATEKEEPER, registerWithGatekeeper);
        dataMap.set(REGISTER_WITH_REGISTRAR, registerWithRegistrar);
        dataMap.set(START_LOCKED, startLocked);
        dataMap.set(CONFERENCE_ME_ENABLED, conferenceMeEnabled);
        dataMap.set(CONTENT_IMPORTANT, contentImportant);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        pin = dataMap.getString(PIN, DEFAULT_COLUMN_LENGTH);
        listedPublicly = dataMap.getBoolean(LISTED_PUBLICLY);
        allowContent = dataMap.getBoolean(ALLOW_CONTENT);
        allowGuests = dataMap.getBoolean(ALLOW_GUESTS);
        joinMicrophoneDisabled = dataMap.getBoolean(JOIN_MICROPHONE_DISABLED);
        joinVideoDisabled = dataMap.getBoolean(JOIN_VIDEO_DISABLED);
        registerWithGatekeeper = dataMap.getBoolean(REGISTER_WITH_GATEKEEPER);
        registerWithRegistrar = dataMap.getBoolean(REGISTER_WITH_REGISTRAR);
        startLocked = dataMap.getBoolean(START_LOCKED);
        conferenceMeEnabled = dataMap.getBoolean(CONFERENCE_ME_ENABLED);
        contentImportant = dataMap.getBoolean(CONTENT_IMPORTANT);
    }

    @Override
    public void merge(RoomSetting roomSetting)
    {
        if (!(roomSetting instanceof H323RoomSetting)) {
            throw new IllegalArgumentException(H323RoomSetting.class.getSimpleName() +
                    " is not compatible with " + roomSetting.getClass().getSimpleName());
        }
        H323RoomSetting h323RoomSetting = (H323RoomSetting) roomSetting;
        if (h323RoomSetting.getPin() != null) {
            setPin(h323RoomSetting.getPin());
        }
        if (h323RoomSetting.getListedPublicly() != null) {
            setListedPublicly(h323RoomSetting.getListedPublicly());
        }
        if (h323RoomSetting.getAllowContent() != null) {
            setAllowContent(h323RoomSetting.getAllowContent());
        }
        if (h323RoomSetting.getAllowGuests() != null) {
            setAllowGuests(h323RoomSetting.getAllowGuests());
        }
        if (h323RoomSetting.getJoinMicrophoneDisabled() != null) {
            setJoinMicrophoneDisabled(h323RoomSetting.getJoinMicrophoneDisabled());
        }
        if (h323RoomSetting.getJoinVideoDisabled() != null) {
            setJoinVideoDisabled(h323RoomSetting.getJoinVideoDisabled());
        }
        if (h323RoomSetting.getRegisterWithGatekeeper() != null) {
            setRegisterWithGatekeeper(h323RoomSetting.getRegisterWithGatekeeper());
        }
        if (h323RoomSetting.getRegisterWithRegistrar() != null) {
            setRegisterWithRegistrar(h323RoomSetting.getRegisterWithRegistrar());
        }
        if (h323RoomSetting.getStartLocked() != null) {
            setStartLocked(h323RoomSetting.getStartLocked());
        }
        if (h323RoomSetting.getConferenceMeEnabled() != null) {
            setConferenceMeEnabled(h323RoomSetting.getConferenceMeEnabled());
        }
        if (h323RoomSetting.getContentImportant() != null) {
            setContentImportant(h323RoomSetting.getContentImportant());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        H323RoomSetting that = (H323RoomSetting) o;

        if (pin != null ? !pin.equals(that.pin) : that.pin != null) return false;
        if (listedPublicly != null ? !listedPublicly.equals(that.listedPublicly) : that.listedPublicly != null)
            return false;
        if (allowContent != null ? !allowContent.equals(that.allowContent) : that.allowContent != null) return false;
        if (allowGuests != null ? !allowGuests.equals(that.allowGuests) : that.allowGuests != null) return false;
        if (joinMicrophoneDisabled != null ? !joinMicrophoneDisabled.equals(that.joinMicrophoneDisabled) : that.joinMicrophoneDisabled != null)
            return false;
        if (joinVideoDisabled != null ? !joinVideoDisabled.equals(that.joinVideoDisabled) : that.joinVideoDisabled != null)
            return false;
        if (registerWithGatekeeper != null ? !registerWithGatekeeper.equals(that.registerWithGatekeeper) : that.registerWithGatekeeper != null)
            return false;
        if (registerWithRegistrar != null ? !registerWithRegistrar.equals(that.registerWithRegistrar) : that.registerWithRegistrar != null)
            return false;
        if (startLocked != null ? !startLocked.equals(that.startLocked) : that.startLocked != null) return false;
        if (conferenceMeEnabled != null ? !conferenceMeEnabled.equals(that.conferenceMeEnabled) : that.conferenceMeEnabled != null)
            return false;
        return contentImportant != null ? contentImportant.equals(that.contentImportant) : that.contentImportant == null;
    }

    @Override
    public int hashCode() {
        int result = pin != null ? pin.hashCode() : 0;
        result = 31 * result + (listedPublicly != null ? listedPublicly.hashCode() : 0);
        result = 31 * result + (allowContent != null ? allowContent.hashCode() : 0);
        result = 31 * result + (allowGuests != null ? allowGuests.hashCode() : 0);
        result = 31 * result + (joinMicrophoneDisabled != null ? joinMicrophoneDisabled.hashCode() : 0);
        result = 31 * result + (joinVideoDisabled != null ? joinVideoDisabled.hashCode() : 0);
        result = 31 * result + (registerWithGatekeeper != null ? registerWithGatekeeper.hashCode() : 0);
        result = 31 * result + (registerWithRegistrar != null ? registerWithRegistrar.hashCode() : 0);
        result = 31 * result + (startLocked != null ? startLocked.hashCode() : 0);
        result = 31 * result + (conferenceMeEnabled != null ? conferenceMeEnabled.hashCode() : 0);
        result = 31 * result + (contentImportant != null ? contentImportant.hashCode() : 0);
        return result;
    }
}
