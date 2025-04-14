package cz.cesnet.shongo.controller.booking.request.auxdata;

import cz.cesnet.shongo.controller.api.TagType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuxDataFilter
{

    private final String tagName;
    private final TagType tagType;
    private final Boolean enabled;
}
