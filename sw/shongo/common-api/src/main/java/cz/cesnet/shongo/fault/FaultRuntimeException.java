package cz.cesnet.shongo.fault;

/**
 * Exception that represents and implements {@link cz.cesnet.shongo.fault.Fault}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class FaultRuntimeException extends RuntimeException implements Fault
{
    /**
     * Fault code.
     */
    int code;

    /**
     * Constructor.
     */
    public FaultRuntimeException()
    {
        this.code = CommonFault.UNKNOWN;
    }

    /**
     * Constructor.
     *
     * @param code
     * @param message
     */
    public FaultRuntimeException(int code, String message)
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
    public FaultRuntimeException(int code, String message, Throwable throwable)
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
    public FaultRuntimeException(int code, Throwable throwable)
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
    public FaultRuntimeException(int code, String message, Object... objects)
    {
        this(code, CommonFault.formatMessage(message, objects));
    }

    /**
     * Constructor.
     *
     * @param fault
     * @param objects
     */
    public FaultRuntimeException(Fault fault, Object... objects)
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
    public FaultRuntimeException(Throwable throwable, Fault fault, Object... objects)
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
    public FaultRuntimeException(Throwable throwable, String message, Object... objects)
    {
        this(CommonFault.UNKNOWN, CommonFault.formatMessage(message, objects), throwable);
    }

    /**
     * Constructor.
     *
     * @param faultString
     * @param objects
     */
    public FaultRuntimeException(String faultString, Object... objects)
    {
        this(CommonFault.UNKNOWN, faultString, objects);
    }

    /**
     * Construct unknown fault by description string.
     *
     * @param faultString
     */
    public FaultRuntimeException(String faultString)
    {
        this(CommonFault.UNKNOWN, faultString);
    }

    /**
     * Construct unknown fault by {@code throwable}.
     *
     * @param throwable
     */
    public FaultRuntimeException(Throwable throwable)
    {
        this(CommonFault.UNKNOWN, throwable);
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
