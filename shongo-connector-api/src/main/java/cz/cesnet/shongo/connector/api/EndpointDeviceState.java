package cz.cesnet.shongo.connector.api;

import java.util.Map;
import java.util.TreeMap;

/**
 * State of an endpoint device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class EndpointDeviceState
{
    /**
     * Is the device muted? <code>null</code> if the state cannot be determined
     */
    private Boolean muted = null;

    /**
     * Is the device sleeping, i.e., in the standby mode? <code>null</code> if the state cannot be determined
     */
    private Boolean sleeping = null;

    /**
     * Is the device receiving a presentation? <code>null</code> if the state cannot be determined
     */
    private Boolean receivingPresentation = null;

    /**
     * Is the device sending a presentation? <code>null</code> if the state cannot be determined
     */
    private Boolean sendingPresentation = null;

    /**
     * Active calls. Map from call ID to the call state.
     */
    private Map<Integer, CallInfo> calls = new TreeMap<Integer, CallInfo>();


    public Map<Integer, CallInfo> getCalls()
    {
        return calls;
    }

    public void setCalls(Map<Integer, CallInfo> calls)
    {
        this.calls = calls;
    }

    public Boolean getMuted()
    {
        return muted;
    }

    public void setMuted(Boolean muted)
    {
        this.muted = muted;
    }

    public Boolean getReceivingPresentation()
    {
        return receivingPresentation;
    }

    public void setReceivingPresentation(Boolean receivingPresentation)
    {
        this.receivingPresentation = receivingPresentation;
    }

    public Boolean getSendingPresentation()
    {
        return sendingPresentation;
    }

    public void setSendingPresentation(Boolean sendingPresentation)
    {
        this.sendingPresentation = sendingPresentation;
    }

    public Boolean getSleeping()
    {
        return sleeping;
    }

    public void setSleeping(Boolean sleeping)
    {
        this.sleeping = sleeping;
    }
}
