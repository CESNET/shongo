package cz.cesnet.shongo.api.util;

import cz.cesnet.shongo.api.FaultException;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Represents a storage for object properties.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class PropertyStorage
{
    /**
     * Internal store for property values.
     */
    private Map<String, Object> values = new HashMap<String, Object>();

    /**
     * Reference to {@link ChangesTrackingObject} which should be notified about property changes;
     */
    ChangesTrackingObject changesTrackingObject;

    /**
     * Constructor.
     *
     * @param changesKeepingObject sets the {@link #changesTrackingObject}
     */
    public PropertyStorage(ChangesTrackingObject changesKeepingObject)
    {
        this.changesTrackingObject = changesKeepingObject;
    }

    /**
     * Set property value.
     *
     * @param property
     * @param value
     */
    public void setValue(String property, Object value)
    {
        values.put(property, value);
        if (changesTrackingObject != null) {
            changesTrackingObject.markPropertyAsFilled(property);
        }
    }

    /**
     * @param property
     * @return value of given property
     */
    public <T> T getValue(String property)
    {
        @SuppressWarnings("unchecked")
        T value = (T) values.get(property);
        return value;
    }

    /**
     * @param property
     * @return internal collection
     */
    @SuppressWarnings("unchecked")
    private Collection getInternalCollection(String property)
    {
        Collection collection = (Collection) values.get(property);
        if (collection == null) {
            Class propertyType = List.class;
            try {
                propertyType = Property.getPropertyType(getClass(), property);
            }
            catch (FaultException e) {
            }
            try {
                collection = Converter.createCollection(propertyType, 0);
            }
            catch (FaultException exception) {
                throw new RuntimeException(exception);
            }
            values.put(property, collection);
        }
        return collection;
    }

    /**
     * @param property
     * @return return collection of given property
     */
    @SuppressWarnings("unchecked")
    public <T extends Collection> T getCollection(String property)
    {
        Collection collection = getInternalCollection(property);
        return (T) collection;
    }

    /**
     * @param property
     * @param type
     * @return return collection of given property converted to array
     */
    @SuppressWarnings("unchecked")
    public <T> T[] getCollection(String property, Class<T> type)
    {
        Collection<T> collection = getCollection(property);
        T[] array = (T[]) Array.newInstance(type, collection.size());
        try {
            return collection.toArray(array);
        }
        catch (RuntimeException exception) {
            throw new RuntimeException(
                    String.format("Failed to convert collection '%s' to array of type '%s'.", property,
                            type.getCanonicalName()), exception);
        }
    }

    /**
     * Set property collection value.
     *
     * @param property
     * @param collection
     */
    public <T> void setCollection(String property, Collection<T> collection)
    {
        values.put(property, collection);
    }

    /**
     * Set property collection value from array.
     *
     * @param property
     * @param array
     */
    public <T> void setCollection(String property, T[] array)
    {
        setCollection(property, new ArrayList<T>(Arrays.asList(array)));
    }

    /**
     * Add given new item to to the collection property.
     *
     * @param property
     * @param item
     * @return true if adding was successful,
     *         false otherwise
     */
    public boolean addCollectionItem(String property, Object item)
    {
        @SuppressWarnings("unchecked")
        Collection<Object> collection = getInternalCollection(property);
        if (collection.add(item)) {
            if (changesTrackingObject != null) {
                changesTrackingObject.markCollectionItemAsNew(property, item);
            }
            return true;
        }
        return false;
    }

    /**
     * Remove given item from the collection property.
     *
     * @param property
     * @param item
     * @return true if removing was successful,
     *         false otherwise
     */
    public boolean removeCollectionItem(String property, Object item)
    {
        Collection collection = (Collection) values.get(property);
        if (collection == null) {
            return false;
        }
        if (collection.remove(item)) {
            if (changesTrackingObject != null) {
                changesTrackingObject.markCollectionItemAsDeleted(property, item);
            }
            return true;
        }
        return false;
    }
}
