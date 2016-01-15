package cz.cesnet.shongo.controller.api.domains.request;

import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Action to foreign virtual room to set participants playback level.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class SetPlaybackLevel extends AbstractDomainRoomAction
{
    @JsonProperty("level")
    private int level;

    @JsonCreator
    public SetPlaybackLevel(@JsonProperty("level") int level) {
        this.level = level;
    }

    @Override
    public ConnectorCommand toApi() {
        cz.cesnet.shongo.connector.api.jade.endpoint.SetPlaybackLevel setPlaybackLevel;
        setPlaybackLevel = new cz.cesnet.shongo.connector.api.jade.endpoint.SetPlaybackLevel();
        setPlaybackLevel.setLevel(level);
        return setPlaybackLevel;
    }
}
