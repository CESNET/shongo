package cz.cesnet.shongo.api.util;

import cz.cesnet.shongo.api.xmlrpc.StructType;

/**
 * Represents a type for a API that can be serialized
 * to/from {@link java.util.Map}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class IdentifiedObject implements StructType
{
    /**
     * Identifier.
     */
    private String identifier;

    /**
     * @return {@link #identifier
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
     * @param identifier sets the {@link #identifier}
     */
    public void setIdentifier(Long identifier)
    {
        this.identifier = identifier.toString();
    }

    /**
     * @return {@link #identifier} as {@link Long}
     * @throws IllegalStateException
     */
    public Long notNullIdAsLong()
    {
        if ( identifier == null ) {
            throw new IllegalStateException();
        }
        return Long.valueOf(identifier);
    }
}
