package cz.cesnet.shongo.controller.booking.room.settting;

import cz.cesnet.shongo.api.AbstractComplexType;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
@Entity
public class PexipRoomSetting extends RoomSetting {

    /**
     * The PIN that must be entered to get to the room as host.
     */
    private String hostPin;

    /**
     * Optional PIN. If not set, all the guest have the access.
     */
    private String guestPin;

    private Boolean allowGuests;


    public Boolean getAllowGuests()
    {
        return allowGuests;
    }

    public void setAllowGuests(Boolean allowGuests)
    {
        this.allowGuests = allowGuests;
    }


    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
    public String getHostPin() {
        return hostPin;
    }

    public void setHostPin(String hostPin) {
        this.hostPin = hostPin;
    }

    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH)
    public String getGuestPin() {
        return guestPin;
    }

    public void setGuestPin(String guestPin) {
        this.guestPin = guestPin;
    }

    @Override
    public void toApi(cz.cesnet.shongo.api.RoomSetting roomSettingApi) {
        super.toApi(roomSettingApi);

        cz.cesnet.shongo.api.PexipRoomSetting pexipRoomSettingApi =
                (cz.cesnet.shongo.api.PexipRoomSetting) roomSettingApi;
        if (hostPin != null) {
            pexipRoomSettingApi.setHostPin(hostPin);
        }
        if (guestPin != null) {
            pexipRoomSettingApi.setGuestPin(guestPin);
        }
        if (allowGuests != null) {
            pexipRoomSettingApi.setAllowGuests(allowGuests);
        }
    }

    @Override
    public void fromApi(cz.cesnet.shongo.api.RoomSetting roomSettingApi) {
        super.fromApi(roomSettingApi);

        cz.cesnet.shongo.api.PexipRoomSetting pexipRoomSettingApi =
                (cz.cesnet.shongo.api.PexipRoomSetting) roomSettingApi;
        setHostPin(pexipRoomSettingApi.getHostPin());
        setGuestPin(pexipRoomSettingApi.getGuestPin());
        setAllowGuests(pexipRoomSettingApi.getAllowGuests());
    }

    @Override
    public boolean isSame(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        PexipRoomSetting that = (PexipRoomSetting) object;

        if (hostPin != null ? !hostPin.equals(that.hostPin) : that.hostPin != null) {
            return false;
        }
        if (guestPin != null ? !guestPin.equals(that.guestPin) : that.guestPin != null) {
            return false;
        }
        if (allowGuests != null ? !allowGuests.equals(that.allowGuests) : that.allowGuests != null) {
            return false;
        }
        return true;
    }

    @Override
    protected cz.cesnet.shongo.api.RoomSetting createApi() {
        return new cz.cesnet.shongo.api.PexipRoomSetting();
    }
}
