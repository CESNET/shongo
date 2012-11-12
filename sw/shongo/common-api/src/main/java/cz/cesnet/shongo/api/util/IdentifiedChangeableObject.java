package cz.cesnet.shongo.api.util;

import cz.cesnet.shongo.api.annotation.Transient;
import cz.cesnet.shongo.api.xmlrpc.StructType;
import jade.content.Concept;
import jade.content.onto.annotations.SuppressSlot;

/**
 * Represents a type for a API that can be serialized
 * to/from {@link java.util.Map}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class IdentifiedChangeableObject extends ChangesTrackingObject implements StructType, Concept
{
    /**
     * Identifier.
     */
    private String identifier;

    /**
     * Storage for properties.
     */
    private PropertyStorage propertyStorage;

    /**
     * @return {@link #propertyStorage}
     */
    @Transient
    public PropertyStorage getPropertyStorage()
    {
        if (propertyStorage == null) {
            propertyStorage = new PropertyStorage(this);
        }
        return propertyStorage;
    }

    /**
     * @param propertyStorage sets the {@link #propertyStorage}
     */
    public void setPropertyStorage(PropertyStorage propertyStorage)
    {
        getPropertyStorage().fill(propertyStorage);
    }

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
    @SuppressSlot
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
        if (identifier == null) {
            throw new IllegalStateException("Attribute 'id' in entity '" + getClass().getSimpleName()
                    + "' must not be null.");
        }
        return Long.valueOf(identifier);
    }
}
