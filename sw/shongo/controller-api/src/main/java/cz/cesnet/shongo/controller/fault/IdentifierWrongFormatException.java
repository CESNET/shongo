package cz.cesnet.shongo.controller.fault;

import cz.cesnet.shongo.controller.api.ControllerFault;
import cz.cesnet.shongo.fault.CommonFault;
import cz.cesnet.shongo.fault.Fault;
import cz.cesnet.shongo.fault.SerializableException;

/**
 * Exception to be thrown when an entity with an id hasn't been found.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class IdentifierWrongFormatException extends RuntimeException implements Fault, SerializableException
{
    /**
     * Identifier which was given.
     */
    private String identifier;

    /**
     * Constructor.
     */
    public IdentifierWrongFormatException()
    {
    }

    /**
     * Constructor.
     *
     * @param identifier         sets the {@link #identifier}
     */
    public IdentifierWrongFormatException(String identifier)
    {
        setIdentifier(identifier);
    }

    /**
     * @return {@link #identifier}
     */
    public String getIdentifier()
    {
        return identifier;
    }

    /**
     * @param identifier sets the {@link #identifier}
     */
    public void setIdentifier(String identifier)
    {
        this.identifier = identifier;
    }

    @Override
    public int getCode()
    {
        return ControllerFault.IDENTIFIER_WRONG_FORMAT;
    }

    @Override
    public String getMessage()
    {
        return CommonFault.formatMessage("The identifier '%s' is in wrong format.", identifier);
    }
}
