package cz.cesnet.shongo.tests.jade_ontologies.ontology;

import jade.content.Concept;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class RoomUser implements Concept {
    private String userId;
    private String roomId;
    private UserIdentity userIdentity;
    private boolean muted;
    private int microphoneLevel;
    private int playbackLevel;

    public int getMicrophoneLevel()
    {
        return microphoneLevel;
    }

    public void setMicrophoneLevel(int microphoneLevel)
    {
        this.microphoneLevel = microphoneLevel;
    }

    public boolean isMuted()
    {
        return muted;
    }

    public void setMuted(boolean muted)
    {
        this.muted = muted;
    }

    public int getPlaybackLevel()
    {
        return playbackLevel;
    }

    public void setPlaybackLevel(int playbackLevel)
    {
        this.playbackLevel = playbackLevel;
    }

    public String getRoomId()
    {
        return roomId;
    }

    public void setRoomId(String roomId)
    {
        this.roomId = roomId;
    }

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    public UserIdentity getUserIdentity()
    {
        return userIdentity;
    }

    public void setUserIdentity(UserIdentity userIdentity)
    {
        this.userIdentity = userIdentity;
    }
}
