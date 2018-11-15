package cz.cesnet.shongo.api;

/**
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
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

    public String getHostPin() {
        return hostPin;
    }

    public void setHostPin(String hostPin) {
        this.hostPin = hostPin;
    }

    public String getGuestPin() {
        return guestPin;
    }

    public void setGuestPin(String guestPin) {
        this.guestPin = guestPin;
    }

    public Boolean getAllowGuests() {
        return allowGuests;
    }

    public void setAllowGuests(Boolean allowGuests) {
        this.allowGuests = allowGuests;
    }

    public static final String ALLOW_GUESTS = "allowGuests";
    public static final String HOST_PIN = "hostPin";
    public static final String GUEST_PIN = "guestPin";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(HOST_PIN, hostPin);
        dataMap.set(GUEST_PIN, guestPin);
        dataMap.set(ALLOW_GUESTS, allowGuests);

        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        hostPin = dataMap.getString(HOST_PIN, DEFAULT_COLUMN_LENGTH);
        guestPin = dataMap.getString(GUEST_PIN, DEFAULT_COLUMN_LENGTH);
        allowGuests = dataMap.getBoolean(ALLOW_GUESTS);

    }

    @Override
    public void merge(RoomSetting roomSetting) {
        if (!(roomSetting instanceof PexipRoomSetting)) {
            throw new IllegalArgumentException(PexipRoomSetting.class.getSimpleName() +
                    " is not compatible with " + roomSetting.getClass().getSimpleName());
        }
        PexipRoomSetting pexipRoomSetting = (PexipRoomSetting) roomSetting;
        if (pexipRoomSetting.getHostPin() != null) {
            setHostPin(pexipRoomSetting.getHostPin());
        }
        if (pexipRoomSetting.getGuestPin() != null) {
            setGuestPin(pexipRoomSetting.getGuestPin());
        }
        if (pexipRoomSetting.getAllowGuests() != null) {
            setAllowGuests(pexipRoomSetting.getAllowGuests());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PexipRoomSetting that = (PexipRoomSetting) o;

        if (hostPin != null ? !hostPin.equals(that.hostPin) : that.hostPin != null) return false;
        if (guestPin != null ? !guestPin.equals(that.guestPin) : that.guestPin != null) return false;
        return allowGuests != null ? allowGuests.equals(that.allowGuests) : that.allowGuests == null;
    }

    @Override
    public int hashCode() {
        int result = hostPin != null ? hostPin.hashCode() : 0;
        result = 31 * result + (guestPin != null ? guestPin.hashCode() : 0);
        result = 31 * result + (allowGuests != null ? allowGuests.hashCode() : 0);
        return result;
    }
}
