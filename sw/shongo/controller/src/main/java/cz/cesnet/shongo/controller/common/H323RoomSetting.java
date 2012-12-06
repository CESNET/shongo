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
        return new cz.cesnet.shongo.api.RoomSetting.H323();
    }

    @Override
    public void toApi(cz.cesnet.shongo.api.RoomSetting roomSettingApi)
    {
        super.toApi(roomSettingApi);

        cz.cesnet.shongo.api.RoomSetting.H323 roomSettingH323Api =
                (cz.cesnet.shongo.api.RoomSetting.H323) roomSettingApi;
        roomSettingH323Api.setPin(getPin());
    }

    @Override
    public void fromApi(cz.cesnet.shongo.api.RoomSetting roomSettingApi)
    {
        super.fromApi(roomSettingApi);

        cz.cesnet.shongo.api.RoomSetting.H323 roomSettingH323Api =
                (cz.cesnet.shongo.api.RoomSetting.H323) roomSettingApi;
        if (roomSettingH323Api.isPropertyFilled(roomSettingH323Api.PIN)) {
            setPin(roomSettingH323Api.getPin());
        }
    }
}
