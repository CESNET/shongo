package cz.cesnet.shongo.api;

import jade.content.Concept;
import jade.content.onto.annotations.SuppressSlot;

/**
 * Represents object which can be serialized to/from {@link java.util.Map}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class IdentifiedComplexType extends AbstractComplexType implements Concept
{
    /**
     * Identifier.
     */
    protected String id;

    /**
     * @return {@link #id}
     */
    public String getId()
    {
        return id;
    }

    /**
     * @return true if the {@link #id} is not null,
     *         false otherwise
     */
    @SuppressSlot
    public boolean hasId()
    {
        return id != null;
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
    @SuppressSlot
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

    private static final String ID = "id";

    @Override
    public DataMap toData()
    {
        DataMap dataMap = super.toData();
        dataMap.set(ID, id);
        return dataMap;
    }

    @Override
    public void fromData(DataMap dataMap)
    {
        super.fromData(dataMap);
        id = dataMap.getString(ID);
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName() + toData();
    }
}
