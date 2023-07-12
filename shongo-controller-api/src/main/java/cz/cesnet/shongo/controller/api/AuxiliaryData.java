package cz.cesnet.shongo.controller.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.api.DataMap;

import java.util.Objects;

public class AuxiliaryData extends AbstractComplexType
{

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String tagName;
    private boolean enabled;
    private String data = objectMapper.nullNode().toString();

    public AuxiliaryData()
    {
    }

    public AuxiliaryData(String tagName, boolean enabled, String data)
    {
        setTagName(tagName);
        setEnabled(enabled);
        setData(data);
    }

    public String getTagName()
    {
        return tagName;
    }

    public void setTagName(String tagName)
    {
        this.tagName = tagName;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getData()
    {
        return data;
    }

    @JsonIgnore
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

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuxiliaryData auxData = (AuxiliaryData) o;
        return enabled == auxData.enabled && Objects.equals(tagName, auxData.tagName) &&
                Objects.equals(getDataAsJsonNode(), auxData.getDataAsJsonNode());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(tagName, enabled, data);
    }

    @Override
    public String toString()
    {
        return "AuxiliaryData{" +
                "tagName='" + tagName + '\'' +
                ", enabled=" + enabled +
                ", data='" + data + '\'' +
                '}';
    }
}
