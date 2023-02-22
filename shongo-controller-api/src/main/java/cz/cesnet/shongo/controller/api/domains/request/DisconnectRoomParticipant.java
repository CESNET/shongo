package cz.cesnet.shongo.controller.api.domains.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;
import cz.cesnet.shongo.controller.api.domains.response.AbstractResponse;


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
