package cz.cesnet.shongo.controller.common;

import javax.persistence.Entity;

/**
 * Represents a {@link RoomSetting} for a {@link RoomConfiguration} which supports {@link cz.cesnet.shongo.Technology#H323}.
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
}
