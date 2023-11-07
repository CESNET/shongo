package cz.cesnet.shongo.controller.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.DataMap;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class AuxiliaryData extends AbstractComplexType
{

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String tagName;
    private boolean enabled;
    private JsonNode data;

    public AuxiliaryData(String tagName, boolean enabled, JsonNode data)
    {
        setTagName(tagName);
        setEnabled(enabled);
        setData(data);
    }

    public void setData(JsonNode data) {
        this.data = data;
        if (data == null) {
            this.data = objectMapper.nullNode();
        }
    }

    @Override
    @JsonIgnore
    public String getClassName() {
        return super.getClassName();
    }

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set("tagName", tagName);
        dataMap.set("enabled", enabled);
        dataMap.set("data", data);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        tagName = dataMap.getString("tagName");
        enabled = dataMap.getBoolean("enabled");
        data = dataMap.getJsonNode("data");
    }
}
