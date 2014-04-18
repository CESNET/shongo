package cz.cesnet.shongo.connector.api;

/**
 * Usage stats of a given multipoint device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class UsageStats
{
    private byte[] callLog;

    /**
     * @return Call log in the CDR format.
     */
    public byte[] getCallLog()
    {
        return callLog;
    }

    /**
     * @param callLog Call log in the CDR format.
     */
    public void setCallLog(byte[] callLog)
    {
        this.callLog = callLog;
    }
}
