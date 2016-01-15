package cz.cesnet.shongo.controller.api.domains.request;

import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

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
}