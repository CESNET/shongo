package cz.cesnet.shongo.fault.jade;

import cz.cesnet.shongo.fault.CommonFault;
import cz.cesnet.shongo.fault.FaultException;

/**
 * Represents a {@link FaultException} for a {@link CommandFailure}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CommandFailureException extends FaultException
{
    /**
     * {@link CommandFailure} which is the cause
     */
    private CommandFailure commandFailure;

    /**
     * Constructor.
     *
     * @param commandFailure
     */
    public CommandFailureException(CommandFailure commandFailure)
    {
        if (commandFailure == null) {
            throw new IllegalArgumentException("Command failure should not be null.");
        }
        this.commandFailure = commandFailure;
    }

    @Override
    public String getMessage()
    {
        return commandFailure.getMessage();
    }

    @Override
    public int getCode()
    {
        return CommonFault.JADE_COMMAND_UNKNOWN;
    }
}
