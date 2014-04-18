package cz.cesnet.shongo.jade;

import cz.cesnet.shongo.api.jade.CommandException;
import cz.cesnet.shongo.api.jade.Command;

/**
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class UnknownCommandException extends CommandException
{
    /**
     * {@link Command} which was requested but the agent doesn't know it.
     */
    private Command command;

    /**
     * Constructor.
     *
     * @param command sets the {@link #command}
     */
    public UnknownCommandException(Command command)
    {
        this.command = command;
    }

    /**
     * @return {@link #command}
     */
    public Command getCommand()
    {
        return command;
    }
}
