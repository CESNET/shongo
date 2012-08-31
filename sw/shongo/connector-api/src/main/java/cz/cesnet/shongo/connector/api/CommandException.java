package cz.cesnet.shongo.connector.api;

/**
 * An exception thrown by invalid commands, i.e. commands that fail when executed.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CommandException extends Exception
{
    public CommandException(String message)
    {
        super(message);
    }

    public CommandException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
