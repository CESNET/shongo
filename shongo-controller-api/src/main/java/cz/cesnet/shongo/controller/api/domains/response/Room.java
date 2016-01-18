package cz.cesnet.shongo.controller.api.domains.response;

import cz.cesnet.shongo.controller.api.domains.request.RoomParticipant;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * Represents a room for foreign resource.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class Room extends AbstractResponse
{
    @JsonProperty("roomParticipants")
    private List<RoomParticipant> roomParticipants;

    public static Room createFromApi(cz.cesnet.shongo.api.Room roomApi)
    {
        Room room = new Room();
//        roomApi.;

        return room;
    }
}
