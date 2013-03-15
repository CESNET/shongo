package cz.cesnet.shongo.fault;

import cz.cesnet.shongo.CommonFaultSet;
import cz.cesnet.shongo.fault.Fault;
import cz.cesnet.shongo.fault.old.CommonFault;
import cz.cesnet.shongo.fault.old.SerializableException;

/**
 * Exception thrown when some feature isn't implemented yet.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TodoImplementException extends RuntimeException implements FaultThrowable, SerializableException
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

    @Override
    public Fault getFault()
    {
        return CommonFaultSet.createUnknownErrorFault(description);
    }
}
