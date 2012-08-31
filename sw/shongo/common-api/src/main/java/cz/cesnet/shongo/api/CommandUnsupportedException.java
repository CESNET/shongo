package cz.cesnet.shongo.api;

/**
 * An exception thrown when a command is not supported on a specific device.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CommandUnsupportedException extends Exception
{
    public CommandUnsupportedException(String message)
    {
        super(message);
    }

    public CommandUnsupportedException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
