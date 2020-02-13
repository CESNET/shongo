package cz.cesnet.shongo.controller.api.domains.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;
import cz.cesnet.shongo.controller.api.domains.response.AbstractResponse;


import java.util.List;

/**
 * Action to foreign virtual room to unmute all participants.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class UnmuteAll extends AbstractDomainRoomAction
{
    @JsonProperty("roomId")
    private Long roomId;

    @JsonCreator
    public UnmuteAll(@JsonProperty("roomId") Long roomId) {
        this.roomId = roomId;
    }

    @Override
    public ConnectorCommand toApi() {
        cz.cesnet.shongo.connector.api.jade.endpoint.Unmute unmute = new cz.cesnet.shongo.connector.api.jade.endpoint.Unmute();
        unmute.setId(roomId);
        return unmute;
    }

    @Override
    public <T extends AbstractResponse> Class<T> getReturnClass()
    {
        throw new TodoImplementException();
    }
}
