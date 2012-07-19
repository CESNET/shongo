package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.api.annotation.ForceAccessible;
import cz.cesnet.shongo.controller.api.xmlrpc.StructType;

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
    private int id;

    /**
     * @return {@link #id
     */
    public int getId()
    {
        return id;
    }

    /**
     * @param id sets the {@link #id}
     */
    @ForceAccessible
    void setId(int id)
    {
        this.id = id;
    }
}
