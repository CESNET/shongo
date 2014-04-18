package cz.cesnet.shongo.connector.api;

/**
 * State of a call.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public enum CallState
{
    /**
     * The remote party is being dialed.
     */
    DIALING,

    /**
     * The remote party is ringing.
     */
    RINGING,

    /**
     * The local device is ringing.
     */
    RING_INCOMING,

    /**
     * The call is connected (i.e., the parties may communicate).
     */
    CONNECTED,

    /**
     * The call is being terminated.
     */
    TERMINATING,

    /**
     * The call got terminated.
     */
    TERMINATED,

    /**
     * Some other, technology specific state. Used when the concrete device state cannot be mapped to any CallState.
     */
    OTHER;

    public boolean isActive()
    {
        switch (this) {
            case DIALING:
                return false;
            case RINGING:
            case RING_INCOMING:
            case CONNECTED:
            case TERMINATING:
            case TERMINATED:
            case OTHER:
                return true;
            default:
                return false;
        }
    }

    public boolean hasConnected()
    {
        switch (this) {
            case DIALING:
            case RINGING:
            case RING_INCOMING:
                return false;
            case CONNECTED:
            case TERMINATING:
            case TERMINATED:
            case OTHER:
                return true;
            default:
                return false;
        }
    }
}
