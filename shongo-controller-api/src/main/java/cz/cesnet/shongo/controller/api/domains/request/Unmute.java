package cz.cesnet.shongo.controller.api.domains.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;
import cz.cesnet.shongo.controller.api.domains.response.AbstractResponse;

import java.util.List;

/**
 * Action to foreign virtual room to unmute participant.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class Unmute extends AbstractDomainRoomAction
{
    @JsonProperty("roomParticipantId")
    private Long roomParticipantId;

    @JsonCreator
    public Unmute(@JsonProperty("roomParticipantId") Long roomParticipantId) {
        this.roomParticipantId = roomParticipantId;
    }

    @Override
    public ConnectorCommand toApi() {
        cz.cesnet.shongo.connector.api.jade.endpoint.Unmute unmute = new cz.cesnet.shongo.connector.api.jade.endpoint.Unmute();
        unmute.setId(roomParticipantId);
        return unmute;
    }

    @Override
    public <T extends AbstractResponse> Class<T> getReturnClass()
    {
        throw new TodoImplementException();
    }
}
