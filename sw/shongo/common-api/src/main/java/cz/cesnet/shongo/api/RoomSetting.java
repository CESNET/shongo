package cz.cesnet.shongo.api;

import cz.cesnet.shongo.api.util.IdentifiedChangeableObject;

/**
 * Represents a setting for a virtual room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class RoomSetting extends IdentifiedChangeableObject
{
    /**
     * Represents a {@link RoomSetting} for a {@link cz.cesnet.shongo.Technology#H323}.
     */
    public static class H323 extends RoomSetting
    {
        /**
         * The PIN that must be entered to get to the room.
         */
        public static final String PIN = "pin";

        /**
         * @param pin sets the {@link #PIN}
         * @return this {@link H323}
         */
        public H323 withPin(String pin)
        {
            setPin(pin);
            return this;
        }

        /**
         * @return {@link #PIN}
         */
        public String getPin()
        {
            return getPropertyStorage().getValue(PIN);
        }

        /**
         * @param pin sets the {@link #PIN}
         */
        public void setPin(String pin)
        {
            getPropertyStorage().setValue(PIN, pin);
        }
    }
}
