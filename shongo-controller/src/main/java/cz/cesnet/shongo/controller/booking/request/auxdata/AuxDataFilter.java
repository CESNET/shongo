package cz.cesnet.shongo.controller.booking.request.auxdata;

import cz.cesnet.shongo.controller.api.TagType;
import lombok.Builder;

@Builder
public class AuxDataFilter
{

    private final String tagName;
    private final TagType tagType;
    private final Boolean enabled;

    public String getTagName()
    {
        return tagName;
    }

    public TagType getTagType()
    {
        return tagType;
    }

    public Boolean isEnabled()
    {
        return enabled;
    }
}
