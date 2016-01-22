package cz.cesnet.shongo.controller.api.domains.request;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Represents a room participant value for {@link RoomParticipantRole}.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class RoomParticipantValue
{
    @JsonProperty("type")
    Type type;

    @JsonProperty("value")
    String value;

    @JsonCreator
    public RoomParticipantValue(@JsonProperty("type") Type type, @JsonProperty("value") String value)
    {
        this.type = type;
        this.value = value;
    }

    public Type getType()
    {
        return type;
    }

    public void setType(Type type)
    {
        this.type = type;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public enum Type
    {
        EPPN,
        EMAIL,
        NAME;
    }
}
