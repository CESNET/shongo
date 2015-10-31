package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;

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
     *
     */
    private String recordingFolderId;

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

    public String getRecordingFolderId() {
        return recordingFolderId;
    }

    public void setRecordingFolderId(String recordingFolderId) {
        this.recordingFolderId = recordingFolderId;
    }

    private static final String RESOURCE_ID = "resourceId";
    private static final String ROOM_ID = "roomId";
    private static final String RECORDING_FOLDER_ID = "recordingFolderId";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RESOURCE_ID, resourceId);
        dataMap.set(ROOM_ID, roomId);
        dataMap.set(RECORDING_FOLDER_ID, recordingFolderId);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        resourceId = dataMap.getString(RESOURCE_ID);
        roomId = dataMap.getString(ROOM_ID);
        recordingFolderId = dataMap.getString(RECORDING_FOLDER_ID);
    }
}
