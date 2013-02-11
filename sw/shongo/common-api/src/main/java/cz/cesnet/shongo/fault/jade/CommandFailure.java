package cz.cesnet.shongo.fault.jade;

import cz.cesnet.shongo.fault.CommonFault;
import jade.content.Concept;

/**
 * @author Martin Srom <martin.srom@cesnet.cz>
 * @see {@link #getMessage()}
 */
public abstract class CommandFailure implements Concept
{
    /**
     * Identifier which can be used to store unique identifier of the {@link CommandFailure}.
     */
    public String id;

    /**
     * Command which failed.
     */
    private String command;

    /**
     * Cause of the failure.
     */
    private Throwable cause;

    /**
     * Constructor.
     */
    public CommandFailure()
    {
    }

    /**
     * @return {@link #command}
     */
    public String getCommand()
    {
        return command;
    }

    /**
     * @param command sets the {@link #command}
     */
    public void setCommand(String command)
    {
        this.command = command;
    }

    /**
     * @return {@link #cause}
     */
    public Throwable getCause()
    {
        return cause;
    }

    /**
     * @param cause sets the {@link #cause}
     */
    public void setCause(Throwable cause)
    {
        this.cause = cause;
    }

    /**
     * @return message of the failure
     */
    public abstract String getMessage();

    /**
     * @return code of the failure
     */
    public int getCode()
    {
        return CommonFault.JADE_COMMAND_UNKNOWN;
    }
}
