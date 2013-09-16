package cz.cesnet.shongo.api;

/**
 * Represents a {@link RoomSetting} for a {@link cz.cesnet.shongo.Technology#ADOBE_CONNECT}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class AdobeConnectRoomSetting extends RoomSetting
{
    /**
     * The PIN that must be entered to get to the room.
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

    /**
     * @param pin sets the {@link #pin}
     * @return this {@link H323RoomSetting}
     */
    public AdobeConnectRoomSetting withPin(String pin)
    {
        setPin(pin);
        return this;
    }

    public static final String PIN = "pin";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(PIN, pin);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        pin = dataMap.getString(PIN);
    }

}
