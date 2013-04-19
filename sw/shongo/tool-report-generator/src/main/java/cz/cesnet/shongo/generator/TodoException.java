package cz.cesnet.shongo.generator;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TodoException extends GeneratorException
{
    public TodoException()
    {
    }

    public TodoException(String message, Object... objects)
    {
        super(message, objects);
    }

    @Override
    public String getMessage()
    {
        return "TODO: " + super.getMessage();
    }
}
