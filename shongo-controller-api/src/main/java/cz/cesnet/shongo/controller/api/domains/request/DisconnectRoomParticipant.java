package cz.cesnet.shongo.controller.api.domains.request;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;
import cz.cesnet.shongo.controller.api.domains.response.AbstractResponse;
import cz.cesnet.shongo.controller.api.domains.response.Room;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Action to foreign virtual room to disconnect room participant.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class DisconnectRoomParticipant extends AbstractDomainRoomAction
{
    @JsonProperty("roomParticipantId")
    private String roomParticipantId;

    @JsonCreator
    public DisconnectRoomParticipant(@JsonProperty("roomParticipantId") String roomParticipantId)
    {
        this.roomParticipantId = roomParticipantId;
    }

    @Override
    public ConnectorCommand toApi()
    {
        cz.cesnet.shongo.connector.api.jade.multipoint.DisconnectRoomParticipant disconnectRoomParticipant;
        disconnectRoomParticipant = new cz.cesnet.shongo.connector.api.jade.multipoint.DisconnectRoomParticipant();
        disconnectRoomParticipant.setRoomParticipantId(roomParticipantId);

        return disconnectRoomParticipant;
    }

    @Override
    public Class<AbstractResponse> getReturnClass()
    {
        return AbstractResponse.class;
    }
}
