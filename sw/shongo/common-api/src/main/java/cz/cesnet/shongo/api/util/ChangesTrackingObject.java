package cz.cesnet.shongo.api.util;

import cz.cesnet.shongo.api.annotation.Required;
import cz.cesnet.shongo.fault.CommonFault;
import cz.cesnet.shongo.fault.FaultException;

import java.util.*;

/**
 * Abstract object which is able to keep changes.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ChangesTrackingObject
{
    /**
     * Keys that are used in map for collection changes.
     */
    public static final String COLLECTION_NEW = "new";
    public static final String COLLECTION_MODIFIED = "modified";
    public static final String COLLECTION_DELETED = "deleted";

    /**
     * Key whose value contains the whole {@link Map} data.
     */
    public static final String MAP_DATA = "__map";

    /**
     * Set of properties which are marked as filled.
     */
    private Set<String> filledProperties = new HashSet<String>();

    /**
     * Specifies whether all collection items are by default new (when new entity is being created this should be true).
     */
    private boolean collectionItemIsByDefaultNew = false;

    /**
     * Stores state of collection property.
     */
    public static class CollectionChanges
    {
        /**
         * Set of collection items marked as new
         */
        public Set<Object> newItems = new HashSet<Object>();

        /**
         * Set of collection items marked as deleted
         */
        public Set<Object> deletedItems = new HashSet<Object>();

        /**
         * @return true if all changes are empty, false otherwise
         */
        public boolean isEmpty()
        {
            return newItems.size() == 0 && deletedItems.size() == 0;
        }
    }

    /**
     * Map of changes for collection properties.
     */
    private Map<String, CollectionChanges> collectionChangesMap = new HashMap<String, CollectionChanges>();

    /**
     * @param property
     * @return true if given field was marked as filled,
     *         false otherwise
     */
    public boolean isPropertyFilled(String property)
    {
        return filledProperties.contains(property);
    }

    /**
     * @param propertyName
     * @return collection changes for given property
     */
    public CollectionChanges getCollectionChanges(String propertyName)
    {
        return collectionChangesMap.get(propertyName);
    }

    /**
     * Mark given property as filled.
     *
     * @param property
     */
    public void markPropertyAsFilled(String property)
    {
        filledProperties.add(property);
    }

    /**
     * Mark item in collection as new.
     *
     * @param property
     * @param item
     */
    public void markPropertyItemAsNew(String property, Object item)
    {
        CollectionChanges collectionChanges = collectionChangesMap.get(property);
        if (collectionChanges == null) {
            collectionChanges = new CollectionChanges();
            collectionChangesMap.put(property, collectionChanges);
        }
        collectionChanges.newItems.add(item);
    }

    /**
     * Mark item in collection as removed.
     *
     * @param property
     * @param item
     */
    public void markPropertyItemAsDeleted(String property, Object item)
    {
        CollectionChanges collectionChanges = collectionChangesMap.get(property);
        if (collectionChanges == null) {
            collectionChanges = new CollectionChanges();
            collectionChangesMap.put(property, collectionChanges);
        }
        if (collectionChanges.newItems.contains(item)) {
            collectionChanges.newItems.remove(item);
        }
        else {
            collectionChanges.deletedItems.add(item);
        }
    }

    /**
     * @param property
     * @param item
     * @return true if collection item is marked as new or when all not marked items are by default new,
     *         false otherwise
     */
    public boolean isPropertyItemMarkedAsNew(String property, Object item)
    {
        CollectionChanges collectionChanges = collectionChangesMap.get(property);
        if (collectionChanges != null) {
            return collectionChanges.newItems.contains(item);
        }
        return collectionItemIsByDefaultNew;
    }

    /**
     * @param property
     * @return set of items from given collection which are marked as deleted
     */
    public <T> Set<T> getPropertyItemsMarkedAsNew(String property)
    {
        CollectionChanges collectionChanges = collectionChangesMap.get(property);
        if (collectionChanges != null) {
            @SuppressWarnings("unchecked")
            Set<T> newItems = (Set) collectionChanges.newItems;
            return newItems;
        }
        else {
            return new HashSet<T>();
        }
    }

    /**
     * @param property
     * @return set of items from given collection which are marked as deleted
     */
    public <T> Set<T> getPropertyItemsMarkedAsDeleted(String property)
    {
        CollectionChanges collectionChanges = collectionChangesMap.get(property);
        if (collectionChanges != null) {
            @SuppressWarnings("unchecked")
            Set<T> deletedItems = (Set) collectionChanges.deletedItems;
            return deletedItems;
        }
        else {
            return new HashSet<T>();
        }
    }

    /**
     * Clear all filled/collection marks
     */
    public void clearMarks()
    {
        filledProperties.clear();
        collectionChangesMap.clear();
    }

    /**
     * Checks whether all properties with {@link Required} annotation are marked as filled and
     * sets the {@link #collectionItemIsByDefaultNew} to true (recursive).
     *
     * @throws FaultException when some required field isn't filled
     */
    public void setupNewEntity() throws FaultException
    {
        setupNewEntity(this);
    }

    /**
     * Check {@link Required} in all properties of {@link ChangesTrackingObject} or in all items of
     * arrays and collections (recursive).
     *
     * @param object
     * @throws FaultException
     */
    private static void setupNewEntity(Object object) throws FaultException
    {
        if (object instanceof ChangesTrackingObject) {
            ChangesTrackingObject changesTrackingObject = (ChangesTrackingObject) object;
            changesTrackingObject.collectionItemIsByDefaultNew = true;
            Class type = changesTrackingObject.getClass();
            String[] propertyNames = Property.getPropertyNames(type, ChangesTrackingObject.class);
            for (String propertyName : propertyNames) {
                Property property = Property.getProperty(changesTrackingObject.getClass(), propertyName);
                int propertyTypeFlags = property.getTypeFlags();
                Object value = property.getValue(changesTrackingObject);
                boolean required = property.isRequired();
                if ( value instanceof ChangesTrackingObject ) {
                    setupNewEntity(value);
                }
                else if (TypeFlags.isArray(propertyTypeFlags)) {
                    Object[] array = (Object[]) value;
                    if (required && array.length == 0) {
                        throw new FaultException(CommonFault.CLASS_ATTRIBUTE_COLLECTION_IS_REQUIRED, propertyName,
                                changesTrackingObject.getClass());
                    }
                    for (Object item : array) {
                        setupNewEntity(item);
                    }
                }
                else if (TypeFlags.isCollection(propertyTypeFlags)) {
                    Collection collection = (Collection) value;
                    if (required && collection.isEmpty()) {
                        throw new FaultException(CommonFault.CLASS_ATTRIBUTE_COLLECTION_IS_REQUIRED, propertyName,
                                changesTrackingObject.getClass());
                    }
                    for (Object item : collection) {
                        setupNewEntity(item);
                    }
                }
                else if (required && value == null) {
                    throw new FaultException(CommonFault.CLASS_ATTRIBUTE_IS_REQUIRED, propertyName,
                            changesTrackingObject.getClass());
                }
            }
        }
        else if (object instanceof Object[]) {
            Object[] array = (Object[]) object;
            for (Object item : array) {
                setupNewEntity(item);
            }
        }
        else if (object instanceof Collection) {
            Collection collection = (Collection) object;
            for (Object item : collection) {
                setupNewEntity(item);
            }
        }
    }
}
