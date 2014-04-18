package cz.cesnet.shongo.generator;

/**
 * Generator exception.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class GeneratorException extends RuntimeException
{
    public GeneratorException()
    {
    }

    public GeneratorException(String message, Object... objects)
    {
        super(String.format(message, objects));
    }
}
