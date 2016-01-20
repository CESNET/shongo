package cz.cesnet.shongo.controller.api.domains.request;

import cz.cesnet.shongo.connector.api.jade.ConnectorCommand;
import cz.cesnet.shongo.controller.api.domains.response.AbstractResponse;
import cz.cesnet.shongo.controller.api.domains.response.Room;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

import java.util.List;

/**
 * Represents an action for foreign virtual room.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "action")
@JsonSubTypes({
        @JsonSubTypes.Type(value = GetRoom.class, name = "getRoom"),
        @JsonSubTypes.Type(value = DisconnectRoomParticipant.class, name = "disconnectRoomParticipant"),
        @JsonSubTypes.Type(value = Mute.class, name = "mute"),
        @JsonSubTypes.Type(value = Unmute.class, name = "unmute"),
        @JsonSubTypes.Type(value = MuteAll.class, name = "muteAll"),
        @JsonSubTypes.Type(value = UnmuteAll.class, name = "unmuteAll"),
        @JsonSubTypes.Type(value = SetMicrophoneLevel.class, name = "setMicrophoneLevel"),
        @JsonSubTypes.Type(value = SetPlaybackLevel.class, name = "setPlaybackLevel")
})
@JsonIgnoreProperties("returnClass")
public abstract class AbstractDomainRoomAction
{
        public abstract ConnectorCommand toApi();

        @Override
        public String toString()
        {
                return getClass().getSimpleName();
        }

        public abstract <T  extends AbstractResponse> Class<T> getReturnClass();
}
