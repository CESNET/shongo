package cz.cesnet.shongo.api.util;

import cz.cesnet.shongo.api.xmlrpc.StructType;
import jade.content.Concept;

/**
 * Represents a type for a API that can be serialized
 * to/from {@link java.util.Map}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class IdentifiedObject implements StructType, Concept
{
    /**
     * Identifier.
     */
    private String id;

    /**
     * @return {@link #id
     */
    public String getId()
    {
        return id;
    }

    /**
     * @param id sets the {@link #id}
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @param id sets the {@link #id}
     */
    public void setId(Long id)
    {
        this.id = (id != null ? id.toString() : null);
    }

    /**
     * @return {@link #id} as {@link Long}
     * @throws IllegalStateException
     */
    public Long notNullIdAsLong()
    {
        if (id == null) {
            throw new IllegalStateException();
        }
        return Long.valueOf(id);
    }
}
