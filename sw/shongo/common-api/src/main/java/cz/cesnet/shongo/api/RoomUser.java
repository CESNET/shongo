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

    private boolean muted;
    private int microphoneLevel;
    private int playbackLevel;
    private RoomLayout layout;

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
     * @return Is the user muted?
     */
    public boolean getMuted()
    {
        return muted;
    }

    /**
     * @param muted Is the user muted?
     */
    public void setMuted(boolean muted)
    {
        this.muted = muted;
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
