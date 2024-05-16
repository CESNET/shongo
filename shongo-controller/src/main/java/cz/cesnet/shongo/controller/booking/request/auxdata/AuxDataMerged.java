package cz.cesnet.shongo.controller.booking.request.auxdata;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cesnet.shongo.controller.api.TagType;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class AuxDataMerged
{

    String tagName;
    TagType type;
    Boolean enabled;
    JsonNode data;
    JsonNode auxData;
}
