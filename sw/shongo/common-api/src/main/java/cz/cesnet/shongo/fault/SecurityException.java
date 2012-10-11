package cz.cesnet.shongo.fault;

/**
 * Exception thrown when some security error need to be reported.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class SecurityException extends RuntimeException implements Fault
{
    /**
     * Constructor.
     *
     * @param description description
     */
    public SecurityException(String description)
    {
        super(String.format("Security failure: %s", description));
    }

    /**
     * Constructor.
     *
     * @param format  format for description
     * @param objects parameters for format
     */
    public SecurityException(String format, Object... objects)
    {
        this(String.format(format, objects));
    }

    @Override
    public int getCode()
    {
        return CommonFault.SECURITY_UNKNOWN;
    }
}
