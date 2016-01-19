package cz.cesnet.shongo.controller.api.domains.response;

import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.RoomLayout;
import cz.cesnet.shongo.api.RoomParticipantRole;
import cz.cesnet.shongo.controller.api.domains.request.RoomParticipant;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Represents a room for foreign resource.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class Room extends AbstractResponse
{
    @JsonProperty("id")
    private String id;

    @JsonProperty("description")
    private String description;

    @JsonProperty("aliases")
    private List<Alias> aliases;

    @JsonProperty("technologies")
    private Set<Technology> technologies;

    @JsonProperty("roomLayout")
    private RoomLayout roomLayout;

    @JsonProperty("roomParticipants")
    private List<RoomParticipant> roomParticipants;

    //TODO room settings??


    public Room()
    {
    }

    @JsonCreator
    public Room(@JsonProperty("id") String id,
                   @JsonProperty("description") String description,
                   @JsonProperty("aliases") List<Alias> aliases,
                   @JsonProperty("technologies") Set<Technology> technologies,
                   @JsonProperty("roomLayout") RoomLayout roomLayout,
                   @JsonProperty("roomParticipants") List<RoomParticipant> roomParticipants)
    {
        this.id = id;
        this.description = description;
        this.aliases = aliases;
        this.technologies = technologies;
        this.roomLayout = roomLayout;
        this.roomParticipants = roomParticipants;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public List<Alias> getAliases()
    {
        return aliases;
    }

    public void setAliases(List<Alias> aliases)
    {
        this.aliases = aliases;
    }

    public Set<Technology> getTechnologies()
    {
        return technologies;
    }

    public void setTechnologies(Set<Technology> technologies)
    {
        this.technologies = technologies;
    }

    public RoomLayout getRoomLayout()
    {
        return roomLayout;
    }

    public void setRoomLayout(RoomLayout roomLayout)
    {
        this.roomLayout = roomLayout;
    }

    public List<RoomParticipant> getRoomParticipants()
    {
        return roomParticipants;
    }

    public void setRoomParticipants(List<RoomParticipant> roomParticipants)
    {
        this.roomParticipants = roomParticipants;
    }

    public static Room createFromApi(cz.cesnet.shongo.api.Room roomApi)
    {
        Room room = new Room();
        room.setId(roomApi.getId());
        room.setDescription(roomApi.getDescription());
        List<Alias> aliases = new ArrayList<>();
        for (cz.cesnet.shongo.api.Alias alias : roomApi.getAliases()) {
            aliases.add(new Alias(alias.getType(), alias.getValue()));
        }
        room.setAliases(aliases);
        room.setRoomLayout(roomApi.getLayout());
        List<RoomParticipant> participants = new ArrayList<>();
        for (RoomParticipantRole participantRole : roomApi.getParticipantRoles()) {
            ParticipantRole role = participantRole.getRole();
            String id = participantRole.getUserId();

            RoomParticipant roomParticipant = new RoomParticipant(id, role);
            participants.add(roomParticipant);
        }
        room.setRoomParticipants(participants);
        room.setTechnologies(roomApi.getTechnologies());

        return room;
    }
}
