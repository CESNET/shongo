package cz.cesnet.shongo.api.jade;

/**
 * An exception thrown when a command is not supported on a specific device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CommandUnsupportedException extends Exception
{
    public CommandUnsupportedException()
    {
        super();
    }

    /**
     * @param message name of the command
     */
    public CommandUnsupportedException(String message)
    {
        super(message);
    }

    /**
     * @param message name of the command
     * @param cause   cause of the failure
     */
    public CommandUnsupportedException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
