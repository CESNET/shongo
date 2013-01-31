package cz.cesnet.shongo.fault.jade;

import cz.cesnet.shongo.fault.CommonFault;

/**
 * Unknown {@link CommandFailure}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class CommandUnknownFailure extends CommandFailure
{
    /**
     * Text message of the unknown failure.
     */
    private String message;

    /**
     * Constructor.
     */
    public CommandUnknownFailure()
    {
    }

    /**
     * Constructor.
     *
     * @param message sets the {@link #message}
     */
    public CommandUnknownFailure(String message)
    {
        setMessage(message);
    }

    /**
     * @param message sets the {@link #message}
     */
    public void setMessage(String message)
    {
        this.message = message;
    }

    @Override
    public int getCode()
    {
        return CommonFault.JADE_COMMAND_REFUSED;
    }

    @Override
    public String getMessage()
    {
        return (message != null ? message : "Unknown failure.");
    }


}
