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

    public cz.cesnet.shongo.controller.api.AuxiliaryData toApi()
    {
        cz.cesnet.shongo.controller.api.AuxiliaryData api = new cz.cesnet.shongo.controller.api.AuxiliaryData();
        api.setTagName(getTagName());
        api.setEnabled(isEnabled());
        api.setData(getData());
        return api;
    }

    public static AuxData fromApi(cz.cesnet.shongo.controller.api.AuxiliaryData api)
    {
        AuxData auxData = new AuxData();
        auxData.setTagName(api.getTagName());
        auxData.setEnabled(api.isEnabled());
        auxData.setData(api.getDataAsJsonNode());
        return auxData;
    }
}
