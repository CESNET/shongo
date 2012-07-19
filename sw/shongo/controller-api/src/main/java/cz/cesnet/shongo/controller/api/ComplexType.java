package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.ChangesTrackingObject;
import cz.cesnet.shongo.api.annotation.Accessible;
import cz.cesnet.shongo.controller.api.xmlrpc.StructType;

/**
 * Represents a type for a API that can be serialized
 * to/from {@link java.util.Map}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ComplexType extends ChangesTrackingObject implements StructType
{
    /**
     * Object identifier
     */
    @Accessible
    private String identifier;

    /**
     * Storage for properties.
     */
    private PropertyStorage propertyStorage;

    /**
     * @return {@link #propertyStorage}
     */
    protected PropertyStorage getPropertyStorage()
    {
        if (propertyStorage == null) {
            propertyStorage = new PropertyStorage(this);
        }
        return propertyStorage;
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
    void setIdentifier(String identifier)
    {
        this.identifier = identifier.toString();
    }
}
