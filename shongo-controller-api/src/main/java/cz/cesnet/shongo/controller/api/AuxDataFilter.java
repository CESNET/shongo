package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.DataMap;
import lombok.Data;

@Data
public class AuxDataFilter extends AbstractComplexType
{

    private String tagName;
    private TagType tagType;
    private Boolean enabled;

    private static final String TAG_NAME = "tagName";
    private static final String TAG_TYPE = "tagType";
    private static final String ENABLED = "enabled";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(TAG_NAME, tagName);
        dataMap.set(TAG_TYPE, tagType);
        dataMap.set(ENABLED, enabled);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        tagName = dataMap.getString(TAG_NAME);
        tagType = dataMap.getEnum(TAG_TYPE, TagType.class);
        enabled = dataMap.getBoolean(ENABLED);
    }
}
