package cz.cesnet.shongo.api;

/**
 * An exception thrown by invalid commands, i.e. commands that fail when executed.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CommandException extends Exception
{
    /**
     * @param message description of the failure
     */
    public CommandException(String message)
    {
        super(message);
    }

    /**
     * @param message description of the failure
     * @param cause   the cause of the failure
     */
    public CommandException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
