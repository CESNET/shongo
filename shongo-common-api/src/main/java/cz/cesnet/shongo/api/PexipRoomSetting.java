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


    public static final String HOST_PIN = "hostPin";
    public static final String GUEST_PIN = "guestPin";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(HOST_PIN, hostPin);
        dataMap.set(GUEST_PIN, guestPin);

        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        hostPin = dataMap.getString(HOST_PIN, DEFAULT_COLUMN_LENGTH);
        guestPin = dataMap.getString(GUEST_PIN, DEFAULT_COLUMN_LENGTH);

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

    }
}
