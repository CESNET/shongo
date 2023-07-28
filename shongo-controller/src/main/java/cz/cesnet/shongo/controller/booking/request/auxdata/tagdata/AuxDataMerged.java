package cz.cesnet.shongo.controller.booking.request.auxdata.tagdata;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cesnet.shongo.controller.api.TagType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuxDataMerged
{

    private String tagName;
    private TagType type;
    private Boolean enabled;
    private JsonNode data;
    private JsonNode auxData;
}
