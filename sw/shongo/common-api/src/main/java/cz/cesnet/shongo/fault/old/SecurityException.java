package cz.cesnet.shongo.fault.old;

import cz.cesnet.shongo.fault.Fault;

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

    /**
     * Constructor.
     *
     * @param format  format for description
     * @param objects parameters for format
     */
    public SecurityException(Throwable throwable, String format, Object... objects)
    {
        super(String.format(format, objects), throwable);
    }

    @Override
    public int getCode()
    {
        return CommonFault.SECURITY_UNKNOWN;
    }
}
