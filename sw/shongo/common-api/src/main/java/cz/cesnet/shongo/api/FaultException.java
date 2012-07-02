package cz.cesnet.shongo.api;

import org.apache.xmlrpc.XmlRpcException;

import static cz.cesnet.shongo.util.ClassHelper.getClassShortName;

/**
 * Fault exception
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class FaultException extends XmlRpcException
{
    public FaultException(int faultCode, String faultString, Object... objects)
    {
        super(faultCode, String.format(faultString, evaluateArguments(objects)));
    }

    public FaultException(Fault fault, Object... objects)
    {
        super(fault.getCode(), String.format(fault.getString(), evaluateArguments(objects)));
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

    private static Object[] evaluateArguments(Object[] objects)
    {
        for (int index = 0; index < objects.length; index++) {
            if (objects[index] instanceof Class) {
                objects[index] = getClassShortName((Class) objects[index]);
            }
        }
        return objects;
    }

    public int getCode()
    {
        return code;
    }
}
