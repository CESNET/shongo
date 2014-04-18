package cz.cesnet.shongo.api.jade;

/**
 * An exception thrown when agent is disabled for the command (it should be report as agent-not-found)
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class CommandDisabledException extends RuntimeException
{
    public CommandDisabledException()
    {
        super();
    }

    @Override
    public String getMessage()
    {
        return "disabled";
    }
}
