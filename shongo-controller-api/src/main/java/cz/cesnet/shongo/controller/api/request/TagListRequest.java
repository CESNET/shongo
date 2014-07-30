package cz.cesnet.shongo.controller.api.request;

import cz.cesnet.shongo.api.DataMap;

/**
 *
 * @author Ondřej Pavelka <pavelka@cesnet.cz>
*/
public class TagListRequest extends AbstractRequest
{
    String resourceId;

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
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
