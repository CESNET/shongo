package cz.cesnet.shongo.api.jade;

/**
 * {@link CommandException} with code.
 *
 * @author Ondrej Bouda <ondrej.bouda@cesnet.cz>
 */
public class SimpleCommandException extends CommandException
{
    private String code;

    /**
     * @param message description of the failure
     */
    public SimpleCommandException(String code, String message)
    {
        super(message);
        this.code = code;
    }

    /**
     * @param message description of the failure
     * @param cause   the cause of the failure
     */
    public SimpleCommandException(String code, String message, Throwable cause)
    {
        super(message, cause);
        this.code = code;
    }

    /**
     * @return code of the failure
     */
    public String getCode()
    {
        return code;
    }
}
