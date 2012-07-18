package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.api.util.ClassHelper;
import cz.cesnet.shongo.controller.api.util.Converter;
import cz.cesnet.shongo.controller.api.util.Property;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.util.*;

import static cz.cesnet.shongo.controller.api.util.ClassHelper.getClassShortName;

/**
 * Represents a type for a API that can be serialized
 * to/from {@link java.util.Map}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ComplexType
{
    /**
     * Annotation used for properties that must be present when a new entity is created.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public static @interface Required
    {
    }

    /**
     * Annotation used for properties to restrict allowed types.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public static @interface AllowedTypes
    {
        /**
         * @return array of allowed types for the property
         */
        Class[] value();

        /**
         * @return default value for {@link #value()}
         */
        Class[] defaultValue() default {};
    }

    /**
     * Annotation used for properties to make them accessible by {@link Property}.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.FIELD})
    public static @interface Accessible
    {
    }

    /**
     * Stores state of collection property.
     */
    private static class CollectionChanges
    {
        /**
         * Set of collection items marked as new
         */
        Set<Object> newItems = new HashSet<Object>();
        /**
         * Set of collection items marked as deleted
         */
        Set<Object> deletedItems = new HashSet<Object>();

        /**
         * @return true if all changes are empty, false otherwise
         */
        public boolean isEmpty()
        {
            return newItems.size() == 0 && deletedItems.size() == 0;
        }
    }

    /**
     * Holds values for properties.
     */
    protected class PropertyStore
    {
        /**
         * Internal store for property values.
         */
        private Map<String, Object> values = new HashMap<String, Object>();

        /**
         * Set property value.
         *
         * @param property
         * @param value
         */
        public void setValue(String property, Object value)
        {
            values.put(property, value);
            markPropertyAsFilled(property);
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
                markCollectionItemAsNew(property, item);
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
                markCollectionItemAsDeleted(property, item);
                return true;
            }
            return false;
        }
    }

    /**
     * Keys that are used in map for collection changes.
     */
    public static final String COLLECTION_NEW = "new";
    public static final String COLLECTION_MODIFIED = "modified";
    public static final String COLLECTION_DELETED = "deleted";

    /**
     * Object identifier
     */
    @Accessible
    private String identifier;

    /**
     * Set of properties which are marked as filled.
     */
    private Set<String> filledProperties = new HashSet<String>();

    /**
     * Map of changes for collection properties.
     */
    private Map<String, CollectionChanges> collectionChangesMap = new HashMap<String, CollectionChanges>();

    /**
     * Store that can be used to store property values by extending classes.
     */
    protected PropertyStore propertyStore = new PropertyStore();

    /**
     * @return {@link #identifier}
     */
    public String getIdentifier()
    {
        return identifier;
    }

    /**
     * @param identifier sets the {@link #identifier}
     */
    void setIdentifier(String identifier)
    {
        this.identifier = identifier.toString();
    }

    /**
     * @return {@link #identifier}
     */
    public Long getIdentifierAsLong()
    {
        return Long.parseLong(identifier);
    }

    /**
     * @param property
     * @return true if given field was marked as filled,
     *         false otherwise
     */
    protected boolean isPropertyFilled(String property)
    {
        return filledProperties.contains(property);
    }

    /**
     * Mark given property as filled.
     *
     * @param property
     */
    protected void markPropertyAsFilled(String property)
    {
        filledProperties.add(property);
    }

    /**
     * Mark item in collection as new.
     *
     * @param property
     * @param item
     */
    protected void markCollectionItemAsNew(String property, Object item)
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
    protected void markCollectionItemAsDeleted(String property, Object item)
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
    protected void clearMarks()
    {
        filledProperties.clear();
        collectionChangesMap.clear();
    }

    /**
     * Options for {@link ComplexType#toMap(Options)}.
     */
    public static final class Options
    {
        /**
         * Options for server.
         * <p/>
         * <code>{@link #storeChanges} = true</code>
         * We want to propagate changes in entities from client to server. And thus
         * {@link ComplexType#toMap(Options)} should store changes in client.
         * <p/>
         * <code>{@link #forceAccessible} = true<code/>
         * On server side we want to serialize/deserialize only "public" properties.
         */
        public static final Options CLIENT = new Options(true, true);

        /**
         * Options for server.
         * <p/>
         * <code>{@link #storeChanges} = false</code>
         * We don't want to propagate changes in entities from server to client. Client always get
         * "clean" object without changes. And thus {@link ComplexType#toMap(Options)} should not
         * store changes on server.
         * <p/>
         * <code>{@link #forceAccessible} = false</code>
         * On server side we want to serialize/deserialize only "public" properties.
         */
        public static final Options SERVER = new Options(false, false);

        /**
         * Specifies whether marks for filled properties should be used and whether collections
         * should be stored as Maps with {@link #COLLECTION_NEW}, {@link #COLLECTION_MODIFIED},
         * {@link #COLLECTION_DELETED} lists.
         */
        private boolean storeChanges = false;

        /**
         * Options whether properties should be set with forced accessibility (e.g., to use private fields or setters).
         */
        private boolean forceAccessible = false;

        /**
         * Constructor.
         *
         * @param storeChanges    sets the {@link #storeChanges}
         * @param forceAccessible sets the {@link #forceAccessible}
         */
        private Options(boolean storeChanges, boolean forceAccessible)
        {
            this.storeChanges = storeChanges;
            this.forceAccessible = forceAccessible;
        }

        /**
         * @return {@link #storeChanges}
         */
        public boolean isStoreChanges()
        {
            return storeChanges;
        }

        /**
         * @return {@link #forceAccessible}
         */
        public boolean isForceAccessible()
        {
            return forceAccessible;
        }
    }

    /**
     * Fill object from the given map.
     *
     * @param map
     * @param options see {@link Options}
     * @throws FaultException
     */
    public void fromMap(Map map, Options options) throws FaultException
    {
        // Clear all filled properties
        clearMarks();

        // Fill each property that is present in map
        for (Object key : map.keySet()) {
            if (!(key instanceof String)) {
                throw new FaultException(Fault.Common.UNKNOWN_FAULT, "Map must contain only string keys.");
            }
            String propertyName = (String) key;
            Object value = map.get(key);

            // Skip class property
            if (propertyName.equals("class")) {
                continue;
            }

            // Get property type and allowed types
            Property property = Property.getPropertyNotNull(getClass(), propertyName);
            Class type = property.getType();
            Class[] allowedTypes = property.getAllowedTypes();

            // Parse collection changes
            if (value instanceof Map && property.isArrayOrCollection()) {
                Map collectionChanges = (Map) value;
                Object newItems = null;
                Object modifiedItems = null;
                Object deletedItems = null;
                if (collectionChanges.containsKey(COLLECTION_NEW)) {
                    newItems = Converter.convert(collectionChanges.get(COLLECTION_NEW),
                            type, allowedTypes, propertyName, options);
                }
                if (collectionChanges.containsKey(COLLECTION_MODIFIED)) {
                    modifiedItems = Converter.convert(collectionChanges.get(COLLECTION_MODIFIED),
                            type, allowedTypes, propertyName, options);
                }
                if (collectionChanges.containsKey(COLLECTION_DELETED)) {
                    deletedItems = Converter.convert(collectionChanges.get(COLLECTION_DELETED),
                            type, allowedTypes, propertyName, options);
                }
                if (newItems != null || modifiedItems != null || deletedItems != null) {
                    if (property.isArray()) {
                        int size = (newItems != null ? ((Object[]) newItems).length : 0)
                                + (modifiedItems != null ? ((Object[]) modifiedItems).length : 0);
                        int index = 0;
                        Object[] array = Converter.createArray(type.getComponentType(), size);
                        if (newItems != null) {
                            for (Object newItem : (Object[]) newItems) {
                                array[index++] = newItem;
                                markCollectionItemAsNew(propertyName, newItem);
                            }
                        }
                        if (modifiedItems != null) {
                            for (Object modifiedItem : (Object[]) modifiedItems) {
                                array[index++] = modifiedItem;
                            }
                        }
                        if (deletedItems != null) {
                            for (Object deletedItem : (Object[]) deletedItems) {
                                markCollectionItemAsDeleted(propertyName, deletedItem);
                            }
                        }
                        value = array;
                    }
                    else if (property.isCollection()) {
                        Collection<Object> collection = Converter.createCollection(type, 0);
                        if (newItems != null) {
                            for (Object newItem : (Collection) newItems) {
                                collection.add(newItem);
                                markCollectionItemAsNew(propertyName, newItem);
                            }
                        }
                        if (modifiedItems != null) {
                            for (Object modifiedItem : (Collection) modifiedItems) {
                                collection.add(modifiedItem);
                            }
                        }
                        if (deletedItems != null) {
                            for (Object deletedItem : (Collection) deletedItems) {
                                markCollectionItemAsDeleted(propertyName, deletedItem);
                            }
                        }
                        value = collection;
                    }
                }
            }

            try {
                value = Converter.convert(value, type, allowedTypes, propertyName, options);
            }
            catch (IllegalArgumentException exception) {
                Object requiredType = type;
                Object givenType = value.getClass();
                if (allowedTypes != null) {
                    StringBuilder builder = new StringBuilder();
                    for (Class allowedType : allowedTypes) {
                        if (builder.length() > 0) {
                            builder.append("|");
                        }
                        builder.append(ClassHelper.getClassShortName(allowedType));
                    }
                    requiredType = builder.toString();
                }
                if (value instanceof String) {
                    givenType = String.format("String(%s)", value);
                }
                throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_TYPE_MISMATCH, propertyName,
                        getClass(), requiredType, givenType);
            }

            // Set the value to property
            property.setValue(this, value, options.isForceAccessible());

            // Mark property as filled
            markPropertyAsFilled(propertyName);
        }
    }

    /**
     * Convert object to a map. Map will contain all object's properties that are:
     * 1) simple (not {@link Object[]} or {@link Collection}) with not {@link null} value
     * 2) simple with {@link null} value but marked as filled through ({@link #markPropertyAsFilled(String)}
     * 3) {@link Object[]} or {@link Collection} which is not empty
     *
     * @param options see {@link Options}
     * @return map which contains object's properties
     * @throws FaultException when the conversion fails
     */
    public Map toMap(Options options) throws FaultException
    {
        Map<String, Object> map = new HashMap<String, Object>();
        String[] propertyNames = Property.getPropertyNames(getClass());
        for (String propertyName : propertyNames) {
            Property property = Property.getProperty(getClass(), propertyName);
            if (property == null) {
                throw new FaultException("Cannot get property '%s' from class '%s'.", propertyName, getClass());
            }
            Object value = property.getValue(this);
            if (value == null && !(options.isStoreChanges() && isPropertyFilled(propertyName))) {
                continue;
            }

            // Store collection changes
            if (options.isStoreChanges() && property.isArrayOrCollection()) {
                Object[] items;
                if (value instanceof Object[]) {
                    items = (Object[]) value;
                }
                else {
                    Collection collection = (Collection) value;
                    items = collection.toArray();
                }

                // Map of changes
                Map<String, Object> mapCollection = new HashMap<String, Object>();
                // List of modified items
                List<Object> modifiedItems = new ArrayList<Object>();

                // Store collection changes into map
                CollectionChanges collectionChanges = collectionChangesMap.get(propertyName);
                if (collectionChanges != null) {
                    // Find all modified items (not marked items are by default modified)
                    for (Object item : items) {
                        if (collectionChanges.newItems.contains(item)) {
                            continue;
                        }
                        if (collectionChanges.deletedItems.contains(item)) {
                            throw new IllegalStateException(
                                    "Item has been marked as delete but not removed from the collection.");
                        }
                        modifiedItems.add(item);
                    }
                    if (collectionChanges.newItems.size() > 0) {
                        mapCollection.put(COLLECTION_NEW,
                                Converter.convertToMapOrArray(collectionChanges.newItems, options));
                    }
                    if (collectionChanges.deletedItems.size() > 0) {
                        mapCollection.put(COLLECTION_DELETED,
                                Converter.convertToMapOrArray(collectionChanges.deletedItems, options));
                    }
                }
                else {
                    // If no collection changes are present then all items are modified
                    for (Object item : items) {
                        modifiedItems.add(item);
                    }
                }
                if (modifiedItems.size() > 0) {
                    mapCollection.put(COLLECTION_MODIFIED,
                            Converter.convertToMapOrArray(modifiedItems, options));
                }
                // Skip empty changes
                if (mapCollection.isEmpty()) {
                    continue;
                }
                value = mapCollection;
            }


            // Convert value to map or array if the conversion is possible
            value = Converter.convertToMapOrArray(value, options);

            // Skip empty arrays
            if (value instanceof Object[] && ((Object[]) value).length == 0) {
                continue;
            }
            map.put(propertyName, value);
        }
        map.put("class", getClassShortName(getClass()));
        return map;
    }

    /**
     * Checks whether all properties with {@link Required} annotation are marked as filled (recursive).
     *
     * @throws FaultException
     */
    protected void checkRequiredPropertiesFilled() throws FaultException
    {
        checkRequiredPropertiesFilled(this);
    }

    /**
     * Check {@link Required} in all properties of {@link ComplexType} or in all items of
     * arrays and collections (recursive).
     *
     * @param object
     * @throws FaultException
     */
    private static void checkRequiredPropertiesFilled(Object object) throws FaultException
    {
        if (object instanceof ComplexType) {
            ComplexType complexType = (ComplexType) object;
            Class type = complexType.getClass();
            String[] propertyNames = Property.getPropertyNames(type);
            for (String propertyName : propertyNames) {
                Property property = Property.getProperty(complexType.getClass(), propertyName);
                Object value = property.getValue(complexType);
                boolean required = property.isRequired();
                if (property.isArray()) {
                    Object[] array = (Object[]) value;
                    if (required && array.length == 0) {
                        throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_COLLECTION_IS_REQUIRED, propertyName,
                                complexType.getClass());
                    }
                    for (Object item : array) {
                        checkRequiredPropertiesFilled(item);
                    }
                }
                else if (property.isCollection()) {
                    Collection collection = (Collection) value;
                    if (required && collection.isEmpty()) {
                        throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_COLLECTION_IS_REQUIRED, propertyName,
                                complexType.getClass());
                    }
                    for (Object item : collection) {
                        checkRequiredPropertiesFilled(item);
                    }
                }
                else if (required && value == null) {
                    throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_IS_REQUIRED, propertyName,
                            complexType.getClass());
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
