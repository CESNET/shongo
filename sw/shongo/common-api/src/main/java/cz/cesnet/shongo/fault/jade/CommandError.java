package cz.cesnet.shongo.fault.jade;

import cz.cesnet.shongo.api.CommandException;

/**
 * Represents a {@link CommandFailure} which happens when a process performing the command has
 * thrown an {@link CommandException}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CommandError extends CommandFailure
{
    /**
     * Text message of the error.
     */
    private String message;

    /**
     * Constructor.
     */
    public CommandError()
    {
    }

    /**
     * Constructor.
     *
     * @param message sets the {@link #message}
     */
    public CommandError(String message)
    {
        setMessage(message);
    }

    /**
     * @param message sets the {@link #message}
     */
    public void setMessage(String message)
    {
        this.message = message;
    }

    @Override
    public String getMessage()
    {
        return (message != null ? message : "Unknown error.");
    }
}
