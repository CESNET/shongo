package cz.cesnet.shongo.controller.api.domains.request;

import cz.cesnet.shongo.ParticipantRole;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a room participant for room reservation.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class RoomParticipant
{
    @JsonProperty("id")
    String id;

    @JsonProperty("role")
    ParticipantRole role;

    @JsonProperty("values")
    List<RoomParticipantValue> values = new ArrayList<>();

    public RoomParticipant(String id)
    {
        this.id = id;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public ParticipantRole getRole()
    {
        return role;
    }

    public void setRole(ParticipantRole role)
    {
        this.role = role;
    }

    public List<RoomParticipantValue> getValues()
    {
        return values;
    }

    public void setValues(List<RoomParticipantValue> values)
    {
        this.values = values;
    }

    public void addValue(Type type, String value)
    {
        this.values.add(new RoomParticipantValue(type, value));
    }

    private class RoomParticipantValue
    {
        @JsonProperty("type")
        Type type;

        @JsonProperty("value")
        String value;

        public RoomParticipantValue(Type type, String value)
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
    }

    public enum Type
    {
        EPPN,
        EMAIL,
        NAME;
    }
}
