package cz.cesnet.shongo.fault.jade;

import cz.cesnet.shongo.fault.old.CommonFault;

/**
 * Represents a {@link CommandFailure} which happens when the result for a sent command is not
 * received in specified period of time.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CommandTimeout extends CommandFailure
{
    @Override
    public int getCode()
    {
        return CommonFault.JADE_COMMAND_TIMEOUT;
    }

    @Override
    public String getMessage()
    {
        return "Command doesn't receive a response in specified period of time.";
    }
}
