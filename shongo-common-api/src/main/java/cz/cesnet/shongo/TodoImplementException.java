package cz.cesnet.shongo;

/**
 * Exception thrown when some feature isn't implemented yet.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TodoImplementException extends RuntimeException
{
    /**
     * Message.
     */
    String description;

    /**
     * Constructor.
     */
    public TodoImplementException()
    {
    }

    /**
     * Constructor.
     *
     * @param type which should be implemented
     */
    public TodoImplementException(Class type)
    {
        this.description = type.getCanonicalName();
    }

    /**
     * Constructor.
     *
     * @param enumValue which should be implemented
     */
    public TodoImplementException(Enum enumValue)
    {
        this.description = enumValue.getClass().getCanonicalName() + "." + enumValue.toString();
    }

    /**
     * Constructor.
     *
     * @param description description what should be implemented
     */
    public TodoImplementException(String description)
    {
        this.description = description;
    }

    /**
     * Constructor.
     *
     * @param format  format for description what should be implemented
     * @param objects parameters for format
     */
    public TodoImplementException(String format, Object... objects)
    {
        this(String.format(format, objects));
    }

    /**
     * @return {@link #description}
     */
    public String getDescription()
    {
        return description;
    }

    @Override
    public String getMessage()
    {
        if (description != null) {
            return String.format("TODO: Implement %s", description);
        }
        else {
            return "TODO: Implement";
        }
    }
}
