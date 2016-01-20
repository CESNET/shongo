package cz.cesnet.shongo.controller.api.domains.request;

import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;
import cz.cesnet.shongo.controller.api.domains.response.AbstractResponse;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.List;

/**
 * Action to foreign virtual room to set participants microphone level.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class SetMicrophoneLevel extends AbstractDomainRoomAction
{
    @JsonProperty("level")
    private int level;

    @JsonCreator
    public SetMicrophoneLevel(@JsonProperty("level") int level) {
        this.level = level;
    }

    @Override
    public ConnectorCommand toApi() {
        cz.cesnet.shongo.connector.api.jade.endpoint.SetMicrophoneLevel setMicrophoneLevel;
        setMicrophoneLevel = new cz.cesnet.shongo.connector.api.jade.endpoint.SetMicrophoneLevel();
        setMicrophoneLevel.setLevel(level);
        return setMicrophoneLevel;
    }

    @Override
    public <T extends AbstractResponse> Class<T> getReturnClass()
    {
        throw new TodoImplementException();
    }
}
