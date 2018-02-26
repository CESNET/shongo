package cz.cesnet.shongo.controller.booking.room.settting;

import cz.cesnet.shongo.api.AbstractComplexType;

import javax.persistence.Column;
import javax.persistence.Entity;

/**
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
    protected cz.cesnet.shongo.api.RoomSetting createApi()
    {
        return null;
    }

    @Override
    public boolean isSame(Object object) {
        return false;
    }
}
