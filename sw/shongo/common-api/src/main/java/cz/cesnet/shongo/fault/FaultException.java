package cz.cesnet.shongo.fault;

/**
 * Exception that represents and implements {@link Fault}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class FaultException extends Exception implements Fault, SerializableException
{
    /**
     * Fault code.
     */
    int code;

    /**
     * Constructor.
     */
    public FaultException()
    {
        this.code = CommonFault.UNKNOWN;
    }

    /**
     * Constructor.
     *
     * @param code
     * @param message
     */
    public FaultException(int code, String message)
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
    public FaultException(int code, String message, Throwable throwable)
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
    public FaultException(int code, Throwable throwable)
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
    public FaultException(int code, String message, Object... objects)
    {
        this(code, CommonFault.formatMessage(message, objects));
    }

    /**
     * Constructor.
     *
     * @param fault
     * @param objects
     */
    public FaultException(Fault fault, Object... objects)
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
    public FaultException(Throwable throwable, Fault fault, Object... objects)
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
    public FaultException(Throwable throwable, String message, Object... objects)
    {
        this(CommonFault.UNKNOWN, CommonFault.formatMessage(message, objects), throwable);
    }

    /**
     * Constructor.
     *
     * @param faultString
     * @param objects
     */
    public FaultException(String faultString, Object... objects)
    {
        this(CommonFault.UNKNOWN, faultString, objects);
    }

    /**
     * Construct unknown fault by description string.
     *
     * @param faultString
     */
    public FaultException(String faultString)
    {
        this(CommonFault.UNKNOWN, faultString);
    }

    @Override
    public int getCode()
    {
        return code;
    }

    /**
     * @param code sets the {@link #code}
     */
    public void setCode(int code)
    {
        this.code = code;
    }
}
