package cz.cesnet.shongo.fault.old;

import cz.cesnet.shongo.fault.Fault;

/**
 * Exception that represents and implements {@link cz.cesnet.shongo.fault.Fault}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class OldFaultException extends Exception implements Fault
{
    /**
     * Fault code.
     */
    int code;

    /**
     * Constructor.
     */
    public OldFaultException()
    {
        this.code = CommonFault.UNKNOWN;
    }

    /**
     * Constructor.
     *
     * @param code
     * @param message
     */
    public OldFaultException(int code, String message)
    {
        super(message);
        this.code = code;
    }

    /**
     * Constructor.
     *
     * @param code
     * @param message
     */
    public OldFaultException(int code, String message, Throwable throwable)
    {
        super(message, throwable);
        this.code = code;
    }

    /**
     * Constructor.
     *
     * @param code
     * @param throwable
     */
    public OldFaultException(int code, Throwable throwable)
    {
        super(throwable);
        this.code = code;
    }

    /**
     * Constructor.
     *
     * @param code
     * @param message
     * @param objects
     */
    public OldFaultException(int code, String message, Object... objects)
    {
        this(code, CommonFault.formatMessage(message, objects));
    }

    /**
     * Constructor.
     *
     * @param fault
     * @param objects
     */
    public OldFaultException(Fault fault, Object... objects)
    {
        this(fault.getCode(), CommonFault.formatMessage(fault.getMessage(), objects));
    }

    /**
     * Constructor.
     *
     * @param throwable
     * @param fault
     * @param objects
     */
    public OldFaultException(Throwable throwable, Fault fault, Object... objects)
    {
        this(fault.getCode(), CommonFault.formatMessage(fault.getMessage(), objects), throwable);
    }

    /**
     * Constructor.
     *
     * @param throwable
     * @param message
     * @param objects
     */
    public OldFaultException(Throwable throwable, String message, Object... objects)
    {
        this(CommonFault.UNKNOWN, CommonFault.formatMessage(message, objects), throwable);
    }

    /**
     * Constructor.
     *
     * @param faultString
     * @param objects
     */
    public OldFaultException(String faultString, Object... objects)
    {
        this(CommonFault.UNKNOWN, faultString, objects);
    }

    /**
     * Construct unknown fault by description string.
     *
     * @param faultString
     */
    public OldFaultException(String faultString)
    {
        this(CommonFault.UNKNOWN, faultString);
    }

    /**
     * Construct unknown fault by {@code throwable}.
     *
     * @param throwable
     */
    public OldFaultException(Throwable throwable)
    {
        this(CommonFault.UNKNOWN, throwable);
    }

    @Override
    public int getCode()
    {
        return code;
    }

    @Override
    public Exception createException()
    {
        throw new RuntimeException("TODO: Implement OldFaultException.getExceptionClass");
    }

    /**
     * @param code sets the {@link #code}
     */
    public void setCode(int code)
    {
        this.code = code;
    }
}
