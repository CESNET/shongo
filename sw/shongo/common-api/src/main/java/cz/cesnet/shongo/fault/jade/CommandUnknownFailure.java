package cz.cesnet.shongo.fault.jade;

/**
 * Represents a {@link CommandFailure} which happens for a not specified reason.
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
    public String getMessage()
    {
        return (message != null ? message : "Unknown failure.");
    }


}
