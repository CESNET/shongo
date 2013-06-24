package cz.cesnet.shongo.api;

import jade.content.Concept;
import org.joda.time.DateTime;

/**
 * Represents an active user in a virtual room on a server.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class RoomUser extends IdentifiedComplexType implements Concept
{
    private String roomId;
    private String userId;
    private String displayName = null;
    private DateTime joinTime;
    private RoomLayout layout;
    private boolean audioMuted;
    private boolean videoMuted;
    private int microphoneLevel;

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
     * @return User layout, overriding the room default layout.
     */
    public RoomLayout getLayout()
    {
        return layout;
    }

    /**
     * @param layout User layout, overriding the room default layout.
     */
    public void setLayout(RoomLayout layout)
    {
        this.layout = layout;
    }

    /**
     * @return Is the user audio-muted?
     */
    public boolean getAudioMuted()
    {
        return audioMuted;
    }

    /**
     * @param audioMuted Is the user audio-muted?
     */
    public void setAudioMuted(boolean audioMuted)
    {
        this.audioMuted = audioMuted;
    }

    /**
     * @return Is the user video-muted?
     */
    public boolean getVideoMuted()
    {
        return videoMuted;
    }

    /**
     * @param videoMuted Is the user video-muted?
     */
    public void setVideoMuted(boolean videoMuted)
    {
        this.videoMuted = videoMuted;
    }

    /**
     * @return Microphone level in milli dB, can be negative
     */
    public int getMicrophoneLevel()
    {
        return microphoneLevel;
    }

    /**
     * @param microphoneLevel Microphone level in milli dB, can be negative
     */
    public void setMicrophoneLevel(int microphoneLevel)
    {
        this.microphoneLevel = microphoneLevel;
    }

    public static final String ROOM_ID = "roomId";
    public static final String USER_ID = "userId";
    public static final String DISPLAY_NAME = "displayName";
    public static final String JOIN_TIME = "joinTime";
    public static final String LAYOUT = "layout";
    public static final String AUDIO_MUTED = "audioMuted";
    public static final String VIDEO_MUTED = "videoMuted";
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
        audioMuted = dataMap.getBool(AUDIO_MUTED);
        videoMuted = dataMap.getBool(VIDEO_MUTED);
        microphoneLevel = dataMap.getInt(MICROPHONE_LEVEL);
    }
}
