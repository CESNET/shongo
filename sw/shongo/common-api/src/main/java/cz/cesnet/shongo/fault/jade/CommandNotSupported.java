package cz.cesnet.shongo.fault.jade;

import cz.cesnet.shongo.fault.old.CommonFault;

/**
 * Represents a {@link CommandFailure} which happens when a requester sends a command which is not implemented by
 * target agent.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CommandNotSupported extends CommandFailure
{
    @Override
    public int getCode()
    {
        return CommonFault.JADE_COMMAND_NOT_SUPPORTED;
    }

    @Override
    public String getMessage()
    {
        return "Command is not supported.";
    }
}
