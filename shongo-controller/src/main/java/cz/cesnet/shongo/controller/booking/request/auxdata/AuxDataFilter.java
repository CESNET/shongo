package cz.cesnet.shongo.controller.booking.request.auxdata;

import cz.cesnet.shongo.controller.api.TagType;

public class AuxDataFilter
{

    private final String tagName;
    private final TagType tagType;
    private final Boolean enabled;

    private AuxDataFilter(String tagName, TagType tagType, boolean enabled)
    {
        this.tagName = tagName;
        this.tagType = tagType;
        this.enabled = enabled;
    }

    public static Builder builder()
    {
        return new Builder();
    }

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

    public static class Builder
    {

        private String tagName;
        private TagType tagType;
        private boolean enabled;

        private Builder()
        {
        }

        public Builder tagName(String tagName)
        {
            this.tagName = tagName;
            return this;
        }

        public Builder tagType(TagType tagType)
        {
            this.tagType = tagType;
            return this;
        }

        public Builder enabled(boolean enabled)
        {
            this.enabled = enabled;
            return this;
        }

        public AuxDataFilter build()
        {
            return new AuxDataFilter(tagName, tagType, enabled);
        }
    }
}
