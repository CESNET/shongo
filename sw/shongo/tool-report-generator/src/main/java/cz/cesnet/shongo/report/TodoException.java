package cz.cesnet.shongo.report;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TodoException extends RuntimeException
{
    public TodoException()
    {
    }

    public TodoException(String message)
    {
        super(message);
    }

    @Override
    public String getMessage()
    {
        return "TODO: " + super.getMessage();
    }
}
