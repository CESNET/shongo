package cz.cesnet.shongo.api;

import org.joda.time.DateTime;

/**
 * Represents an active participant session in a {@link Room}.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class RoomParticipant extends IdentifiedComplexType
{
    /**
     * {@link Room#id}
     */
    private String roomId;

    /**
     * Shongo-user-id (if available).
     */
    private String userId;

    /**
     * Name of the participant which can be displayed.
     */
    private String displayName;

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
    private Boolean audioMuted;

    /**
     * Specifies whether video is switched off or {@code null} if this option isn't available.
     */
    private Boolean videoMuted;

    /**
     * Specifies whether participant has video snapshot.
     */
    private boolean videoSnapshot;

    /**
     * Specifies whether level of microphone (range 0-100) or {@code null} if this option isn't available.
     */
    private Integer microphoneLevel;

    /**
     * @return Room unique identifier
     */
    public String getRoomId()
    {
        return roomId;
    }

    /**
     * @param roomId Room unique identifier
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
     * @return name of the user displayed to the others (e.g., name of the person, physical room name...)
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * @param displayName name of the user displayed to the others (e.g., name of the person, physical room name...)
     */
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
     * @return {@link #audioMuted}
     */
    public Boolean getAudioMuted()
    {
        return audioMuted;
    }

    /**
     * @param audioMuted sets the {@link #audioMuted}
     */
    public void setAudioMuted(Boolean audioMuted)
    {
        this.audioMuted = audioMuted;
    }

    /**
     * @return {@link #videoMuted}
     */
    public Boolean getVideoMuted()
    {
        return videoMuted;
    }

    /**
     * @param videoMuted sets the {@link #videoMuted}
     */
    public void setVideoMuted(Boolean videoMuted)
    {
        this.videoMuted = videoMuted;
    }

    /**
     * @return {@link #videoSnapshot}
     */
    public boolean isVideoSnapshot()
    {
        return videoSnapshot;
    }

    /**
     * @param videoSnapshot sets the {@link #videoSnapshot}
     */
    public void setVideoSnapshot(boolean videoSnapshot)
    {
        this.videoSnapshot = videoSnapshot;
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
        if (microphoneLevel != null && (microphoneLevel < 0 || microphoneLevel > 100)) {
            throw new IllegalArgumentException("Microphone level " + microphoneLevel + " is out of range 0-100.");
        }
        this.microphoneLevel = microphoneLevel;
    }

    @Override
    public String toString()
    {
        return String.format(RoomParticipant.class.getSimpleName() +
                " (roomId: %s, roomParticipantId: %s, layout: %s, audioMuted: %s, videoMuted: %s, videoSnapshot: %s, microphoneLevel: %d)",
                roomId, id, layout, audioMuted, videoMuted, videoSnapshot, microphoneLevel);
    }

    public static final String ROOM_ID = "roomId";
    public static final String USER_ID = "userId";
    public static final String DISPLAY_NAME = "displayName";
    public static final String JOIN_TIME = "joinTime";
    public static final String LAYOUT = "layout";
    public static final String AUDIO_MUTED = "audioMuted";
    public static final String VIDEO_MUTED = "videoMuted";
    public static final String VIDEO_SNAPSHOT = "videoSnapshot";
    public static final String MICROPHONE_LEVEL = "microphoneLevel";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(ROOM_ID, roomId);
        dataMap.set(USER_ID, userId);
        dataMap.set(DISPLAY_NAME, displayName);
        dataMap.set(JOIN_TIME, joinTime);
        dataMap.set(LAYOUT, layout);
        dataMap.set(AUDIO_MUTED, audioMuted);
        dataMap.set(VIDEO_MUTED, videoMuted);
        dataMap.set(VIDEO_SNAPSHOT, videoSnapshot);
        dataMap.set(MICROPHONE_LEVEL, microphoneLevel);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        roomId = dataMap.getString(ROOM_ID);
        userId = dataMap.getString(USER_ID);
        displayName = dataMap.getString(DISPLAY_NAME);
        joinTime = dataMap.getDateTime(JOIN_TIME);
        layout = dataMap.getEnum(LAYOUT, RoomLayout.class);
        audioMuted = dataMap.getBoolean(AUDIO_MUTED);
        videoMuted = dataMap.getBoolean(VIDEO_MUTED);
        videoSnapshot = dataMap.getBool(VIDEO_SNAPSHOT);
        setMicrophoneLevel(dataMap.getInteger(MICROPHONE_LEVEL));;
    }
}
