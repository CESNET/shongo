package cz.cesnet.shongo.controller.booking.room.settting;

import javax.persistence.Entity;

/**
 * Represents a {@link RoomSetting} for a {@link cz.cesnet.shongo.controller.booking.room.RoomEndpoint} which
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
    protected cz.cesnet.shongo.api.RoomSetting createApi()
    {
        return new cz.cesnet.shongo.api.H323RoomSetting();
    }

    @Override
    public void toApi(cz.cesnet.shongo.api.RoomSetting roomSettingApi)
    {
        super.toApi(roomSettingApi);

        cz.cesnet.shongo.api.H323RoomSetting roomSettingH323Api =
                (cz.cesnet.shongo.api.H323RoomSetting) roomSettingApi;
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
    public void fromApi(cz.cesnet.shongo.api.RoomSetting roomSettingApi)
    {
        super.fromApi(roomSettingApi);

        cz.cesnet.shongo.api.H323RoomSetting roomSettingH323Api =
                (cz.cesnet.shongo.api.H323RoomSetting) roomSettingApi;
        setPin(roomSettingH323Api.getPin());
        setListedPublicly(roomSettingH323Api.getListedPublicly());
        setAllowContent(roomSettingH323Api.getAllowContent());
        setAllowGuests(roomSettingH323Api.getAllowGuests());
        setJoinAudioMuted(roomSettingH323Api.getJoinAudioMuted());
        setJoinVideoMuted(roomSettingH323Api.getJoinVideoMuted());
        setRegisterWithGatekeeper(roomSettingH323Api.getRegisterWithGatekeeper());
        setRegisterWithRegistrar(roomSettingH323Api.getRegisterWithRegistrar());
        setStartLocked(roomSettingH323Api.getStartLocked());
        setConferenceMeEnabled(roomSettingH323Api.getConferenceMeEnabled());
    }

    @Override
    public boolean isSame(Object object)
    {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        H323RoomSetting that = (H323RoomSetting) object;

        if (allowContent != null ? !allowContent.equals(that.allowContent) : that.allowContent != null) {
            return false;
        }
        if (allowGuests != null ? !allowGuests.equals(that.allowGuests) : that.allowGuests != null) {
            return false;
        }
        if (conferenceMeEnabled != null ? !conferenceMeEnabled
                .equals(that.conferenceMeEnabled) : that.conferenceMeEnabled != null) {
            return false;
        }
        if (joinAudioMuted != null ? !joinAudioMuted.equals(that.joinAudioMuted) : that.joinAudioMuted != null) {
            return false;
        }
        if (joinVideoMuted != null ? !joinVideoMuted.equals(that.joinVideoMuted) : that.joinVideoMuted != null) {
            return false;
        }
        if (listedPublicly != null ? !listedPublicly.equals(that.listedPublicly) : that.listedPublicly != null) {
            return false;
        }
        if (pin != null ? !pin.equals(that.pin) : that.pin != null) {
            return false;
        }
        if (registerWithGatekeeper != null ? !registerWithGatekeeper
                .equals(that.registerWithGatekeeper) : that.registerWithGatekeeper != null) {
            return false;
        }
        if (registerWithRegistrar != null ? !registerWithRegistrar
                .equals(that.registerWithRegistrar) : that.registerWithRegistrar != null) {
            return false;
        }
        if (startLocked != null ? !startLocked.equals(that.startLocked) : that.startLocked != null) {
            return false;
        }

        return true;
    }
}
