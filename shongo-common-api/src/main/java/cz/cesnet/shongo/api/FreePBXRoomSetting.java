package cz.cesnet.shongo.api;

/**
 * @author Marek Perichta <mperichta@cesnet.cz>
 */
public class FreePBXRoomSetting extends RoomSetting
{

    /**
     * The PIN which must be entered by room admin to join to the room.
     */
    private String adminPin;

    /**
     * The PIN which must be entered by room user to join to the room.
     */
    private String userPin;

    public String getAdminPin()
    {
        return adminPin;
    }

    public void setAdminPin(String adminPin)
    {
        this.adminPin = adminPin;
    }

    public String getUserPin()
    {
        return userPin;
    }

    public void setUserPin(String userPin)
    {
        this.userPin = userPin;
    }


    public static final String ADMIN_PIN = "adminPin";
    public static final String USER_PIN = "userPin";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(ADMIN_PIN, adminPin);
        dataMap.set(USER_PIN, userPin);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        adminPin = dataMap.getString(ADMIN_PIN, DEFAULT_COLUMN_LENGTH);
        userPin = dataMap.getString(USER_PIN, DEFAULT_COLUMN_LENGTH);
    }

    @Override
    public void merge(RoomSetting roomSetting)
    {
        if (!(roomSetting instanceof FreePBXRoomSetting)) {
            throw new IllegalArgumentException(FreePBXRoomSetting.class.getSimpleName() +
                    " is not compatible with " + roomSetting.getClass().getSimpleName());
        }
        FreePBXRoomSetting freePBXRoomSetting = (FreePBXRoomSetting) roomSetting;
        if (freePBXRoomSetting.getAdminPin() != null) {
            setAdminPin(freePBXRoomSetting.getAdminPin());
        }
        if (freePBXRoomSetting.getUserPin() != null) {
            setUserPin(freePBXRoomSetting.getUserPin());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FreePBXRoomSetting that = (FreePBXRoomSetting) o;

        if (adminPin != null ? !adminPin.equals(that.adminPin) : that.adminPin != null) return false;
        return userPin != null ? userPin.equals(that.userPin) : that.userPin == null;
    }

    @Override
    public int hashCode() {
        int result = adminPin != null ? adminPin.hashCode() : 0;
        result = 31 * result + (userPin != null ? userPin.hashCode() : 0);
        return result;
    }
}
