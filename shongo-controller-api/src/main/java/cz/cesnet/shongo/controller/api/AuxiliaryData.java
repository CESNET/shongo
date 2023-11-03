package cz.cesnet.shongo.controller.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.DataMap;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class AuxiliaryData extends AbstractComplexType
{

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String tagName;
    private boolean enabled;
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private String data = objectMapper.nullNode().toString();

    public AuxiliaryData(String tagName, boolean enabled, String data)
    {
        setTagName(tagName);
        setEnabled(enabled);
        setData(data);
    }

    @JsonIgnore
    @ToString.Include(name = "data")
    @EqualsAndHashCode.Include
    public JsonNode getDataAsJsonNode()
    {
        if (data == null) {
            return null;
        }

        try {
            return objectMapper.readTree(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void setData(String data)
    {
        if (data == null) {
            this.data = objectMapper.nullNode().toString();
            return;
        }

        this.data = data;
    }

    public void setData(JsonNode data)
    {
        try {
            this.data = objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
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
        data = dataMap.getString("data");
    }
}
