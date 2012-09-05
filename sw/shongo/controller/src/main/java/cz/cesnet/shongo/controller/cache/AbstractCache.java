package cz.cesnet.shongo.controller.cache;

import cz.cesnet.shongo.PersistentObject;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an abstract cache.
 *
 * @param <T> type of cached object
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class AbstractCache<T extends PersistentObject>
{
    /**
     * Map of cached objects by theirs identifiers.
     */
    protected Map<Long, T> objectById = new HashMap<Long, T>();

    /**
     * Load cached objects from the database.
     *
     * @param entityManager
     */
    public abstract void loadObjects(EntityManager entityManager);

    /**
     * @param objectId identifier of a cached object
     * @return cached object with given {@code objectId}
     */
    public T getObject(Long objectId)
    {
        return objectById.get(objectId);
    }

    /**
     * @param object to be added to the cache
     */
    public void addObject(T object)
    {
        object.checkPersisted();
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
}
