package cz.cesnet.shongo.controller.common;

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
    @Override
    public RoomSetting clone()
    {
        AdobeConnectRoomSetting roomSetting = new AdobeConnectRoomSetting();
        return roomSetting;
    }

    @Override
    protected cz.cesnet.shongo.oldapi.RoomSetting createApi()
    {
        return new cz.cesnet.shongo.oldapi.AdobeConnectRoomSetting();
    }

    @Override
    public void toApi(cz.cesnet.shongo.oldapi.RoomSetting roomSettingApi)
    {
        super.toApi(roomSettingApi);

        cz.cesnet.shongo.oldapi.AdobeConnectRoomSetting roomSettingAdobeConnectApi =
                (cz.cesnet.shongo.oldapi.AdobeConnectRoomSetting) roomSettingApi;
    }

    @Override
    public void fromApi(cz.cesnet.shongo.oldapi.RoomSetting roomSettingApi)
    {
        super.fromApi(roomSettingApi);

        cz.cesnet.shongo.oldapi.AdobeConnectRoomSetting roomSettingAdobeConnectApi =
                (cz.cesnet.shongo.oldapi.AdobeConnectRoomSetting) roomSettingApi;
    }
}
