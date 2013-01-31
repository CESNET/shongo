package cz.cesnet.shongo.fault.jade;

import cz.cesnet.shongo.fault.CommonFault;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getMessage()}
 */
public class CommandResultDecoding extends CommandFailure
{
    /**
     * Constructor.
     *
     * @param throwable
     */
    public CommandResultDecoding(Throwable throwable)
    {
        setCause(throwable);
    }

    @Override
    public int getCode()
    {
        return CommonFault.JADE_COMMAND_RESULT_DECODING;
    }

    @Override
    public String getMessage()
    {
        return CommonFault.formatMessage("The result of the command could not be decoded.");
    }
}
