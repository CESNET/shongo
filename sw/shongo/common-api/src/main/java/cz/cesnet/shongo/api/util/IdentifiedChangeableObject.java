package cz.cesnet.shongo.api.util;

import cz.cesnet.shongo.api.annotation.Required;
import cz.cesnet.shongo.api.annotation.Transient;
import cz.cesnet.shongo.api.xmlrpc.StructType;
import cz.cesnet.shongo.fault.FaultException;
import jade.content.Concept;
import jade.content.onto.annotations.SuppressSlot;

import java.util.Set;

/**
 * Represents a type for a API that can be serialized
 * to/from {@link java.util.Map}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class IdentifiedChangeableObject implements ChangesTracking.Changeable, StructType, Concept
{
    /**
     * Id.
     */
    private String id;

    /**
     * Storage for properties.
     */
    private PropertyStorage propertyStorage;

    /**
     * @see ChangesTracking
     */
    private ChangesTracking changesTracking;

    /**
     * @return {@link #propertyStorage}
     */
    protected PropertyStorage getPropertyStorage()
    {
        if (propertyStorage == null) {
            propertyStorage = new PropertyStorage(getChangesTracking());
        }
        return propertyStorage;
    }

    @Override
    @Transient
    public ChangesTracking getChangesTracking()
    {
        if (changesTracking == null) {
            changesTracking = new ChangesTracking();
        }
        return changesTracking;
    }

    /**
     * @param changesTracking from which should be {@link #changesTracking} filled
     */
    public void setChangesTracking(ChangesTracking changesTracking)
    {
        getChangesTracking().fill(changesTracking);
    }

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
    @SuppressSlot
    public void setId(Long id)
    {
        this.id = id.toString();
    }

    /**
     * @return {@link #id} as {@link Long}
     * @throws IllegalStateException
     */
    public Long notNullIdAsLong()
    {
        if (id == null) {
            throw new IllegalStateException("Attribute 'id' in entity '" + getClass().getSimpleName()
                    + "' must not be null.");
        }
        return Long.valueOf(id);
    }

    /**
     * Checks whether all properties with {@link Required} annotation are marked as filled and
     * sets the {@link ChangesTracking#collectionItemIsByDefaultNew} to true (recursive).
     *
     * @throws cz.cesnet.shongo.fault.FaultException
     *          when some required field isn't filled
     */
    public void setupNewEntity() throws FaultException
    {
        ChangesTracking.setupNewEntity(this);
    }

    /**
     * @see ChangesTracking#isPropertyFilled(String)
     */
    public boolean isPropertyFilled(String property)
    {
        return getChangesTracking().isPropertyFilled(property);
    }

    /**
     * @see ChangesTracking#isPropertyItemMarkedAsNew(String, Object)
     */
    public boolean isPropertyItemMarkedAsNew(String property, Object item)
    {
        return getChangesTracking().isPropertyItemMarkedAsNew(property, item);
    }

    /**
     * @see ChangesTracking#getPropertyItemsMarkedAsNew(String)
     */
    public <T> Set<T> getPropertyItemsMarkedAsNew(String property)
    {
        return getChangesTracking().getPropertyItemsMarkedAsNew(property);
    }

    /**
     * @see ChangesTracking#getPropertyItemsMarkedAsDeleted(String)
     */
    public <T> Set<T> getPropertyItemsMarkedAsDeleted(String property)
    {
        return getChangesTracking().getPropertyItemsMarkedAsDeleted(property);
    }
}
