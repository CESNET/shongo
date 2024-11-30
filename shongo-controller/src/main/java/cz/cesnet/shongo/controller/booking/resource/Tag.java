package cz.cesnet.shongo.controller.booking.resource;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cesnet.shongo.SimplePersistentObject;
import cz.cesnet.shongo.api.AbstractComplexType;
import cz.cesnet.shongo.controller.api.TagType;
import cz.cesnet.shongo.controller.booking.ObjectIdentifier;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 * @author: Ondřej Pavelka <pavelka@cesnet.cz>
 */
@Entity
public class Tag extends SimplePersistentObject {

    private String name;

    private TagType type;

    private JsonNode data;

    @Column(length = AbstractComplexType.DEFAULT_COLUMN_LENGTH, unique = true)
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    @Column(nullable = false, length = AbstractComplexType.ENUM_COLUMN_LENGTH)
    @Enumerated(EnumType.STRING)
    public TagType getType()
    {
        return type;
    }

    public void setType(TagType type)
    {
        this.type = type;
    }

    // @Type and @Column both needed, because HSQLDB does not support JSONB type
    @Type(type = "jsonb")
    @Column(columnDefinition = "text")
    public JsonNode getData()
    {
        return data;
    }

    public void setData(JsonNode data)
    {
        this.data = data;
    }

    /**
     * @return tag converted capability to API
     */
    public final cz.cesnet.shongo.controller.api.Tag toApi()
    {
        cz.cesnet.shongo.controller.api.Tag api = new cz.cesnet.shongo.controller.api.Tag();
        toApi(api);
        return api;
    }

    public void toApi(cz.cesnet.shongo.controller.api.Tag tagApi)
    {
        tagApi.setId(ObjectIdentifier.formatId(this));
        tagApi.setName(name);
        tagApi.setType(type);
        tagApi.setData(data);
    }

    /**
     * @param tagApi
     * @return tag converted from API
     */
    public static Tag createFromApi(cz.cesnet.shongo.controller.api.Tag tagApi)
    {
        Tag tag = new Tag();
        tag.fromApi(tagApi);
        return tag;
    }

    public void fromApi(cz.cesnet.shongo.controller.api.Tag tagApi)
    {
        this.setName(tagApi.getName());
        this.setType(tagApi.getType());
        this.setData(tagApi.getData());
    }
}
