package cz.cesnet.shongo.controller.api.domains.request;

import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.controller.api.AnonymousPerson;
import cz.cesnet.shongo.controller.api.PersonParticipant;
import org.codehaus.jackson.annotate.JsonCreator;
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

    @JsonCreator
    public RoomParticipant(@JsonProperty("id") String id,
                           @JsonProperty("role") ParticipantRole role,
                           @JsonProperty("values") List<RoomParticipantValue> values)
    {
        this.id = id;
        this.role = role;
        this.values = values;
    }

    public RoomParticipant(String id, ParticipantRole role)
    {
        this.id = id;
        this.role = role;
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

    public void addValue(RoomParticipantValue.Type type, String value)
    {
        this.values.add(new RoomParticipantValue(type, value));
    }

    public PersonParticipant toApi() {
        PersonParticipant participant = new PersonParticipant();
        participant.setRole(role);
        AnonymousPerson person = new AnonymousPerson();
        for (RoomParticipantValue value : values) {
            switch (value.getType()) {
                case NAME:
                    person.setName(value.getValue());
                    break;
                case EMAIL:
                    person.setEmail(value.getValue());
                    break;
            }
        }
        participant.setPerson(person);
        return participant;
    }



}
