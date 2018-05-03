package cz.cesnet.shongo.controller.booking.room.settting;

import cz.cesnet.shongo.api.AbstractComplexType;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 *
 * Represents a {@link RoomSetting} for a {@link cz.cesnet.shongo.controller.booking.room.RoomEndpoint} which
 * supports {@link cz.cesnet.shongo.Technology#FREEPBX}.
 * @author Marek Perichta <mperichta@cesnet.cz>
 */

@Entity
public class FreePBXRoomSetting extends RoomSetting{
    /**
     * The PIN which must be entered by room admin to join to the room.
     */
    private String adminPin;

    /**
     * The PIN which must be entered by room user to join to the room.
     */
    private String userPin;

    /**
     * @return {@link #adminPin}
     */
    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
    public String getAdminPin()
    {
        return adminPin;
    }

    /**
     * @param pin sets the {@link #adminPin}
     */
    public void setAdminPin(String pin)
    {
        this.adminPin = pin;
    }


    /**
     * @return {@link #userPin}
     */
    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
    public String getUserPin()
    {
        return userPin;
    }

    /**
     * @param pin sets the {@link #userPin}
     */
    public void setUserPin(String pin)
    {
        this.userPin = pin;
    }


    @Override
    public RoomSetting clone() throws CloneNotSupportedException
    {
        FreePBXRoomSetting roomSetting = (FreePBXRoomSetting) super.clone();
        roomSetting.setAdminPin(getAdminPin());
        roomSetting.setUserPin(getUserPin());
        return roomSetting;
    }

    @Override
    public void toApi(cz.cesnet.shongo.api.RoomSetting roomSettingApi)
    {
        super.toApi(roomSettingApi);

        cz.cesnet.shongo.api.FreePBXRoomSetting roomSettingFreePBXApi =
                (cz.cesnet.shongo.api.FreePBXRoomSetting) roomSettingApi;

        if (adminPin != null) {
            roomSettingFreePBXApi.setAdminPin(adminPin);
        }
        if (userPin != null) {
            roomSettingFreePBXApi.setUserPin(userPin);
        }

    }

    @Override
    public void fromApi(cz.cesnet.shongo.api.RoomSetting roomSettingApi)
    {
        super.fromApi(roomSettingApi);

        cz.cesnet.shongo.api.FreePBXRoomSetting roomSettingFreePBXApi =
                (cz.cesnet.shongo.api.FreePBXRoomSetting) roomSettingApi;

        setAdminPin(roomSettingFreePBXApi.getAdminPin());
        setUserPin(roomSettingFreePBXApi.getUserPin());
    }

    @Override
    protected cz.cesnet.shongo.api.RoomSetting createApi()
    {
        return new cz.cesnet.shongo.api.FreePBXRoomSetting();
    }

    @Override
    public boolean isSame(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        FreePBXRoomSetting that = (FreePBXRoomSetting) object;


        if (adminPin!= null ? !adminPin.equals(that.adminPin) : that.adminPin != null) {
            return false;
        }

        if (userPin != null ? !userPin.equals(that.userPin) : that.userPin != null) {
            return false;
        }

        return true;
    }
}
