package cz.cesnet.shongo.api;

/**
 * Represents a setting for a virtual room.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class RoomSetting extends IdentifiedComplexType
{
    /**
     * @param roomSetting to be merged into this {@link RoomSetting}
     */
    public abstract void merge(RoomSetting roomSetting);
}
