package cz.cesnet.shongo.fault.jade;

/**
 * Represents a {@link CommandFailure} which happens when a requester sends a command which is not implemented by
 * target agent.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CommandNotSupported extends CommandFailure
{
    @Override
    public String getMessage()
    {
        return "Command is not supported.";
    }
}
