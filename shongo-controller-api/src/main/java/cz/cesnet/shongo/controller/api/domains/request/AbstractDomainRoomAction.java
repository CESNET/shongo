package cz.cesnet.shongo.controller.api.domains.request;

import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

/**
 * Represents an action for foreign virtual room.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "action")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DisconnectRoomParticipant.class, name = "disconnectRoomParticipant"),
        @JsonSubTypes.Type(value = Mute.class, name = "mute"),
        @JsonSubTypes.Type(value = Unmute.class, name = "unmute"),
        @JsonSubTypes.Type(value = MuteAll.class, name = "muteAll"),
        @JsonSubTypes.Type(value = UnmuteAll.class, name = "unmuteAll"),
        @JsonSubTypes.Type(value = SetMicrophoneLevel.class, name = "setMicrophoneLevel"),
        @JsonSubTypes.Type(value = SetPlaybackLevel.class, name = "setPlaybackLevel")
})
public abstract class AbstractDomainRoomAction
{
}
