package cz.cesnet.shongo.controller.api.domains.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;
import cz.cesnet.shongo.controller.api.domains.response.AbstractResponse;


import java.util.List;

/**
 * Action to foreign virtual room to mute participant.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class Mute extends AbstractDomainRoomAction
{
    @JsonProperty("roomParticipantId")
    private Long roomParticipantId;

    @JsonCreator
    public Mute(@JsonProperty("roomParticipantId") Long roomParticipantId) {
        this.roomParticipantId = roomParticipantId;
    }

    @Override
    public ConnectorCommand toApi() {
        cz.cesnet.shongo.connector.api.jade.endpoint.Mute mute = new cz.cesnet.shongo.connector.api.jade.endpoint.Mute();
        mute.setId(roomParticipantId);
        return mute;
    }

    @Override
    public <T extends AbstractResponse> Class<T> getReturnClass()
    {
        throw new TodoImplementException();
    }
}