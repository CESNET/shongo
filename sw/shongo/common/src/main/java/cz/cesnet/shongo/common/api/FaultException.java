package cz.cesnet.shongo.common.api;

import org.apache.xmlrpc.XmlRpcException;

/**
 * Fault exception
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class FaultException extends XmlRpcException
{
    public FaultException(int faultCode, String faultString, Object... objects)
    {
        super(faultCode, String.format(faultString, objects));
    }

    public FaultException(Fault fault, Object... objects)
    {
        super(fault.getCode(), String.format(fault.getString(), objects));
    }

    public FaultException(Exception exception, String faultString)
    {
        super(Fault.Common.UNKNOWN_FAULT.getCode(), String.format(Fault.Common.UNKNOWN_FAULT.getString(), faultString),
                exception);
    }

    public FaultException(Exception exception)
    {
        this(exception, exception.getMessage());
    }
}
