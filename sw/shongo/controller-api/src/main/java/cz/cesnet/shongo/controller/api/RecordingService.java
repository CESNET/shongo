package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;
import org.joda.time.Interval;

/**
 * Represents a {@link ExecutableService} for recording.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RecordingService extends ExecutableService
{
    /**
     * Identifier of resource used for recording.
     */
    private String resourceId;

    /**
     * Identifier of recording which is currently being recorded.
     */
    private String recordingId;

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
     * @return {@link #recordingId}
     */
    public String getRecordingId()
    {
        return recordingId;
    }

    /**
     * @param recordingId sets the {@link #recordingId}
     */
    public void setRecordingId(String recordingId)
    {
        this.recordingId = recordingId;
    }

    private static final String RESOURCE_ID = "resourceId";
    private static final String RECORDING_ID = "recordingId";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RESOURCE_ID, resourceId);
        dataMap.set(RECORDING_ID, recordingId);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        resourceId = dataMap.getString(RESOURCE_ID);
        recordingId = dataMap.getString(RECORDING_ID);
    }
}
