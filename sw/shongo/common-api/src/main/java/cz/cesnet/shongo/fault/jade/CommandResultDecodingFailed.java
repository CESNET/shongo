package cz.cesnet.shongo.fault.jade;

import cz.cesnet.shongo.fault.old.CommonFault;

/**
 * Represents a {@link CommandFailure} which happens when a requester is not able to decode the received result
 * for a command.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CommandResultDecodingFailed extends CommandFailure
{
    /**
     * Constructor.
     */
    public CommandResultDecodingFailed()
    {
    }

    /**
     * Constructor.
     *
     * @param throwable
     */
    public CommandResultDecodingFailed(Throwable throwable)
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
        return "Command result decoding failed.";
    }
}
