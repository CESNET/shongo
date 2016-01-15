package cz.cesnet.shongo.controller.api.domains.request;

import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Action to foreign virtual room to mute all participants.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class MuteAll extends AbstractDomainRoomAction
{
    @JsonProperty("roomId")
    private Long roomId;

    @JsonCreator
    public MuteAll(@JsonProperty("roomId") Long roomId) {
        this.roomId = roomId;
    }

    @Override
    public ConnectorCommand toApi() {
        cz.cesnet.shongo.connector.api.jade.endpoint.Mute mute = new cz.cesnet.shongo.connector.api.jade.endpoint.Mute();
        mute.setId(roomId);
        return mute;
    }
}
