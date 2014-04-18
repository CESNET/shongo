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
 * Represents a room in a device.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RoomExecutable extends AbstractRoomExecutable
{
    /**
     * Identifier of the {@link DeviceResource}.
     */
    private String resourceId;

    /**
     * Technology specific room identifier.
     */
    private String roomId;

    /**
     * @return {@link #resourceId}
     */
    public String getResourceId()
    {
        return resourceId;
    }

    /**
     * @param resourceId sets the {@link #resourceId}
     */
    public void setResourceId(String resourceId)
    {
        this.resourceId = resourceId;
    }

    /**
     * @return {@link #roomId}
     */
    public String getRoomId()
    {
        return roomId;
    }

    /**
     * @param roomId sets the {@link #roomId}
     */
    public void setRoomId(String roomId)
    {
        this.roomId = roomId;
    }

    private static final String RESOURCE_ID = "resourceId";
    private static final String ROOM_ID = "roomId";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RESOURCE_ID, resourceId);
        dataMap.set(ROOM_ID, roomId);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        resourceId = dataMap.getString(RESOURCE_ID);
        roomId = dataMap.getString(ROOM_ID);
    }
}
