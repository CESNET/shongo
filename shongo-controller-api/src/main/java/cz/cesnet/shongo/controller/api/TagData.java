package cz.cesnet.shongo.controller.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.DataMap;
import lombok.Data;

@Data
public class TagData<T> extends AbstractComplexType
{

    private String name;
    private TagType type;
    private JsonNode data;

    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String DATA = "data";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(NAME, name);
        dataMap.set(TYPE, type);
        dataMap.set(DATA, data);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        name = dataMap.getString(NAME);
        type = dataMap.getEnum(TYPE, TagType.class);
        data = dataMap.getJsonNode(DATA);
    }

    @Override
    @JsonIgnore
    public String getClassName()
    {
        return super.getClassName();
    }
}
