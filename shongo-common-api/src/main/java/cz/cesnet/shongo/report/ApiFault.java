package cz.cesnet.shongo.report;

/**
 * Object which implements the {@link ApiFault} represents a XML-RPC fault - a pair of fault code and fault string and
 * it can be serialized to {@link org.apache.xmlrpc.XmlRpcException}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface ApiFault extends SerializableReport
{
    /**
     * @return fault code
     */
    public int getFaultCode();

    /**
     * @return fault string
     */
    public String getFaultString();

    /**
     * @return {@link Exception} for this fault
     */
    public Exception getException();
}
