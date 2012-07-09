package cz.cesnet.shongo.controller.api;

import org.apache.xmlrpc.XmlRpcException;

import static cz.cesnet.shongo.controller.api.util.ClassHelper.getClassShortName;

/**
 * Fault exception
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class FaultException extends XmlRpcException
{
    /**
     * Construct exception from known fault described by given fault and parameters.
     *
     * @param fault
     * @param objects
     */
    public FaultException(Fault fault, Object... objects)
    {
        super(fault.getCode(), String.format(fault.getString(), evaluateParameters(objects)));
    }

    /**
     * Construct exception from know fault described by give fault and parameters and another exception
     * which cause the fault.
     *
     * @param exception
     * @param fault
     * @param objects
     */
    public FaultException(Exception exception, Fault fault, Object... objects)
    {
        super(fault.getCode(), String.format(fault.getString(), evaluateParameters(objects)), exception);
    }

    /**
     * Construct unknown fault by another exception which case the fault and a description string.
     *
     * @param exception
     * @param faultString
     */
    public FaultException(Exception exception, String faultString, Object... objects)
    {
        super(Fault.Common.UNKNOWN_FAULT.getCode(), String.format(Fault.Common.UNKNOWN_FAULT.getString(),
                String.format(faultString, evaluateParameters(objects))), exception);
    }

    /**
     * Construct unknown fault by description string.
     *
     * @param faultString
     */
    public FaultException(String faultString)
    {
        super(Fault.Common.UNKNOWN_FAULT.getCode(), faultString);
    }

    /**
     * Evaluate all given parameters (e.g., classes to class names).
     *
     * @param objects
     * @return array of evaluated parameters
     */
    private static Object[] evaluateParameters(Object[] objects)
    {
        for (int index = 0; index < objects.length; index++) {
            if (objects[index] instanceof Class) {
                objects[index] = getClassShortName((Class) objects[index]);
            }
        }
        return objects;
    }

    /**
     * @return fault exception code.
     */
    public int getCode()
    {
        return code;
    }
}
