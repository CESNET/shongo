package cz.cesnet.shongo.connector.api;

import cz.cesnet.shongo.Technology;
import cz.cesnet.shongo.api.Alias;

import java.util.Date;

/**
 * Information about a call, outgoing or incoming. Used for device states.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CallInfo
{
    /**
     * ID of the call on the device.
     */
    private int callId;

    /**
     * ID of the conference the call belongs to. <code>null</code> if the call does not belong to any conference
     */
    private Integer conferenceId = null;

    /**
     * Alias of the multipoint with which the device is connected. <code>null</code> if it cannot be determined
     */
    private Alias remoteAlias = null;

    /**
     * Technology used for the call. <code>null</code> if it cannot be determined
     */
    private Technology technology = null;

    /**
     * Is the call incoming (i.e., the device was called by someone else)? <code>null</code> if it cannot be determined
     */
    private Boolean incoming = null;

    /**
     * Does the call contain audio channel? <code>null</code> if it cannot be determined
     */
    private Boolean audioContained = null;

    /**
     * Does the call contain video channel? <code>null</code> if it cannot be determined
     */
    private Boolean videoContained = null;

    /**
     * Time when the call started. <code>null</code> if it cannot be determined
     */
    private Date startTime = null;

    /**
     * State of the call. <code>null</code> if it cannot be determined
     */
    private CallState state = null;


    public Boolean getAudioContained()
    {
        return audioContained;
    }

    public void setAudioContained(Boolean audioContained)
    {
        this.audioContained = audioContained;
    }

    public int getCallId()
    {
        return callId;
    }

    public void setCallId(int callId)
    {
        this.callId = callId;
    }

    public Integer getConferenceId()
    {
        return conferenceId;
    }

    public void setConferenceId(Integer conferenceId)
    {
        this.conferenceId = conferenceId;
    }

    public Boolean getIncoming()
    {
        return incoming;
    }

    public void setIncoming(Boolean incoming)
    {
        this.incoming = incoming;
    }

    public Alias getRemoteAlias()
    {
        return remoteAlias;
    }

    public void setRemoteAlias(Alias remoteAlias)
    {
        this.remoteAlias = remoteAlias;
    }

    public Date getStartTime()
    {
        return startTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    public CallState getState()
    {
        return state;
    }

    public void setState(CallState state)
    {
        this.state = state;
    }

    public Technology getTechnology()
    {
        return technology;
    }

    public void setTechnology(Technology technology)
    {
        this.technology = technology;
    }

    public Boolean getVideoContained()
    {
        return videoContained;
    }

    public void setVideoContained(Boolean videoContained)
    {
        this.videoContained = videoContained;
    }

    @Override
    public String toString()
    {
        return String.format(
                "%s call #%d, conference #%s, %s (remote party: %s, using %s, audio %s, video %s, started at %s)",
                (incoming == null ? "unknown" : (incoming ? "incoming" : "outgoing")),
                callId,
                (conferenceId == null ? "?" : String.valueOf(conferenceId)),
                (state == null ? "unknown state" : state),
                (remoteAlias == null ? "?" : remoteAlias),
                (technology == null ? "?" : technology),
                (audioContained == null ? "?" : (audioContained ? "on" : "off")),
                (videoContained == null ? "?" : (videoContained ? "on" : "off")),
                (startTime == null ? "?" : startTime)
        );
    }
}
