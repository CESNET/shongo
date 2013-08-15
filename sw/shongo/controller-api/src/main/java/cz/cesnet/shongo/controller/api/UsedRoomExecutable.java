package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.AliasType;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.RoomSetting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a reused room in a device.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UsedRoomExecutable extends AbstractRoomExecutable
{
    /**
     * Used {@link RoomExecutable} identifier.
     */
    private String roomExecutableId;

    /**
     * @return {@link #roomExecutableId}
     */
    public String getRoomExecutableId()
    {
        return roomExecutableId;
    }

    /**
     * @param roomExecutableId sets the {@link #roomExecutableId}
     */
    public void setRoomExecutableId(String roomExecutableId)
    {
        this.roomExecutableId = roomExecutableId;
    }

    private static final String ROOM_EXECUTABLE_ID = "roomExecutableId";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(ROOM_EXECUTABLE_ID, roomExecutableId);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        roomExecutableId = dataMap.getString(ROOM_EXECUTABLE_ID);
    }
}
