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
public class IdentifierWrongTypeException extends RuntimeException implements Fault, SerializableException
{
    /**
     * Identifier which was given.
     */
    private String identifier;

    /**
     * Type which was required.
     */
    private String requiredType;

    /**
     * Constructor.
     */
    public IdentifierWrongTypeException()
    {
    }

    /**
     * Constructor.
     *
     * @param identifier         sets the {@link #identifier}
     * @param requiredType sets the {@link #requiredType}
     */
    public IdentifierWrongTypeException(String identifier, String requiredType)
    {
        setIdentifier(identifier);
        setRequiredType(requiredType);
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

    /**
     * @return {@link #requiredType}
     */
    public String getRequiredType()
    {
        return requiredType;
    }

    /**
     * @param requiredType sets the {@link #requiredType}
     */
    public void setRequiredType(String requiredType)
    {
        this.requiredType = requiredType;
    }

    @Override
    public int getCode()
    {
        return ControllerFault.IDENTIFIER_WRONG_TYPE;
    }

    @Override
    public String getMessage()
    {
        return CommonFault.formatMessage("The identifier '%s' isn't of required type '%s'.", identifier, requiredType);
    }
}
