package cz.cesnet.shongo.api;

import cz.cesnet.shongo.ParticipantRole;
import org.joda.time.DateTime;

/**
 * Represents an active participant session in a {@link Room}.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class RoomParticipant extends IdentifiedComplexType
{
    public static final Integer DEFAULT_MICROPHONE_LEVEL = 0;

    /**
     * {@link Room#id}
     */
    private String roomId;

    /**
     * Shongo-user-id (if available).
     */
    private String userId;

    /**
     * {@link Alias} which uses the user for connecting.
     */
    private Alias alias;

    /**
     * Name of the user displayed to the others (e.g., name of the person, physical room name...).
     */
    private String displayName;

    /**
     * Role of the participant.
     */
    private ParticipantRole role;

    /**
     * Date/time when the participants joined to the {@link Room} (started the current session).
     */
    private DateTime joinTime;

    /**
     * Overrides the default {@link RoomLayout} or {@code null} if this option isn't available.
     */
    private RoomLayout layout;

    /**
     * Specifies whether video is switched off or {@code null} if this option isn't available.
     */
    private Boolean microphoneEnabled;

    /**
     * Specifies whether level of microphone (range 0-100) or {@code null} if this option isn't available.
     */
    private Integer microphoneLevel;

    /**
     * Specifies whether video is switched off or {@code null} if this option isn't available.
     */
    private Boolean videoEnabled;

    /**
     * Specifies whether participant has video snapshot.
     */
    private Boolean videoSnapshot;

    private String protocol;

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Constructor.
     */
    public RoomParticipant()
    {
    }

    /**
     * Constructor.
     *
     * @param id sets the {@link #id}
     */
    public RoomParticipant(String id)
    {
        this.id = id;
    }

    /**
     * @return {@link #roomId}
     */
    public String getRoomId()
    {
        return roomId;
    }

    /**
     * @param roomId sets the {@link #roomId}
     */
    public void setRoomId(String roomId)
    {
        this.roomId = roomId;
    }

    /**
     * @return the shongo user-id or null
     */
    public String getUserId()
    {
        return userId;
    }

    /**
     * @param userId sets the shongo user-id
     */
    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    /**
     * @return {@link #alias}
     */
    public Alias getAlias()
    {
        return alias;
    }

    /**
     * @param alias sets the {@link #alias}
     */
    public void setAlias(Alias alias)
    {
        this.alias = alias;
    }

    /**
     * @return {@link #displayName}
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * @param displayName sets the {@link #displayName}
     */
    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    /**
     * @return {@link #role}
     */
    public ParticipantRole getRole()
    {
        return role;
    }

    /**
     * @param role sets the {@link #role}
     */
    public void setRole(ParticipantRole role)
    {
        this.role = role;
    }

    public DateTime getJoinTime()
    {
        return joinTime;
    }

    public void setJoinTime(DateTime joinTime)
    {
        this.joinTime = joinTime;
    }

    /**
     * @return {@link #layout}
     */
    public RoomLayout getLayout()
    {
        return layout;
    }

    /**
     * @param layout sets the {@link #layout}
     */
    public void setLayout(RoomLayout layout)
    {
        this.layout = layout;
    }

    /**
     * @return {@link #microphoneEnabled}
     */
    public Boolean getMicrophoneEnabled()
    {
        return microphoneEnabled;
    }

    /**
     * @param microphoneEnabled sets the {@link #microphoneEnabled}
     */
    public void setMicrophoneEnabled(Boolean microphoneEnabled)
    {
        this.microphoneEnabled = microphoneEnabled;
    }

    /**
     * @return {@link #microphoneLevel}
     */
    public Integer getMicrophoneLevel()
    {
        return microphoneLevel;
    }

    /**
     * @param microphoneLevel sets the {@link #microphoneLevel}
     */
    public void setMicrophoneLevel(Integer microphoneLevel)
    {
        if (microphoneLevel != null && !microphoneLevel.equals(DEFAULT_MICROPHONE_LEVEL)
                && (microphoneLevel < 1 || microphoneLevel > 10)) {
            throw new IllegalArgumentException("Microphone level " + microphoneLevel + " is out of range 1-10.");
        }
        this.microphoneLevel = microphoneLevel;
    }

    /**
     * @return {@link #videoEnabled}
     */
    public Boolean getVideoEnabled()
    {
        return videoEnabled;
    }

    /**
     * @param videoEnabled sets the {@link #videoEnabled}
     */
    public void setVideoEnabled(Boolean videoEnabled)
    {
        this.videoEnabled = videoEnabled;
    }

    /**
     * @return {@link #videoSnapshot}
     */
    public boolean isVideoSnapshot()
    {
        return Boolean.TRUE.equals(videoSnapshot);
    }

    /**
     * @param videoSnapshot sets the {@link #videoSnapshot}
     */
    public void setVideoSnapshot(Boolean videoSnapshot)
    {
        this.videoSnapshot = videoSnapshot;
    }

    /**
     * @param roomParticipant
     * @return true when this {@link cz.cesnet.shongo.api.RoomParticipant} is same as given {@code roomParticipant},
     * false otherwise
     */
    public boolean isSame(RoomParticipant roomParticipant)
    {
        if (roomParticipant == null) {
            return false;
        }

        if (id != null ? !id.equals(roomParticipant.id) : roomParticipant.id != null) {
            return false;
        }
        if (alias != null ? !alias.equals(roomParticipant.alias) : roomParticipant.alias != null) {
            return false;
        }
        if (displayName != null ? !displayName.equals(
                roomParticipant.displayName) : roomParticipant.displayName != null) {
            return false;
        }
        if (joinTime != null ? !joinTime.equals(roomParticipant.joinTime) : roomParticipant.joinTime != null) {
            return false;
        }
        if (layout != roomParticipant.layout) {
            return false;
        }
        if (microphoneEnabled != null ? !microphoneEnabled.equals(
                roomParticipant.microphoneEnabled) : roomParticipant.microphoneEnabled != null) {
            return false;
        }
        if (microphoneLevel != null ? !microphoneLevel.equals(
                roomParticipant.microphoneLevel) : roomParticipant.microphoneLevel != null) {
            return false;
        }
        if (role != roomParticipant.role) {
            return false;
        }
        if (roomId != null ? !roomId.equals(roomParticipant.roomId) : roomParticipant.roomId != null) {
            return false;
        }
        if (userId != null ? !userId.equals(roomParticipant.userId) : roomParticipant.userId != null) {
            return false;
        }
        if (videoEnabled != null ? !videoEnabled.equals(
                roomParticipant.videoEnabled) : roomParticipant.videoEnabled != null) {
            return false;
        }
        if (videoSnapshot != null ? !videoSnapshot.equals(
                roomParticipant.videoSnapshot) : roomParticipant.videoSnapshot != null) {
            return false;
        }

        if (protocol != null ? !protocol.equals(
                roomParticipant.protocol) : roomParticipant.protocol != null) {
            return false;
        }

        return true;
    }

    @Override
    public String toString()
    {
        return String.format(RoomParticipant.class.getSimpleName() +
                " (roomId: %s, roomParticipantId: %s, name: %s, layout: %s, microphoneEnabled: %s, microphoneLevel: %d, videoEnabled: %s, videoSnapshot: %s)",
                roomId, id, displayName, layout, microphoneEnabled, microphoneLevel, videoEnabled, videoSnapshot);
    }

    public static final String ROOM_ID = "roomId";
    public static final String USER_ID = "userId";
    public static final String ALIAS = "alias";
    public static final String DISPLAY_NAME = "displayName";
    public static final String ROLE = "role";
    public static final String JOIN_TIME = "joinTime";
    public static final String LAYOUT = "layout";
    public static final String MICROPHONE_ENABLED = "microphoneEnabled";
    public static final String MICROPHONE_LEVEL = "microphoneLevel";
    public static final String VIDEO_ENABLED = "videoEnabled";
    public static final String VIDEO_SNAPSHOT = "videoSnapshot";
    public static final String PROTOCOL = "protocol";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(ROOM_ID, roomId);
        dataMap.set(USER_ID, userId);
        dataMap.set(ALIAS, alias);
        dataMap.set(DISPLAY_NAME, displayName);
        dataMap.set(ROLE, role);
        dataMap.set(JOIN_TIME, joinTime);
        dataMap.set(LAYOUT, layout);
        dataMap.set(MICROPHONE_ENABLED, microphoneEnabled);
        dataMap.set(MICROPHONE_LEVEL, microphoneLevel);
        dataMap.set(VIDEO_ENABLED, videoEnabled);
        dataMap.set(VIDEO_SNAPSHOT, videoSnapshot);
        dataMap.set(PROTOCOL, protocol);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        roomId = dataMap.getString(ROOM_ID);
        userId = dataMap.getString(USER_ID);
        alias = dataMap.getComplexType(ALIAS, Alias.class);
        displayName = dataMap.getString(DISPLAY_NAME);
        role = dataMap.getEnum(ROLE, ParticipantRole.class);
        joinTime = dataMap.getDateTime(JOIN_TIME);
        layout = dataMap.getEnum(LAYOUT, RoomLayout.class);
        microphoneEnabled = dataMap.getBoolean(MICROPHONE_ENABLED);
        setMicrophoneLevel(dataMap.getInteger(MICROPHONE_LEVEL));
        ;
        videoEnabled = dataMap.getBoolean(VIDEO_ENABLED);
        videoSnapshot = dataMap.getBool(VIDEO_SNAPSHOT);
        protocol = dataMap.getString(PROTOCOL);
    }
}