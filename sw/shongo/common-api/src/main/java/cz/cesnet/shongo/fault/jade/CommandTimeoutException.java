package cz.cesnet.shongo.fault.jade;

import cz.cesnet.shongo.fault.CommonFault;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getMessage()}
 */
public class CommandTimeoutException extends CommandFailureException
{
    @Override
    public int getCode()
    {
        return CommonFault.JADE_COMMAND_TIMEOUT;
    }

    @Override
    public String getMessage()
    {
        return CommonFault.formatMessage("Jade command doesn't receive a response in specified period of time.");
    }
}
