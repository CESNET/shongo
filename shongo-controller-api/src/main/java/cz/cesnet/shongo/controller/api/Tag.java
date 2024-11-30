package cz.cesnet.shongo.controller.api;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cesnet.shongo.api.DataMap;
import cz.cesnet.shongo.api.IdentifiedComplexType;

import java.util.Objects;

/**
 *
 * @author Ondřej Pavelka <pavelka@cesnet.cz>
 */
public class Tag extends IdentifiedComplexType
{
    private static final ObjectMapper objectMapper = new ObjectMapper();

    String name;
    TagType type = TagType.DEFAULT;
    JsonNode data;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TagType getType()
    {
        return type;
    }

    public void setType(TagType type)
    {
        this.type = type;
    }

    public JsonNode getData()
    {
        return data;
    }

    public void setData(JsonNode data)
    {
        this.data = data;
    }

    public static Tag fromConcat(String concat)
    {
        String[] parts = concat.split(",", 4);
        Tag tag = new Tag();
        tag.setId(parts[0]);
        tag.setName(parts[1]);
        tag.setType(TagType.valueOf(parts[2]));
        if (parts.length > 3) {
            try {
                tag.setData(objectMapper.readTree(parts[3]));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Error parsing tag data", e);
            }
        }
        return tag;
    }

    @Override
    @JsonIgnore
    public String getClassName() {
        return super.getClassName();
    }

    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String DATA = "data";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(NAME,name);
        dataMap.set(TYPE, type);
        dataMap.set(DATA, data);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        name = dataMap.getString(NAME);
        type = dataMap.getEnumRequired(TYPE, TagType.class);
        JsonNode jsonNode = dataMap.getJsonNode(DATA);
        if (jsonNode != null) {
            data = jsonNode;
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Tag tag = (Tag) o;
        return Objects.equals(name, tag.name) && type == tag.type && Objects.equals(data, tag.data);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, type, data);
    }
}
