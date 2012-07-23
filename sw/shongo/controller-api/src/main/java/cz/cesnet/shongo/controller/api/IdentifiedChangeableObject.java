package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.util.ChangesTrackingObject;
import cz.cesnet.shongo.api.util.PropertyStorage;
import cz.cesnet.shongo.controller.api.xmlrpc.StructType;

/**
 * Represents a type for a API that can be serialized
 * to/from {@link java.util.Map}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class IdentifiedChangeableObject extends ChangesTrackingObject implements StructType
{
    /**
     * Identifier.
     */
    private Integer id;

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
     * @return {@link #id
     */
    public Integer getId()
    {
        return id;
    }

    /**
     * @param id sets the {@link #id}
     */
    public void setId(Integer id)
    {
        this.id = id;
    }
}
