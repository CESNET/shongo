package cz.cesnet.shongo.fault.jade;

import cz.cesnet.shongo.fault.CommonFault;
import cz.cesnet.shongo.fault.FaultException;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getMessage()}
 */
public class CommandFailureException extends FaultException
{
    /**
     * Constructor.
     */
    public CommandFailureException()
    {
    }

    /**
     * Constructor.
     */
    public CommandFailureException(String message)
    {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param throwable
     */
    public CommandFailureException(Throwable throwable)
    {
        super(throwable);
    }

    @Override
    public int getCode()
    {
        return CommonFault.JADE_COMMAND_UNKNOWN;
    }
}
