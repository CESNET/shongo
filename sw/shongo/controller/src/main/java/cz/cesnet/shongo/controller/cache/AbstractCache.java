package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.PersistentObject;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an abstract cache of objects.
 *
 * @param <T> type of cached object
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractCache<T extends PersistentObject>
{
    /**
     * Map of cached objects by theirs ids.
     */
    private Map<Long, T> objectById = new HashMap<Long, T>();

    /**
     * Constructor.
     */
    protected AbstractCache()
    {
        clear();
    }

    /**
     * @param objectId id of a cached object
     * @return cached object with given {@code objectId}
     */
    public T getObject(Long objectId)
    {
        return objectById.get(objectId);
    }

    /**
     * @return collection of all cached objects
     */
    public Collection<T> getObjects()
    {
        return objectById.values();
    }

    /**
     * @param object to be added to the cache
     */
    public void addObject(T object)
    {
        Long objectId = object.getId();
        if (objectById.containsKey(objectId)) {
            throw new IllegalArgumentException(
                    object.getClass().getSimpleName() + " '" + objectId + "' is already in the cache!");
        }
        objectById.put(objectId, object);
    }

    /**
     * @param object to be removed from the cache
     */
    public void removeObject(T object)
    {
        Long objectId = object.getId();
        if (!objectById.containsKey(objectId)) {
            throw new IllegalArgumentException(
                    object.getClass().getSimpleName() + " '" + objectId + "' isn't in the cache!");
        }
        objectById.remove(objectId);
    }

    /**
     * Clear cache.
     */
    public void clear()
    {
        objectById.clear();
    }
}
