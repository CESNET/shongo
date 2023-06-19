package cz.cesnet.shongo.controller.booking.request.auxdata.tagdata;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cesnet.shongo.controller.booking.request.auxdata.AuxData;
import cz.cesnet.shongo.controller.booking.resource.Tag;

import java.util.ArrayList;
import java.util.List;

public class NotifyEmailAuxData extends TagData<List<String>>
{

    public NotifyEmailAuxData(Tag tag, AuxData auxData)
    {
        super(tag, auxData);
    }

    @Override
    public List<String> getData()
    {
        List<String> emails = new ArrayList<>();

        for (JsonNode child : tag.getData()) {
            emails.add(child.asText());
        }
        for (JsonNode child : aux.getData()) {
            emails.add(child.asText());
        }
        return emails;
    }
}
