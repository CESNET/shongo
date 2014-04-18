package cz.cesnet.shongo.jade;

/**
 * An exception thrown during processing of {@link LocalCommand} by the {@link LocalCommandBehaviour}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class LocalCommandException extends Exception
{
    /**
     * @param message description of the failure
     */
    public LocalCommandException(String message)
    {
        super(message);
    }

    /**
     * @param message description of the failure
     * @param cause   the cause of the failure
     */
    public LocalCommandException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
