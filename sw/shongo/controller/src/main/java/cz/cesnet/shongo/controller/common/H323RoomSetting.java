package cz.cesnet.shongo.controller.common;

import javax.persistence.Entity;

/**
 * Represents a {@link RoomSetting} for a {@link RoomConfiguration} which
 * supports {@link cz.cesnet.shongo.Technology#H323}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class H323RoomSetting extends RoomSetting
{
    /**
     * The PIN which must be entered by participant to join to the room.
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

    @Override
    public RoomSetting clone()
    {
        H323RoomSetting roomSetting = new H323RoomSetting();
        roomSetting.setPin(getPin());
        return roomSetting;
    }

    @Override
    protected cz.cesnet.shongo.oldapi.RoomSetting createApi()
    {
        return new cz.cesnet.shongo.oldapi.H323RoomSetting();
    }

    @Override
    public void toApi(cz.cesnet.shongo.oldapi.RoomSetting roomSettingApi)
    {
        super.toApi(roomSettingApi);

        cz.cesnet.shongo.oldapi.H323RoomSetting roomSettingH323Api =
                (cz.cesnet.shongo.oldapi.H323RoomSetting) roomSettingApi;
        if (pin != null) {
            roomSettingH323Api.setPin(pin);
        }
        if (listedPublicly != null) {
            roomSettingH323Api.setListedPublicly(listedPublicly);
        }
        if (allowContent != null) {
            roomSettingH323Api.setAllowContent(allowContent);
        }
        if (allowGuests != null) {
            roomSettingH323Api.setAllowGuests(allowGuests);
        }
        if (joinAudioMuted != null) {
            roomSettingH323Api.setJoinAudioMuted(joinAudioMuted);
        }
        if (joinVideoMuted != null) {
            roomSettingH323Api.setJoinVideoMuted(joinVideoMuted);
        }
        if (registerWithGatekeeper != null) {
            roomSettingH323Api.setRegisterWithGatekeeper(registerWithGatekeeper);
        }
        if (registerWithRegistrar != null) {
            roomSettingH323Api.setRegisterWithRegistrar(registerWithRegistrar);
        }
        if (startLocked != null) {
            roomSettingH323Api.setStartLocked(startLocked);
        }
        if (conferenceMeEnabled != null) {
            roomSettingH323Api.setConferenceMeEnabled(conferenceMeEnabled);
        }
    }

    @Override
    public void fromApi(cz.cesnet.shongo.oldapi.RoomSetting roomSettingApi)
    {
        super.fromApi(roomSettingApi);

        cz.cesnet.shongo.oldapi.H323RoomSetting roomSettingH323Api =
                (cz.cesnet.shongo.oldapi.H323RoomSetting) roomSettingApi;
        if (roomSettingH323Api.isPropertyFilled(cz.cesnet.shongo.oldapi.H323RoomSetting.PIN)) {
            setPin(roomSettingH323Api.getPin());
        }
        if (roomSettingH323Api.isPropertyFilled(cz.cesnet.shongo.oldapi.H323RoomSetting.LISTED_PUBLICLY)) {
            setListedPublicly(roomSettingH323Api.getListedPublicly());
        }
        if (roomSettingH323Api.isPropertyFilled(cz.cesnet.shongo.oldapi.H323RoomSetting.ALLOW_CONTENT)) {
            setAllowContent(roomSettingH323Api.getAllowContent());
        }
        if (roomSettingH323Api.isPropertyFilled(cz.cesnet.shongo.oldapi.H323RoomSetting.ALLOW_GUESTS)) {
            setAllowGuests(roomSettingH323Api.getAllowGuests());
        }
        if (roomSettingH323Api.isPropertyFilled(cz.cesnet.shongo.oldapi.H323RoomSetting.JOIN_AUDIO_MUTED)) {
            setJoinAudioMuted(roomSettingH323Api.getJoinAudioMuted());
        }
        if (roomSettingH323Api.isPropertyFilled(cz.cesnet.shongo.oldapi.H323RoomSetting.JOIN_VIDEO_MUTED)) {
            setJoinVideoMuted(roomSettingH323Api.getJoinVideoMuted());
        }
        if (roomSettingH323Api.isPropertyFilled(cz.cesnet.shongo.oldapi.H323RoomSetting.REGISTER_WITH_GATEKEEPER)) {
            setRegisterWithGatekeeper(roomSettingH323Api.getRegisterWithGatekeeper());
        }
        if (roomSettingH323Api.isPropertyFilled(cz.cesnet.shongo.oldapi.H323RoomSetting.REGISTER_WITH_REGISTRAR)) {
            setRegisterWithRegistrar(roomSettingH323Api.getRegisterWithRegistrar());
        }
        if (roomSettingH323Api.isPropertyFilled(cz.cesnet.shongo.oldapi.H323RoomSetting.START_LOCKED)) {
            setStartLocked(roomSettingH323Api.getStartLocked());
        }
        if (roomSettingH323Api.isPropertyFilled(cz.cesnet.shongo.oldapi.H323RoomSetting.CONFERENCE_ME_ENABLED)) {
            setConferenceMeEnabled(roomSettingH323Api.getConferenceMeEnabled());
        }
    }
}
