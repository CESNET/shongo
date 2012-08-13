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

    @Override
    public int getCode()
    {
        return CommonFault.TODO_IMPLEMENT;
    }
}
