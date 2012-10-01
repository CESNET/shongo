package cz.cesnet.shongo.controller.api;

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
    private Integer id;

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
