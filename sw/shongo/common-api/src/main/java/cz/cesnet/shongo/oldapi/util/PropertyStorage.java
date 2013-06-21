package cz.cesnet.shongo.oldapi.util;

import cz.cesnet.shongo.api.ClassHelper;

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
     * Reference to {@link ChangesTracking} which should be notified about property changes;
     */
    ChangesTracking changesTracking;

    /**
     * Constructor.
     */
    public PropertyStorage()
    {
    }

    /**
     * Constructor.
     *
     * @param changesKeeping sets the {@link #changesTracking}
     */
    public PropertyStorage(ChangesTracking changesKeeping)
    {
        this.changesTracking = changesKeeping;
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
        if (changesTracking != null) {
            changesTracking.markPropertyAsFilled(property);
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
     * @param defaultValue
     * @return value of given property as int
     */
    public boolean getValueAsBoolean(String property, Boolean defaultValue)
    {
        Object value = getValue(property);
        if (value == null) {
            return defaultValue;
        }
        return (Boolean) value;
    }

    /**
     * @param property
     * @return value of given property as int
     */
    public int getValueAsInt(String property)
    {
        Object value = getValue(property);
        if (value == null) {
            return 0;
        }
        return (Integer) value;
    }

    /**
     * @param property
     * @return internal collection
     */
    @SuppressWarnings("unchecked")
    private <T extends Collection> Collection getInternalCollection(String property, Class<T> collectionType)
    {
        Collection collection = (Collection) values.get(property);
        if (collection == null && collectionType != null) {
            collection = ClassHelper.createCollection(collectionType, 0);
            values.put(property, collection);
        }
        return collection;
    }

    /**
     * @param property
     * @return return collection of given property
     */
    @SuppressWarnings("unchecked")
    public <T extends Collection> T getCollection(String property, Class<? extends Collection> collectionType)
    {
        Collection collection = getInternalCollection(property, collectionType);
        return (T) collection;
    }

    /**
     * @param property
     * @param type
     * @return return collection of given property converted to array
     */
    @SuppressWarnings("unchecked")
    public <T> T[] getCollectionAsArray(String property, Class<T> type)
    {
        Collection<T> collection = (Collection) values.get(property);
        if (collection == null) {
            collection = new ArrayList();
        }
        T[] array = (T[]) Array.newInstance(type, collection.size());
        try {
            return collection.toArray(array);
        }
        catch (RuntimeException exception) {
            throw new RuntimeException(
                    String.format("Failed to convert collection '%s' to array of collectionType '%s'.", property,
                            type.getCanonicalName()), exception);
        }
    }

    /**
     * Set property collection value.
     *
     * @param property
     * @param collection
     */
    public <T> void setCollection(String property, Set<T> collection)
    {
        if (collection != null) {
            collection = new HashSet<T>(collection);
        }
        values.put(property, collection);
        if (changesTracking != null) {
            changesTracking.markPropertyAsFilled(property);
        }
    }

    /**
     * Set property collection value.
     *
     * @param property
     * @param collection
     */
    public <T> void setCollection(String property, List<T> collection)
    {
        if (collection != null) {
            collection = new LinkedList<T>(collection);
        }
        values.put(property, collection);
        if (changesTracking != null) {
            changesTracking.markPropertyAsFilled(property);
        }
    }

    /**
     * Set property collection value from array.
     *
     * @param property
     * @param array
     */
    public <T> void setCollection(String property, T[] array)
    {
        if (array != null) {
            setCollection(property, new ArrayList<T>(Arrays.asList(array)));
        }
        else {
            values.put(property, null);
            if (changesTracking != null) {
                changesTracking.markPropertyAsFilled(property);
            }
        }
    }

    /**
     * Add given new item to to the collection property.
     *
     * @param property
     * @param item
     * @return true if adding was successful,
     *         false otherwise
     */
    public <T extends Collection> boolean addCollectionItem(String property, Object item,
            Class<? extends Collection> collectionType)
    {
        @SuppressWarnings("unchecked")
        Collection<Object> collection = getInternalCollection(property, collectionType);
        if (collection.add(item)) {
            if (changesTracking != null) {
                changesTracking.markPropertyAsFilled(property);
                changesTracking.markPropertyItemAsNew(property, item);
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
        @SuppressWarnings("unchecked")
        Collection collection = getInternalCollection(property, null);
        if (collection == null) {
            return false;
        }
        if (collection.remove(item)) {
            if (changesTracking != null) {
                changesTracking.markPropertyAsFilled(property);
                changesTracking.markPropertyItemAsDeleted(property, item);
            }
            return true;
        }
        return false;
    }


    /**
     * @param property
     * @return internal {@link Map}
     */
    private Map<Object, Object> getInternalMap(String property)
    {
        @SuppressWarnings("unchecked")
        Map<Object, Object> map = (Map<Object, Object>) values.get(property);
        if (map == null) {
            map = new HashMap<Object, Object>();
            values.put(property, map);
        }
        return map;
    }

    /**
     * @param property name of the property
     * @return return {@link Map} for given {@code property}
     */
    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getMap(String property)
    {
        return (Map<K, V>) getInternalMap(property);
    }

    /**
     * Set property {@link Map} value.
     *
     * @param property name of the property
     * @param map      {@link Map} value to be set
     */
    public <T> void setMap(String property, Map map)
    {
        values.put(property, map);
        if (changesTracking != null) {
            changesTracking.markPropertyAsFilled(property);
        }
    }

    /**
     * Add given new item to to the collection property.
     *
     * @param property
     * @param itemKey
     * @param itemValue
     * @return true if adding was successful,
     *         false otherwise
     */
    public <T extends Collection> boolean addMapItem(String property, Object itemKey, Object itemValue)
    {
        Map<Object, Object> map = getInternalMap(property);
        if (map.put(itemKey, itemValue) == null) {
            if (changesTracking != null) {
                changesTracking.markPropertyAsFilled(property);
                changesTracking.markPropertyItemAsNew(property, itemKey);
            }
            return true;
        }
        return false;
    }

    /**
     * Remove given item from the collection property.
     *
     * @param property
     * @param itemKey
     * @return true if removing was successful,
     *         false otherwise
     */
    public boolean removeMapItem(String property, Object itemKey)
    {
        Map map = (Map) values.get(property);
        if (map == null) {
            return false;
        }
        if (map.remove(itemKey) != null) {
            if (changesTracking != null) {
                changesTracking.markPropertyAsFilled(property);
                changesTracking.markPropertyItemAsDeleted(property, itemKey);
            }
            return true;
        }
        return false;
    }
}
