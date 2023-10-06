package cz.cesnet.shongo.controller.booking.request.auxdata;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class AuxData
{

    private String tagName;
    private boolean enabled;
    private JsonNode data;

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
