package cz.cesnet.shongo.controller.booking.request.auxdata;

import com.fasterxml.jackson.databind.JsonNode;

public class AuxData
{

    private String tagName;
    private boolean enabled;
    private JsonNode data;

    public AuxData()
    {
    }

    public AuxData(String tagName, boolean enabled, JsonNode data)
    {
        this.tagName = tagName;
        this.enabled = enabled;
        this.data = data;
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

    public JsonNode getData()
    {
        return data;
    }

    public void setData(JsonNode data)
    {
        this.data = data;
    }

    @Override
    public String toString()
    {
        return "AuxData{" +
                "tag='" + tagName + '\'' +
                ", enabled=" + enabled +
                ", data='" + data + '\'' +
                '}';
    }
}
