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
public class IdentifierWrongDomainException extends RuntimeException implements Fault, SerializableException
{
    /**
     * Identifier name which was given.
     */
    private String identifier;

    /**
     * Domain name which was required.
     */
    private String requiredDomain;

    /**
     * Constructor.
     */
    public IdentifierWrongDomainException()
    {
    }

    /**
     * Constructor.
     *
     * @param identifier         sets the {@link #identifier}
     * @param requiredDomain sets the {@link #requiredDomain}
     */
    public IdentifierWrongDomainException(String identifier, String requiredDomain)
    {
        setIdentifier(identifier);
        setRequiredDomain(requiredDomain);
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
     * @return {@link #requiredDomain}
     */
    public String getRequiredDomain()
    {
        return requiredDomain;
    }

    /**
     * @param requiredDomain sets the {@link #requiredDomain}
     */
    public void setRequiredDomain(String requiredDomain)
    {
        this.requiredDomain = requiredDomain;
    }

    @Override
    public int getCode()
    {
        return ControllerFault.IDENTIFIER_WRONG_DOMAIN;
    }

    @Override
    public String getMessage()
    {
        return CommonFault.formatMessage("The identifier '%s' doesn't belong to domain '%s'.", identifier, requiredDomain);
    }
}
