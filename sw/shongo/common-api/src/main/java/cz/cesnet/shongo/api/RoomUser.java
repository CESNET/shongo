package cz.cesnet.shongo.api;

import cz.cesnet.shongo.api.xmlrpc.StructType;
import jade.content.Concept;
import org.joda.time.DateTime;

/**
 * Represents an active user in a virtual room on a server.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class RoomUser implements StructType, Concept
{
    private String userId;
    private String roomId;
    private UserIdentity userIdentity;
    private String displayName = null;

    private DateTime joinTime;

    private boolean audioMuted;
    private boolean videoMuted;
    private int microphoneLevel;
    private int playbackLevel;
    private RoomLayout layout;


    /**
     * RoomUser attribute names.
     * <p/>
     * Suitable for modifyParticipant() command.
     * <p/>
     * NOTE: Keep in sync with actual attributes of the class.
     */

    /** Display name. Type: String */
    public static final String DISPLAY_NAME = "displayName";
    /** Is the user audio-muted? Type: boolean */
    public static final String AUDIO_MUTED = "audioMuted";
    /** Is the user video-muted? Type: boolean */
    public static final String VIDEO_MUTED = "videoMuted";
    /** Microphone level. Type: int */
    public static final String MICROPHONE_LEVEL = "microphoneLevel";
    /** Playback level. Type: int */
    public static final String PLAYBACK_LEVEL = "playbackLevel";
    /** Layout of the virtual room. Type: RoomLayout */
    public static final String LAYOUT = "layout";


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
     * @return Playback level (speakers volume)
     */
    public int getPlaybackLevel()
    {
        return playbackLevel;
    }

    /**
     * @param playbackLevel Playback level (speakers volume)
     */
    public void setPlaybackLevel(int playbackLevel)
    {
        this.playbackLevel = playbackLevel;
    }

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
     * @return User identification in room (technology specific).
     */
    public String getUserId()
    {
        return userId;
    }

    /**
     * @param userId User identification in room (technology specific).
     */
    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    /**
     * @return User identity (may be NULL, e.g., when the user is calling from a cell phone)
     */
    public UserIdentity getUserIdentity()
    {
        return userIdentity;
    }

    /**
     * @param userIdentity User identity
     */
    public void setUserIdentity(UserIdentity userIdentity)
    {
        this.userIdentity = userIdentity;
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

}
