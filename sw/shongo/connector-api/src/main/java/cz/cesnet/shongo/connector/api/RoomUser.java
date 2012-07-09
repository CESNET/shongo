package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.controller.common.AbsoluteDateTimeSpecification;

/**
 * Represents an active user in a virtual room on a server.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class RoomUser
{
    private String userId;
    private String roomId;
    private UserIdentity userIdentity;
    private AbsoluteDateTimeSpecification joinTime;
    private boolean muted;
    private int microphoneLevel;
    private int playbackLevel;
    private RoomLayout layout;

    public AbsoluteDateTimeSpecification getJoinTime()
    {
        return joinTime;
    }

    public void setJoinTime(AbsoluteDateTimeSpecification joinTime)
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
     * @param layout    User layout, overriding the room default layout.
     */
    public void setLayout(RoomLayout layout)
    {
        this.layout = layout;
    }

    /**
     * @return Microphone level
     */
    public int getMicrophoneLevel()
    {
        return microphoneLevel;
    }

    /**
     * @param microphoneLevel    Microphone level
     */
    public void setMicrophoneLevel(int microphoneLevel)
    {
        this.microphoneLevel = microphoneLevel;
    }

    /**
     * @return Is the user muted?
     */
    public boolean isMuted()
    {
        return muted;
    }

    /**
     * @param muted    Is the user muted?
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
     * @param playbackLevel    Playback level (speakers volume)
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
     * @param roomId    Room unique identifier
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
     * @param userId    User identification in room (technology specific).
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
     * @param userIdentity    User identity
     */
    public void setUserIdentity(UserIdentity userIdentity)
    {
        this.userIdentity = userIdentity;
    }
}
