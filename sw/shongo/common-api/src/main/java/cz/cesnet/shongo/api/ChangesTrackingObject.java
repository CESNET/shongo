package cz.cesnet.shongo.api;

import cz.cesnet.shongo.api.annotation.Required;
import cz.cesnet.shongo.api.util.Property;

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
     * Set of properties which are marked as filled.
     */
    private Set<String> filledProperties = new HashSet<String>();

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
    public void markCollectionItemAsNew(String property, Object item)
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
    public void markCollectionItemAsDeleted(String property, Object item)
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

    public boolean isCollectionItemMarkedAsNew(String property, Object item)
    {
        CollectionChanges collectionChanges = collectionChangesMap.get(property);
        if (collectionChanges != null) {
            return collectionChanges.newItems.contains(item);
        }
        return false;
    }

    /**
     * @param property
     * @param type
     * @return set of items from given collection which are marked as deleted
     */
    public <T> Set<T> getCollectionItemsMarkedAsDeleted(String property, Class<T> type)
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
     * Checks whether all properties with {@link Required} annotation are marked as filled (recursive).
     *
     * @throws cz.cesnet.shongo.api.FaultException
     *
     */
    public void checkRequiredPropertiesFilled() throws FaultException
    {
        checkRequiredPropertiesFilled(this);
    }

    /**
     * Check {@link Required} in all properties of {@link ChangesTrackingObject} or in all items of
     * arrays and collections (recursive).
     *
     * @param object
     * @throws FaultException
     */
    private static void checkRequiredPropertiesFilled(Object object) throws FaultException
    {
        if (object instanceof ChangesTrackingObject) {
            ChangesTrackingObject changesTrackingObject = (ChangesTrackingObject) object;
            Class type = changesTrackingObject.getClass();
            String[] propertyNames = Property.getPropertyNames(type);
            for (String propertyName : propertyNames) {
                Property property = Property.getProperty(changesTrackingObject.getClass(), propertyName);
                Object value = property.getValue(changesTrackingObject);
                boolean required = property.isRequired();
                if (property.isArray()) {
                    Object[] array = (Object[]) value;
                    if (required && array.length == 0) {
                        throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_COLLECTION_IS_REQUIRED, propertyName,
                                changesTrackingObject.getClass());
                    }
                    for (Object item : array) {
                        checkRequiredPropertiesFilled(item);
                    }
                }
                else if (property.isCollection()) {
                    Collection collection = (Collection) value;
                    if (required && collection.isEmpty()) {
                        throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_COLLECTION_IS_REQUIRED, propertyName,
                                changesTrackingObject.getClass());
                    }
                    for (Object item : collection) {
                        checkRequiredPropertiesFilled(item);
                    }
                }
                else if (required && value == null) {
                    throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_IS_REQUIRED, propertyName,
                            changesTrackingObject.getClass());
                }
            }
        }
        else if (object instanceof Object[]) {
            Object[] array = (Object[]) object;
            for (Object item : array) {
                checkRequiredPropertiesFilled(item);
            }
        }
        else if (object instanceof Collection) {
            Collection collection = (Collection) object;
            for (Object item : collection) {
                checkRequiredPropertiesFilled(item);
            }
        }
    }
}
