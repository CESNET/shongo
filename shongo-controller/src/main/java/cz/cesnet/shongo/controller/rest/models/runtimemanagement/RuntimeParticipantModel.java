package cz.cesnet.shongo.controller.rest.models.runtimemanagement;

import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.api.Alias;
import cz.cesnet.shongo.api.RoomLayout;
import cz.cesnet.shongo.api.RoomParticipant;
import cz.cesnet.shongo.api.UserInformation;
import cz.cesnet.shongo.controller.rest.CacheProvider;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a {@link RoomParticipant}.
 *
 * @author Filip Karnis
 */
@Data
@NoArgsConstructor
public class RuntimeParticipantModel
{

    private String id;
    private String name;
    private String email;
    private String alias;
    private ParticipantRole role;
    private RoomLayout layout;
    private Boolean microphoneEnabled;
    private Integer microphoneLevel;
    private Boolean videoEnabled;
    private Boolean videoSnapshot;

    public RuntimeParticipantModel(RoomParticipant roomParticipant, CacheProvider cacheProvider)
    {
        UserInformation user = null;
        String userId = roomParticipant.getUserId();
        if (userId != null) {
            user = cacheProvider.getUserInformation(userId);
        }

        this.id = roomParticipant.getId();
        this.name = (user != null ? user.getFullName() : roomParticipant.getDisplayName());
        this.email = (user != null ? user.getPrimaryEmail() : null);
        Alias alias = roomParticipant.getAlias();
        this.alias = (alias != null ? alias.getValue() : null);
        this.role = roomParticipant.getRole();
        this.layout = roomParticipant.getLayout();
        this.microphoneEnabled = roomParticipant.getMicrophoneEnabled();
        this.microphoneLevel = roomParticipant.getMicrophoneLevel();
        this.videoEnabled = roomParticipant.getVideoEnabled();
        this.videoSnapshot = roomParticipant.isVideoSnapshot();
    }
}
