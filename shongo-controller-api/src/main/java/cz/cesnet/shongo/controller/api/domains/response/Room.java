package cz.cesnet.shongo.controller.api.domains.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.RoomLayout;
import cz.cesnet.shongo.api.RoomParticipantRole;


import java.util.ArrayList;
import java.util.HashSet;
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
    private List<Alias> aliases = new ArrayList<>();

    @JsonProperty("technologies")
    private Set<Technology> technologies;

    @JsonProperty("roomLayout")
    private RoomLayout roomLayout;

    @JsonProperty("roomParticipants")
    private List<cz.cesnet.shongo.controller.api.domains.request.ForeignRoomParticipantRole> roomParticipants;

    //TODO room settings??

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

    public List<cz.cesnet.shongo.controller.api.domains.request.ForeignRoomParticipantRole> getRoomParticipants()
    {
        return roomParticipants;
    }

    public void setRoomParticipants(List<cz.cesnet.shongo.controller.api.domains.request.ForeignRoomParticipantRole> roomParticipants)
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
        List<cz.cesnet.shongo.controller.api.domains.request.ForeignRoomParticipantRole> participants = new ArrayList<>();
        for (RoomParticipantRole participantRole : roomApi.getParticipantRoles()) {
            ParticipantRole role = participantRole.getRole();
            String id = participantRole.getUserId();

            cz.cesnet.shongo.controller.api.domains.request.ForeignRoomParticipantRole roomParticipant;
            roomParticipant = new cz.cesnet.shongo.controller.api.domains.request.ForeignRoomParticipantRole(id, role);
            participants.add(roomParticipant);
        }
        room.setRoomParticipants(participants);
        room.setTechnologies(roomApi.getTechnologies());

        return room;
    }

    public cz.cesnet.shongo.api.Room toApi()
    {
        cz.cesnet.shongo.api.Room room = new cz.cesnet.shongo.api.Room();
        room.setId(id);
        room.setDescription(description);
        for (Alias alias : aliases) {
            room.addAlias(alias.getType(), alias.getValue());
        }
        //TODO licences
        room.setLayout(roomLayout);
        for (cz.cesnet.shongo.controller.api.domains.request.ForeignRoomParticipantRole participant : roomParticipants) {
            room.addParticipantRole(participant.getUserId(), participant.getRole());
        }
        room.setTechnologies(new HashSet<>(technologies));

        return room;
    }
}
