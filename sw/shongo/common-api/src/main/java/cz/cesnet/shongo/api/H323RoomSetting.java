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
    private Boolean joinAudioMuted;

    /**
     * A boolean option whether video should be muted on join. Defaults to false.
     */
    private Boolean joinVideoMuted;


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
     * @return {@link #joinAudioMuted}
     */
    public Boolean getJoinAudioMuted()
    {
        return joinAudioMuted;
    }

    /**
     * @param joinAudioMuted sets the {@link #joinAudioMuted}
     */
    public void setJoinAudioMuted(Boolean joinAudioMuted)
    {
        this.joinAudioMuted = joinAudioMuted;
    }

    /**
     * @return {@link #joinVideoMuted}
     */
    public Boolean getJoinVideoMuted()
    {
        return joinVideoMuted;
    }

    /**
     * @param joinVideoMuted sets the {@link #joinVideoMuted}
     */
    public void setJoinVideoMuted(Boolean joinVideoMuted)
    {
        this.joinVideoMuted = joinVideoMuted;
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
    public static final String JOIN_AUDIO_MUTED = "joinAudioMuted";
    public static final String JOIN_VIDEO_MUTED = "joinVideoMuted";
    public static final String REGISTER_WITH_GATEKEEPER = "registerWithGatekeeper";
    public static final String REGISTER_WITH_REGISTRAR = "registerWithRegistrar";
    public static final String START_LOCKED = "startLocked";
    public static final String CONFERENCE_ME_ENABLED = "conferenceMeEnabled";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(PIN, pin);
        dataMap.set(LISTED_PUBLICLY, listedPublicly);
        dataMap.set(ALLOW_CONTENT, allowContent);
        dataMap.set(ALLOW_GUESTS, allowGuests);
        dataMap.set(JOIN_AUDIO_MUTED, joinAudioMuted);
        dataMap.set(JOIN_VIDEO_MUTED, joinVideoMuted);
        dataMap.set(REGISTER_WITH_GATEKEEPER, registerWithGatekeeper);
        dataMap.set(REGISTER_WITH_REGISTRAR, registerWithRegistrar);
        dataMap.set(START_LOCKED, startLocked);
        dataMap.set(CONFERENCE_ME_ENABLED, conferenceMeEnabled);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        pin = dataMap.getString(PIN);
        listedPublicly = dataMap.getBoolean(LISTED_PUBLICLY);
        allowContent = dataMap.getBoolean(ALLOW_CONTENT);
        allowGuests = dataMap.getBoolean(ALLOW_GUESTS);
        joinAudioMuted = dataMap.getBoolean(JOIN_AUDIO_MUTED);
        joinVideoMuted = dataMap.getBoolean(JOIN_VIDEO_MUTED);
        registerWithGatekeeper = dataMap.getBoolean(REGISTER_WITH_GATEKEEPER);
        registerWithRegistrar = dataMap.getBoolean(REGISTER_WITH_REGISTRAR);
        startLocked = dataMap.getBoolean(START_LOCKED);
        conferenceMeEnabled = dataMap.getBoolean(CONFERENCE_ME_ENABLED);
    }
}
