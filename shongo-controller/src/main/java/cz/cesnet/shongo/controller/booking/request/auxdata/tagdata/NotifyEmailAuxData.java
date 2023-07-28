package cz.cesnet.shongo.controller.booking.request.auxdata.tagdata;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cesnet.shongo.controller.booking.request.auxdata.AuxDataMerged;

import java.util.ArrayList;
import java.util.List;

public class NotifyEmailAuxData extends TagData<List<String>>
{

    public NotifyEmailAuxData(AuxDataMerged auxData)
    {
        super(auxData);
    }

    @Override
    public List<String> getData()
    {
        List<String> emails = new ArrayList<>();

        for (JsonNode child : auxData.getAuxData()) {
            emails.add(child.asText());
        }
        for (JsonNode child : auxData.getData()) {
            emails.add(child.asText());
        }
        return emails;
    }
}
