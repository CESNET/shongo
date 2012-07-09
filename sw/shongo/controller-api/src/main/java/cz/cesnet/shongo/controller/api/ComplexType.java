package cz.cesnet.shongo.controller.api;

import cz.cesnet.shongo.controller.api.util.Converter;
import cz.cesnet.shongo.controller.api.util.Property;

import java.awt.event.ComponentEvent;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
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
     * Annotation used for properties that must be presented when creating new entity.
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
        Class[] value();

        Class[] defaultValue() default {};
    }

    /**
     * Stores state of collection.
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
     * Object identifier
     */
    private Object identifier;

    /**
     * Set of fields which was marked as filled.
     */
    private Set<String> filledProperties = new HashSet<String>();

    /**
     * Map of changes for collections.
     */
    private Map<String, CollectionChanges> collectionChangesMap = new HashMap<String, CollectionChanges>();

    /**
     * @return {@link #identifier}
     */
    public Object getIdentifier()
    {
        return identifier;
    }

    /**
     * @param identifier sets the {@link #identifier}
     */
    void setIdentifier(Object identifier)
    {
        this.identifier = identifier;
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
     * Clear all filled/collection marks
     */
    public void clearMarks()
    {
        filledProperties.clear();
        collectionChangesMap.clear();
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
        if ( collectionChanges == null ) {
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
    protected void markCollectionItemAsRemoved(String property, Object item)
    {
        CollectionChanges collectionChanges = collectionChangesMap.get(property);
        if ( collectionChanges == null ) {
            collectionChanges = new CollectionChanges();
            collectionChangesMap.put(property, collectionChanges);
        }
        if ( collectionChanges.newItems.contains(item)) {
            collectionChanges.newItems.remove(item);
        } else {
            collectionChanges.deletedItems.add(item);
        }
    }

    /**
     * Fill object from the given map.
     *
     * @param map
     * @throws FaultException
     */
    public void fromMap(Map map) throws FaultException
    {
        // Clear all filled properties
        clearMarks();

        // Fill each property that is present in map
        for (Object key : map.keySet()) {
            if (!(key instanceof String)) {
                throw new FaultException(Fault.Common.UNKNOWN_FAULT, "Map must contain only string keys.");
            }
            String property = (String) key;
            Object value = map.get(key);

            // Skip class property
            if (property.equals("class")) {
                continue;
            }

            // Get property type and allowed types
            Property propertyDefinition = Property.getPropertyNotNull(getClass(), property);
            Class type = propertyDefinition.getType();
            Class[] allowedTypes = propertyDefinition.getAllowedTypes();

            try {
                value = Converter.convert(value, type, allowedTypes);
            }
            catch (IllegalArgumentException exception) {
                /*StringBuilder builder = new StringBuilder();
                for (Class allowedType : allowedTypes) {
                    if (builder.length() > 0) {
                        builder.append("|");
                    }
                    builder.append(Converter.getClassShortName(allowedType));
                }*/
                throw new FaultException(exception, Fault.Common.CLASS_ATTRIBUTE_TYPE_MISMATCH, property,
                        getClass(),
                        type,
                        value.getClass());
            }

            // Set the value to property
            Property.setPropertyValue(this, property, value);

            // Mark property as filled
            markPropertyAsFilled(property);
        }
    }

    /**
     * Convert object to a map. Put to map all properties that are not {@code null} or marked as filled. Don't put to
     * map empty arrays or collections.
     *
     * @return map
     * @throws FaultException
     */
    public Map toMap() throws FaultException
    {
        Map<String, Object> map = new HashMap<String, Object>();
        String[] propertyNames = Property.getPropertyNames(getClass());
        for (String propertyName : propertyNames) {
            Property property = Property.getProperty(getClass(), propertyName);
            Object value = property.getValue(this);
            if (value == null && !isPropertyFilled(propertyName)) {
                continue;
            }

            // If property is collection with changes
            if ( collectionChangesMap.containsKey(propertyName)) {
                CollectionChanges collectionChanges = collectionChangesMap.get(propertyName);
                // Skip empty changes
                if (!collectionChanges.isEmpty()) {
                    Object[] items = null;
                    if ( value instanceof Object[]) {
                        items = (Object[]) value;
                    } else {
                        Collection collection = (Collection) value;
                        items = collection.toArray(new Object[collection.size()]);
                    }
                    // Get modified items from current items in collection
                    List<Object> modifiedItems = new ArrayList<Object>();
                    for ( Object item : items ) {
                        if ( collectionChanges.newItems.contains(item)) {
                            continue;
                        }
                        if ( collectionChanges.deletedItems.contains(item)) {
                            throw new IllegalStateException(
                                    "Item has been marked as delete but not removed from the collection.");
                        }
                        modifiedItems.add(item);
                    }

                    // Pass only list of new items
                    if ( modifiedItems.size() == 0 && collectionChanges.deletedItems.size() == 0) {
                        value = collectionChanges.newItems;
                    }
                    // Use new/modified/delete map
                    else {
                        Map<String, Object> mapCollection = new HashMap<String, Object>();
                        if (collectionChanges.newItems.size() > 0 ) {
                            mapCollection.put("new", Converter.convertToMapOrArray(collectionChanges.newItems));
                        }
                        if (collectionChanges.deletedItems.size() > 0 ) {
                            mapCollection.put("deleted", Converter.convertToMapOrArray(collectionChanges.deletedItems));
                        }



                        if (modifiedItems.size() > 0 ) {
                            mapCollection.put("modified", Converter.convertToMapOrArray(modifiedItems));
                        }

                        value = mapCollection;
                    }
                }
            }

            // Convert value to map or array if the conversion is possible
            value = Converter.convertToMapOrArray(value);

            // Skip empty arrays
            if ( value instanceof Object[] && ((Object[]) value).length == 0) {
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
