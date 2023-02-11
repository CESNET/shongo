package cz.cesnet.shongo.controller.booking.request.auxdata;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuxData
{

    private String tagName;
    private boolean enabled;
    private JsonNode data;
}
