package cz.cesnet.shongo.fault;

/**
 * Exception thrown when some feature isn't implemented yet.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TodoImplementException extends RuntimeException implements Fault
{
    /**
     * Constructor.
     */
    public TodoImplementException()
    {
        this("TODO: Implement");
    }

    /**
     * Constructor.
     *
     * @param description description what should be implemented
     */
    public TodoImplementException(String description)
    {
        super(String.format("TODO: Implement %s", description));
    }

    /**
     * Constructor.
     *
     * @param format  format for description what should be implemented
     * @param objects parameters for format
     */
    public TodoImplementException(String format, Object... objects)
    {
        this(String.format(format, objects));
    }

    @Override
    public int getCode()
    {
        return CommonFault.TODO_IMPLEMENT;
    }
}
