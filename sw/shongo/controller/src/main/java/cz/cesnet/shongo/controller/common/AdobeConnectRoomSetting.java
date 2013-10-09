package cz.cesnet.shongo.controller.common;

import cz.cesnet.shongo.api.AdobeConnectAccessMode;

import javax.persistence.Entity;

/**
 * Represents a {@link cz.cesnet.shongo.controller.common.RoomSetting} for a {@link cz.cesnet.shongo.controller.common.RoomConfiguration} which
 * supports {@link cz.cesnet.shongo.Technology#ADOBE_CONNECT}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
@Entity
public class AdobeConnectRoomSetting extends RoomSetting
{
    /**
     * The PIN which must be entered by participant to join to the room.
     */
    private String pin;

    /**
     * Room access mode
     */
    private AdobeConnectAccessMode accessMode;

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

    public AdobeConnectAccessMode getAccessMode()
    {
        return accessMode;
    }

    public void setAccessMode(AdobeConnectAccessMode accessMode)
    {
        this.accessMode = accessMode;
    }

    @Override
    public RoomSetting clone()
    {
        AdobeConnectRoomSetting roomSetting = new AdobeConnectRoomSetting();
        roomSetting.setPin(getPin());
        roomSetting.setAccessMode(getAccessMode());
        return roomSetting;
    }

    @Override
    protected cz.cesnet.shongo.api.RoomSetting createApi()
    {
        return new cz.cesnet.shongo.api.AdobeConnectRoomSetting();
    }

    @Override
    public void toApi(cz.cesnet.shongo.api.RoomSetting roomSettingApi)
    {
        super.toApi(roomSettingApi);

        cz.cesnet.shongo.api.AdobeConnectRoomSetting roomSettingAdobeConnectApi =
                (cz.cesnet.shongo.api.AdobeConnectRoomSetting) roomSettingApi;

        if (pin != null) {
            roomSettingAdobeConnectApi.setPin(pin);
        }
        if (accessMode != null) {
            roomSettingAdobeConnectApi.setAccessMode(accessMode);
        }

    }

    @Override
    public void fromApi(cz.cesnet.shongo.api.RoomSetting roomSettingApi)
    {
        super.fromApi(roomSettingApi);

        cz.cesnet.shongo.api.AdobeConnectRoomSetting roomSettingAdobeConnectApi =
                (cz.cesnet.shongo.api.AdobeConnectRoomSetting) roomSettingApi;

        setPin(roomSettingAdobeConnectApi.getPin());
        setAccessMode(roomSettingAdobeConnectApi.getAccessMode());

    }
}
