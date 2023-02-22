package cz.cesnet.shongo.controller.api.domains.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import cz.cesnet.shongo.ParticipantRole;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.*;
import org.joda.time.DateTime;

/**
 * Represents a room participant for open room.
 *
 * @author Ondrej Pavelka <pavelka@cesnet.cz>
 */
public class RoomParticipant
{
    @JsonProperty("id")
    String id;

    @JsonProperty("userId")
    String userId;

    @JsonProperty("role")
    ParticipantRole role;

    @JsonProperty("displayName")
    private String displayName;

    @JsonProperty("joinTime")
    private DateTime joinTime;

    @JsonProperty("layout")
    private RoomLayout layout;

    @JsonProperty("microphoneEnabled")
    private Boolean microphoneEnabled;

    @JsonProperty("microphoneLevel")
    private Integer microphoneLevel;

    @JsonProperty("videoEnabled")
    private Boolean videoEnabled;

    @JsonProperty("videoSnapshot")
    private Boolean videoSnapshot;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    public ParticipantRole getRole()
    {
        return role;
    }

    public void setRole(ParticipantRole role)
    {
        this.role = role;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public DateTime getJoinTime()
    {
        return joinTime;
    }

    public void setJoinTime(DateTime joinTime)
    {
        this.joinTime = joinTime;
    }

    public RoomLayout getLayout()
    {
        return layout;
    }

    public void setLayout(RoomLayout layout)
    {
        this.layout = layout;
    }

    public Boolean getMicrophoneEnabled()
    {
        return microphoneEnabled;
    }

    public void setMicrophoneEnabled(Boolean microphoneEnabled)
    {
        this.microphoneEnabled = microphoneEnabled;
    }

    public Integer getMicrophoneLevel()
    {
        return microphoneLevel;
    }

    public void setMicrophoneLevel(Integer microphoneLevel)
    {
        this.microphoneLevel = microphoneLevel;
    }

    public Boolean getVideoEnabled()
    {
        return videoEnabled;
    }

    public void setVideoEnabled(Boolean videoEnabled)
    {
        this.videoEnabled = videoEnabled;
    }

    public Boolean getVideoSnapshot()
    {
        return videoSnapshot;
    }

    public void setVideoSnapshot(Boolean videoSnapshot)
    {
        this.videoSnapshot = videoSnapshot;
    }

    public static RoomParticipant createFromApi(cz.cesnet.shongo.api.RoomParticipant roomParticipantApi)
    {
        RoomParticipant roomParticipant = new RoomParticipant();
        roomParticipant.setId(roomParticipantApi.getId());
        roomParticipant.setUserId(roomParticipantApi.getUserId());
        roomParticipant.setDisplayName(roomParticipantApi.getDisplayName());
        roomParticipant.setRole(roomParticipantApi.getRole());
        roomParticipant.setJoinTime(roomParticipantApi.getJoinTime());
        roomParticipant.setLayout(roomParticipantApi.getLayout());
        roomParticipant.setMicrophoneEnabled(roomParticipantApi.getMicrophoneEnabled());
        roomParticipant.setMicrophoneLevel(roomParticipantApi.getMicrophoneLevel());
        roomParticipant.setVideoEnabled(roomParticipantApi.getVideoEnabled());
        //TODO:
        roomParticipant.setVideoSnapshot(false);
        return roomParticipant;
    }

    //TODO aliases!
    public cz.cesnet.shongo.api.RoomParticipant toApi()
    {
        cz.cesnet.shongo.api.RoomParticipant roomParticipant = new cz.cesnet.shongo.api.RoomParticipant();
        roomParticipant.setId(getId());
        String userId = getUserId();
        if (UserInformation.isLocal(userId)) {
            roomParticipant.setUserId(userId);
        }
        else {
            throw new TodoImplementException();
        }
        roomParticipant.setDisplayName(getDisplayName());
        roomParticipant.setRole(getRole());
        roomParticipant.setJoinTime(getJoinTime());
        roomParticipant.setLayout(getLayout());
        roomParticipant.setMicrophoneEnabled(getMicrophoneEnabled());
        roomParticipant.setMicrophoneLevel(getMicrophoneLevel());
        roomParticipant.setVideoEnabled(getVideoEnabled());
        roomParticipant.setVideoSnapshot(getVideoSnapshot());
        return roomParticipant;
    }
}
