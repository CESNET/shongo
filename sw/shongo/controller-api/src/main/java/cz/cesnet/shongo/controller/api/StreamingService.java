package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.DataMap;

/**
 * Represents a {@link cz.cesnet.shongo.controller.api.ExecutableService} for streaming.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class StreamingService extends ExecutableService
{
    /**
     * Identifier of resource used for streaming.
     */
    private String resourceId;

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

    private static final String RESOURCE_ID = "resourceId";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(RESOURCE_ID, resourceId);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        resourceId = dataMap.getString(RESOURCE_ID);
    }
}
