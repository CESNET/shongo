package cz.cesnet.shongo.fault.jade;

import cz.cesnet.shongo.fault.CommonFault;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getMessage()}
 */
public class CommandRefused extends CommandFailure
{
    @Override
    public int getCode()
    {
        return CommonFault.JADE_COMMAND_REFUSED;
    }

    @Override
    public String getMessage()
    {
        return CommonFault.formatMessage("The requested command is unknown to the connector.");
    }
}
