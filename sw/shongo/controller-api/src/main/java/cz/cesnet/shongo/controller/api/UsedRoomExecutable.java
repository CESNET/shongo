package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;

/**
 * Represents a reused room in a device.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class UsedRoomExecutable extends AbstractRoomExecutable
{
    /**
     * Re-used {@link RoomExecutable} identifier.
     */
    private String reusedRoomExecutableId;

    /**
     * @return {@link #reusedRoomExecutableId}
     */
    public String getReusedRoomExecutableId()
    {
        return reusedRoomExecutableId;
    }

    /**
     * @param reusedRoomExecutableId sets the {@link #reusedRoomExecutableId}
     */
    public void setReusedRoomExecutableId(String reusedRoomExecutableId)
    {
        this.reusedRoomExecutableId = reusedRoomExecutableId;
    }

    private static final String REUSED_ROOM_EXECUTABLE_ID = "reusedRoomExecutableId";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(REUSED_ROOM_EXECUTABLE_ID, reusedRoomExecutableId);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        reusedRoomExecutableId = dataMap.getString(REUSED_ROOM_EXECUTABLE_ID);
    }
}
