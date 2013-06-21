package cz.cesnet.shongo.api.jade;

import cz.cesnet.shongo.api.jade.Command;

/**
 * An exception thrown by invalid {@link Command}s, i.e. {@link Command}s that fail when executed.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CommandException extends Exception
{
    /**
     * Constructor.
     */
    protected CommandException()
    {
    }

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
