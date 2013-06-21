package cz.cesnet.shongo.oldapi.util;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.api.ClassHelper;
import cz.cesnet.shongo.oldapi.rpc.AtomicType;
import org.joda.time.*;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cz.cesnet.shongo.api.ClassHelper.getClassFromShortName;
import static cz.cesnet.shongo.api.ClassHelper.getClassShortName;

/**
 * Helper class for converting between types. The main purpose is to convert from any {@link Object} to
 * {@link TypeFlags#BASIC} {@link Map} type by attributes ({@link #convertToBasic(Object, Options)}) and
 * to convert from {@link TypeFlags#BASIC} {@link Map} to any {@link Object} again by attributes
 * ({@link #convertFromBasic(Object, Options)}).
 * <p/>
 * It is needed because XML-RPC allows only for {@link TypeFlags#BASIC} types so we must be able to serialize to them.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Converter
{
    private static Logger logger = LoggerFactory.getLogger(Converter.class);

    /**
     * Default options to be used, e.g., for {@link #convert(Object, Property)}.
     */
    private static final Options DEFAULT_OPTIONS = Options.SERVER;

    /**
     * Convert given {@code value} to a type specified by a given {@code property}.
     *
     * @param value    to be converted
     * @param property specifies target type for conversion
     * @return given {@code value} converted to type specified by given {@code property}
     */
    public static Object convert(Object value, Property property)
    {
        return convert(value, property.getType(), property.getValueAllowedTypes(), property, DEFAULT_OPTIONS);
    }

    /**
     * @see #convert(Object, Property)
     */
    public static Object convert(Object value, Class targetType)
    {
        return convert(value, targetType, null, null, DEFAULT_OPTIONS);
    }

    /**
     * @see #convert(Object, Property)
     */
    public static Object convert(Object value, Class targetType, Class[] targetAllowedTypes)
    {
        return convert(value, targetType, targetAllowedTypes, null, DEFAULT_OPTIONS);

    }

    /**
     * Convert given {@link TypeFlags#BASIC} {@code value} to:
     * 1) {@link Object} if given {@code value} is {@link Map} and has defined "class"
     * or return the same value.
     *
     * @param value   to be converted
     * @param options {@link Options}
     * @return converted given {@code value}
     */
    public static Object convertFromBasic(Object value, Options options)
    {
        return convertFromBasic(value, Object.class, options);
    }

    /**
     * Convert given {@link TypeFlags#BASIC} {@code value} to:
     * 1) {@link Object} if given {@code value} is {@link Map} and has defined "class" and to given {@code targetType}
     * the {@link Map} cannot be assigned
     * or return the same value.
     *
     * @param value      to be converted
     * @param targetType to which the given {@code value} should be converted
     * @param options    {@link Options}
     * @return converted given {@code value} to given {@code targetType}
     */
    public static Object convertFromBasic(Object value, Class targetType, Options options)
    {
        Class valueType = value.getClass();
        int valueTypeFlags = TypeFlags.get(valueType);
        if (!TypeFlags.isBasic(valueTypeFlags)) {
            throw new ConverterException("Type '%s' isn't basic." + value.getClass().getCanonicalName());
        }
        if (value instanceof Map) {
            Map map = (Map) value;
            if (map.containsKey("class") || !targetType.isAssignableFrom(valueType)) {
                return convertMapToObject(map, targetType, options);
            }
        }
        if (!targetType.isAssignableFrom(value.getClass())) {
            return convert(value, targetType, null, null, options);
        }
        return value;
    }

    /**
     * Convert given {@code value} to {@link TypeFlags#BASIC} type.
     *
     * @param value   to be converted
     * @param options {@link Options}
     * @return converted given {@code value} to {@link TypeFlags#BASIC} type
     */
    public static Object convertToBasic(Object value, Options options)
    {
        if (value == null) {
            return value;
        }
        else if (TypeFlags.isAtomic(TypeFlags.get(value))) {
            return convertAtomicToBasic(value);
        }
        else if (value instanceof Collection) {
            Collection collection = (Collection) value;
            Object[] newArray = new Object[collection.size()];
            int index = 0;
            for (Object item : collection) {
                newArray[index] = convertToBasic(item, options);
                index++;
            }
            return newArray;
        }
        else if (value instanceof Object[]) {
            Object[] oldArray = (Object[]) value;
            Object[] newArray = new Object[oldArray.length];
            for (int index = 0; index < oldArray.length; index++) {
                newArray[index] = convertToBasic(oldArray[index], options);
            }
            return newArray;
        }
        else if (value instanceof Map) {
            Map oldMap = (Map) value;
            Map<Object, Object> newMap = new HashMap<Object, Object>();
            for (Object itemKey : oldMap.keySet()) {
                Object itemValue = oldMap.get(itemKey);
                itemKey = convertToBasic(itemKey, options);
                itemValue = convertToBasic(itemValue, options);
                newMap.put(itemKey, itemValue);
            }
            return newMap;
        }
        return convertObjectToMap(value, options);
    }

    /**
     * Convert given {@code value} to appropriate {@link TypeFlags#ATOMIC} type.
     *
     * @param value to be converted
     * @return converted given {@code value} to appropriate {@link TypeFlags#BASIC}
     */
    private static Object convertAtomicToBasic(Object value)
    {
        if (TypeFlags.isPrimitive(TypeFlags.get(value))) {
            return value;
        }
        if (value instanceof Class) {
            Class type = (Class) value;
            return ClassHelper.getClassShortName(type);
        }
        if (value instanceof Interval) {
            Interval interval = (Interval) value;
            return Atomic.convertIntervalToString(interval);
        }
        return value.toString();
    }

    /**
     * Convert object to a map. Map will contain all object's properties that are:
     * 1) simple (not {@link Object[]} or {@link Collection}) with not {@link null} value
     * 2) simple with {@link null} value but marked as filled through
     * ({@link ChangesTracking#markPropertyAsFilled(String)}
     * 3) {@link Object[]} or {@link Collection} which is not empty
     *
     * @param object  to be converted
     * @param options see {@link Options}
     * @return map which contains object's properties
     */
    private static Map convertObjectToMap(Object object, Options options)
    {
        ChangesTracking changesTrackingObject =
                ((object instanceof ChangesTracking.Changeable) ?
                         ((ChangesTracking.Changeable) object).getChangesTracking() : null);

        Map<String, Object> map = new HashMap<String, Object>();
        Collection<String> propertyNames = Property.getClassHierarchyPropertyNames(object.getClass());
        for (String propertyName : propertyNames) {
            Property property = Property.getProperty(object.getClass(), propertyName);
            if (property == null) {
                throw new ConverterException("Cannot get property '%s' from class '%s'.",
                        propertyName, object.getClass());
            }

            // Skip read-only properties
            if (property.isReadOnly() && !options.isStoreReadOnly()) {
                continue;
            }

            if (changesTrackingObject != null && options.isStoreChanges()) {
                if (!changesTrackingObject.isPropertyFilled(propertyName)) {
                    continue;
                }
            }

            // Get property value
            Object value = property.getValue(object);

            // If value is empty, it can be skipped in some cases
            if (property.isEmptyValue(value)) {
                // Skip empty values if we do not track changes (a missing value is interpreted as empty)
                if (changesTrackingObject == null) {
                    continue;
                }
                // Skip empty values if we should not save changes (a missing value is interpreted as empty)
                if (!options.isStoreChanges()) {
                    continue;
                }
                // If value was not filled (e.g., modified) we should skip empty values too
                if (!changesTrackingObject.isPropertyFilled(propertyName)) {
                    continue;
                }
            }

            // If changes should be stored and the property has items, we must save the property by custom structure
            // with "new"/"modified"/"deleted" arrays
            if (options.isStoreChanges() && TypeFlags.isArrayOrCollectionOrMap(property.getTypeFlags())) {
                ChangesTracking.CollectionChanges collectionChanges =
                        (changesTrackingObject != null ?
                                 changesTrackingObject.getCollectionChanges(propertyName) : null);
                value = getValueItemChanges(value, collectionChanges, options);
                if (value == null) {
                    // No changes so skip the property
                    continue;
                }
                // No further conversion of value is needed (already performed inside the getValueItemChanges)
            }
            // Only convert value
            else {
                // Convert value to basic if the conversion is possible
                value = Converter.convertToBasic(value, options);
            }

            // Skip empty arrays
            if (value instanceof Object[] && ((Object[]) value).length == 0) {
                continue;
            }
            map.put(propertyName, value);
        }
        map.put("class", getClassShortName(object.getClass()));
        return map;
    }

    /**
     * Convert given {@code map} to {@link Object} of {@code targetType} ("class" item can also specify object's class).
     *
     * @param map        to be converted
     * @param targetType target type for conversion
     * @param options    see {@link Options}
     * @return new instance of given {@code targetType} that is filled by attributes from given {@code map}
     */
    private static Object convertMapToObject(Map map, Class targetType, Options options)
    {
        // Null or empty map means "null" object
        if (map == null || map.size() == 0) {
            return null;
        }
        else {
            // Check proper class
            if (map.containsKey("class")) {
                String className = (String) map.get("class");
                Class declaredType = targetType;
                try {
                    targetType = getClassFromShortName(className);
                }
                catch (ClassNotFoundException exception) {
                    throw new CommonReportSet.ClassUndefinedException(className);
                }
                if (!declaredType.isAssignableFrom(targetType)) {
                    throw new ConverterException("Cannot convert map to object of class '%s'"
                            + " because map specifies not assignable class '%s'.",
                            getClassShortName(declaredType), className);
                }
            }

            // Create new instance of object
            Object object = null;
            try {
                object = targetType.newInstance();
            }
            catch (Exception exception) {
                throw new CommonReportSet.ClassInstantiationErrorException(targetType.getSimpleName());
            }

            ChangesTracking changesTrackingObject =
                    ((object instanceof ChangesTracking.Changeable) ?
                             ((ChangesTracking.Changeable) object).getChangesTracking() : null);

            // Clear all filled properties
            if (changesTrackingObject != null) {
                changesTrackingObject.clearMarks();
            }

            // Fill each property that is present in map
            for (Object key : map.keySet()) {
                if (!(key instanceof String)) {
                    throw new ConverterException("Map must contain only string keys.");
                }
                String propertyName = (String) key;
                Object value = map.get(key);

                // Skip class property
                if (propertyName.equals("class")) {
                    continue;
                }

                Property property = Property.getPropertyNotNull(object.getClass(), propertyName);
                if (property.isReadOnly() && !options.isLoadReadOnly()) {
                    throw new CommonReportSet.ClassAttributeReadonlyException(
                            object.getClass().getSimpleName(), propertyName);
                }

                // Set changes for items
                int propertyTypeFlags = property.getTypeFlags();
                boolean storeChanges = (value instanceof Map)
                        && (TypeFlags.isArrayOrCollection(propertyTypeFlags)
                                    || (TypeFlags.isMap(propertyTypeFlags)
                                                && ((Map) value).containsKey(ChangesTracking.MAP_DATA)));
                if (storeChanges) {
                    value = setValueItemChanges(value, property, changesTrackingObject, options);
                    if (value == null) {
                        // No changes so skip the property
                        continue;
                    }
                }
                // Only convert value
                else {
                    try {
                        value = Converter.convert(value, property.getType(), property.getValueAllowedTypes(),
                                property, options);
                    }
                    catch (IllegalArgumentException exception) {
                        Object requiredType = property.getType();
                        Object givenType = value.getClass();
                        if (property.getValueAllowedTypes() != null) {
                            StringBuilder builder = new StringBuilder();
                            for (Class allowedType : property.getValueAllowedTypes()) {
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
                        else {
                            givenType = givenType.getClass().getSimpleName();
                        }
                        throw new CommonReportSet.ClassAttributeTypeMismatchException(
                                object.getClass().getSimpleName(), propertyName,
                                requiredType.getClass().getSimpleName(), givenType.toString());
                    }
                }

                // Set the value to property
                property.setValue(object, value, options.isForceAccessible());

                // Mark property as filled
                if (changesTrackingObject != null) {
                    changesTrackingObject.markPropertyAsFilled(propertyName);
                }
            }

            return object;
        }
    }

    /**
     * Get item changes for given {@code value}.
     *
     * @param value             of type {@link Object[]}, {@link Collection} or {@link Map}
     * @param collectionChanges changes which should be stored
     * @param options           see {@link Options}
     * @return {@link Map} containing changes for given {@code value} or null when no changes are present
     *         (the changes are also converted to {@link TypeFlags#BASIC} types)
     */
    private static Map<String, Object> getValueItemChanges(Object value,
            ChangesTracking.CollectionChanges collectionChanges, Options options)
    {
        // Map of changes
        Map<String, Object> mapValueItemChanges = new HashMap<String, Object>();

        // Get items from value (for Map the keys are used)
        Object[] items;
        if (value instanceof Object[]) {
            items = (Object[]) value;
        }
        else if (value instanceof Map) {
            Set keys = ((Map) value).keySet();
            items = keys.toArray();
        }
        else {
            Collection collection = (Collection) value;
            items = collection.toArray();
        }

        // List of modified items
        List<Object> modifiedItems = new ArrayList<Object>();

        // Store collection changes into map
        if (collectionChanges != null) {
            // Find all modified items (not marked items are by default modified)
            List<Object> newItems = new ArrayList<Object>();
            for (Object item : items) {
                if (collectionChanges.isItemNew(item)) {
                    newItems.add(item);
                }
                else if (collectionChanges.isItemDeleted(item)) {
                    throw new ConverterException("Item has been marked as delete but not removed from the collection.");
                }
                else {
                    modifiedItems.add(item);
                }
            }
            if (newItems.size() > 0) {
                mapValueItemChanges.put(ChangesTracking.COLLECTION_NEW, Converter.convertToBasic(newItems, options));
            }
            Collection<Object> deletedItems = collectionChanges.getDeletedItems();
            if (deletedItems.size() > 0) {
                mapValueItemChanges.put(ChangesTracking.COLLECTION_DELETED,
                        Converter.convertToBasic(deletedItems, options));
            }
        }
        else {
            // If no collection changes are present then all items are modified
            for (Object item : items) {
                modifiedItems.add(item);
            }
        }
        if (modifiedItems.size() > 0) {
            mapValueItemChanges.put(ChangesTracking.COLLECTION_MODIFIED,
                    Converter.convertToBasic(modifiedItems, options));
        }
        // Skip empty changes
        if (mapValueItemChanges.isEmpty()) {
            return null;
        }
        // Append values for Map
        if (value instanceof Map) {
            mapValueItemChanges.put(ChangesTracking.MAP_DATA, value);
        }
        return mapValueItemChanges;
    }

    /**
     * Set changes from given {@code value} to the given {@code changesTracking} and return value which should
     * be set to the {@code property}.
     *
     * @param value           of type {@link Object[]}, {@link Collection} or {@link Map}
     * @param property        to which the value belongs
     * @param changesTracking {@link ChangesTracking} to which the changes should be filled
     * @param options         see {@link Options}
     * @return value which should be set to the given {@code property} (converted from{@link TypeFlags#BASIC} types)
     */
    private static Object setValueItemChanges(Object value, Property property, ChangesTracking changesTracking,
            Options options)
    {
        Map changes = (Map) value;

        // Get property type and type flags
        String propertyName = property.getName();
        Class propertyType = property.getType();
        Class[] propertyAllowedTypes = property.getValueAllowedTypes();
        int propertyTypeFlags = property.getTypeFlags();

        Class changesType = propertyType;
        Class[] changesAllowedTypes = propertyAllowedTypes;
        if (TypeFlags.isMap(propertyTypeFlags)) {
            changesType = Set.class;
            changesAllowedTypes = new Class[]{property.getKeyAllowedType()};
        }

        // Get changes
        Object newItems = null;
        Object modifiedItems = null;
        Object deletedItems = null;
        if (changes.containsKey(ChangesTracking.COLLECTION_NEW)) {
            newItems = changes.get(ChangesTracking.COLLECTION_NEW);
            newItems = Converter.convert(newItems, changesType, changesAllowedTypes, property, options);
            if (changesTracking != null) {
                for (Object newItem : (Collection) newItems) {
                    changesTracking.markPropertyItemAsNew(propertyName, newItem);
                }
            }
        }
        if (changes.containsKey(ChangesTracking.COLLECTION_MODIFIED)) {
            modifiedItems = changes.get(ChangesTracking.COLLECTION_MODIFIED);
            modifiedItems = Converter.convert(modifiedItems, changesType, changesAllowedTypes, property, options);
        }
        if (changes.containsKey(ChangesTracking.COLLECTION_DELETED)) {
            deletedItems = changes.get(ChangesTracking.COLLECTION_DELETED);
            deletedItems = Converter.convert(deletedItems, changesType, changesAllowedTypes, property, options);
            if (changesTracking != null) {
                for (Object deletedItem : (Collection) deletedItems) {
                    changesTracking.markPropertyItemAsDeleted(propertyName, deletedItem);
                }
            }
        }
        if (newItems == null && modifiedItems == null && deletedItems == null) {
            return null;
        }
        if (TypeFlags.isArray(propertyTypeFlags)) {
            int size = (newItems != null ? ((Object[]) newItems).length : 0)
                    + (modifiedItems != null ? ((Object[]) modifiedItems).length : 0);
            int index = 0;
            Object[] array = ClassHelper.createArray(propertyType.getComponentType(), size);
            if (newItems != null) {
                for (Object newItem : (Object[]) newItems) {
                    array[index++] = newItem;
                }
            }
            if (modifiedItems != null) {
                for (Object modifiedItem : (Object[]) modifiedItems) {
                    array[index++] = modifiedItem;
                }
            }
            value = array;
        }
        else if (TypeFlags.isCollection(propertyTypeFlags)) {
            Collection<Object> collection = ClassHelper.createCollection(propertyType, 0);
            if (newItems != null) {
                for (Object newItem : (Collection) newItems) {
                    collection.add(newItem);
                }
            }
            if (modifiedItems != null) {
                for (Object modifiedItem : (Collection) modifiedItems) {
                    collection.add(modifiedItem);
                }
            }
            value = collection;
        }
        else if (TypeFlags.isMap(propertyTypeFlags)) {
            Map map = (Map) value;
            value = convert(map.get(ChangesTracking.MAP_DATA), property.getType(), propertyAllowedTypes,
                    property, options);
        }
        return value;
    }

    /**
     * Convert given {@code value} to a {@code targetType}.
     *
     * @param value              which should be converted
     * @param targetType         to which the given {@code value} should be converted
     * @param targetAllowedTypes restricts types to which the value can be converted
     * @param property           specifies property for whose value the conversion is done
     * @param options            see {@link Options}
     * @return converted value
     * @throws IllegalArgumentException when the value cannot be converted to specified type
     */
    private static Object convert(Object value, Class targetType, Class[] targetAllowedTypes, Property property,
            Options options)
    {
        // Null values aren't converted
        if (value == null) {
            return null;
        }

        // Get type flags
        int valueTypeFlags = TypeFlags.get(value);
        int targetTypeFlags = TypeFlags.get(targetType);

        // Iterate through allowed types for value and try the conversion for each one (this operation should not be
        // performed for types containing items, it will be performed for each item in nested call)
        if (targetAllowedTypes != null && targetAllowedTypes.length > 0
                && !TypeFlags.isArrayOrCollectionOrMap(targetTypeFlags)) {
            // When not converting array/collection
            if (targetAllowedTypes.length == 1) {
                // If only one allowed type is present set the single allowed type as target type
                targetType = targetAllowedTypes[0];
            }
            else {
                // Iterate through each allowed type and try to convert the value to it
                Object allowedValue = null;
                List<Exception> exceptionList = new ArrayList<Exception>(targetAllowedTypes.length);
                for (Class allowedType : targetAllowedTypes) {
                    try {
                        allowedValue = Converter.convert(value, allowedType, null, null, options);
                        break;
                    }
                    catch (Exception exception) {
                        exceptionList.add(exception);
                    }
                }
                if (allowedValue != null) {
                    return allowedValue;
                }
                else {
                    for (int index = 0; index < exceptionList.size(); index++) {
                        logger.debug(String.format("Cannot convert value '%s' to '%s'.",
                                value.getClass().getCanonicalName(), targetAllowedTypes[index].getCanonicalName()),
                                exceptionList.get(index));
                    }
                    throw new IllegalArgumentException();
                }
            }
        }

        // If types are compatible
        if (targetType.isAssignableFrom(value.getClass())) {
            // Process map
            if (value instanceof Map) {
                // Convert keys to proper type
                if (property != null && property.getKeyAllowedType() != null) {
                    Map oldMap = (Map) value;
                    Map<Object, Object> newMap = new HashMap<Object, Object>();
                    for (Object itemKey : oldMap.keySet()) {
                        Object itemValue = oldMap.get(itemKey);
                        itemKey = convert(itemKey, property.getKeyAllowedType(), null, null, options);
                        itemValue = convert(itemValue, Object.class, targetAllowedTypes, null, options);
                        newMap.put(itemKey, itemValue);
                    }
                    return newMap;
                }
            }
            // Process collection
            else if (value instanceof Collection) {
                // Convert collection items to proper type
                Collection collectionValue = (Collection) value;
                Collection<Object> collection = ClassHelper.createCollection(targetType, collectionValue.size());
                for (Object item : collectionValue) {
                    item = convert(item, Object.class, targetAllowedTypes, null, options);
                    if (item == null) {
                        throw new CommonReportSet.CollectionItemNullException(property.getName());
                    }
                    collection.add(item);
                }
                return collection;
            }
            // Do nothing
            return value;
        }
        // Convert from primitive types
        else if (TypeFlags.isPrimitive(valueTypeFlags)) {
            if (targetType.equals(String.class)) {
                return value.toString();
            }
            else if (targetType.equals(Boolean.class) && value instanceof Integer) {
                return ((Integer) value).intValue() != 0;
            }
            else if (TypeFlags.isPrimitive(targetTypeFlags)) {
                return value;
            }
        }
        // Convert from date
        else if (value instanceof Date) {
            if (targetType.equals(DateTime.class)) {
                return new DateTime(value);
            }
        }
        // Convert from string
        else if (value instanceof String) {
            // If Class is required
            if (targetType.equals(Class.class)) {
                String className = (String) value;
                try {
                    return ClassHelper.getClassFromShortName(className);
                }
                catch (ClassNotFoundException exception) {
                    throw new CommonReportSet.ClassUndefinedException(className);
                }
            }
            // If boolean is required
            if (targetType.equals(Boolean.class)) {
                return Boolean.parseBoolean((String) value);
            }
            // If boolean is required
            else if (targetType.equals(Long.class)) {
                return Long.parseLong((String) value);
            }
            // If enum is required
            else if (targetType.isEnum() && value instanceof String) {
                return Atomic.convertStringToEnum((String) value, (Class<Enum>) targetType);
            }
            // If atomic type is required
            else if (AtomicType.class.isAssignableFrom(targetType) && value instanceof String) {
                AtomicType atomicType = null;
                try {
                    atomicType = (AtomicType) targetType.newInstance();
                }
                catch (Exception exception) {
                    throw new CommonReportSet.ClassInstantiationErrorException(targetType.getSimpleName());
                }
                atomicType.fromString((String) value);
                return atomicType;
            }
            // If period is required
            else if (Period.class.isAssignableFrom(targetType)) {
                return Atomic.convertStringToPeriod((String) value);
            }
            // If date/time is required
            else if (DateTime.class.isAssignableFrom(targetType)) {
                return Atomic.convertStringToDateTime((String) value);
            }
            // If interval is required
            else if (ReadablePartial.class.isAssignableFrom(targetType)) {
                return Atomic.convertStringToReadablePartial((String) value);
            }
            // If interval is required
            else if (Interval.class.isAssignableFrom(targetType)) {
                return Atomic.convertStringToInterval((String) value);
            }
        }
        // Convert to string
        else if (targetType.equals(String.class)) {
            if (TypeFlags.isAtomic(valueTypeFlags)) {
                if (value instanceof Interval) {
                    return Atomic.convertIntervalToString((Interval) value);
                }
                return value.toString();
            }
        }
        // Convert from array
        else if (value instanceof Object[]) {
            // Convert to array
            if (targetType.isArray()) {
                // Convert array to specific type
                Class componentType = targetType.getComponentType();
                Object[] arrayValue = (Object[]) value;
                Object[] newArray = ClassHelper.createArray(componentType, arrayValue.length);
                for (int index = 0; index < arrayValue.length; index++) {
                    Object item = convert(arrayValue[index], componentType, targetAllowedTypes, null, options);
                    if (item == null) {
                        throw new CommonReportSet.CollectionItemNullException(property.getName());
                    }
                    newArray[index] = item;
                }
                return newArray;
            }
            // Convert to collection
            else if (Collection.class.isAssignableFrom(targetType)) {
                // Convert collection to specific type
                Object[] arrayValue = (Object[]) value;
                Collection<Object> collection = ClassHelper.createCollection(targetType, arrayValue.length);
                for (Object item : arrayValue) {
                    item = convert(item, Object.class, targetAllowedTypes, null, options);
                    if (item == null) {
                        throw new CommonReportSet.CollectionItemNullException(property.getName());
                    }
                    collection.add(item);
                }
                return collection;
            }
        }
        // Convert from map to object
        else if (value instanceof Map) {
            return convertMapToObject((Map) value, targetType, options);
        }
        throw new ConverterException("Cannot convert value of type '%s' to '%s'.",
                value.getClass().getCanonicalName(), targetType.getCanonicalName());
    }

    /**
     * Convert given {@code value} to a type specified by a given {@code property}.
     *
     * @see #convert(Object, Class, Class[], Property, Options)
     */
    private static Object convert(Object value, Property property, Options options)
    {
        return convert(value, property.getType(), property.getValueAllowedTypes(), property, options);
    }

    /**
     * Helper functions for {@link TypeFlags#ATOMIC} types.
     */
    public static class Atomic
    {
        /**
         * Array of supported partial fields (in the same order as in the partial regex pattern).
         */
        private static final DateTimeFieldType[] PARTIAL_FIELDS = new DateTimeFieldType[]{
                DateTimeFieldType.year(),
                DateTimeFieldType.monthOfYear(),
                DateTimeFieldType.dayOfMonth(),
                DateTimeFieldType.hourOfDay(),
                DateTimeFieldType.minuteOfHour()
        };

        /**
         * {@link org.joda.time.DateTime#getMillis()} for {@link Temporal#DATETIME_INFINITY_START}
         */
        private static final long DATETIME_INFINITY_START_MILLIS = Temporal.DATETIME_INFINITY_START.getMillis();

        /**
         * {@link org.joda.time.DateTime#getMillis()} for {@link Temporal#DATETIME_INFINITY_END}
         */
        private static final long DATETIME_INFINITY_END_MILLIS = Temporal.DATETIME_INFINITY_END.getMillis();

        /**
         * Convert string to enum type.
         *
         * @param value
         * @param enumClass
         * @return enum value for given string from specified enum class
         * @throws CommonReportSet.TypeIllegalValueException
         */
        public static <T extends Enum<T>> T convertStringToEnum(String value, Class<T> enumClass)
        {
            try {
                return Enum.valueOf(enumClass, value);
            }
            catch (IllegalArgumentException exception) {
                throw new CommonReportSet.TypeIllegalValueException(getClassShortName(enumClass), value);
            }
        }

        /**
         * @param value
         * @return parsed date/time from string
         * @throws CommonReportSet.TypeIllegalValueException,
         */
        public static DateTime convertStringToDateTime(String value)
        {
            DateTime dateTime;
            try {
                dateTime = ISODateTimeFormat.dateTimeParser().parseDateTime(value);
            }
            catch (Exception exception) {
                throw new CommonReportSet.TypeIllegalValueException(DateTime.class.getSimpleName(), value);
            }
            final long millis = dateTime.getMillis();
            if (millis < DATETIME_INFINITY_START_MILLIS || millis > DATETIME_INFINITY_END_MILLIS) {
                throw new CommonReportSet.TypeIllegalValueException(DateTime.class.getSimpleName(), value);
            }
            return dateTime;
        }

        /**
         * @param value
         * @return parsed partial date/time from string
         * @throws CommonReportSet.TypeIllegalValueException
         */
        public static ReadablePartial convertStringToReadablePartial(String value)
        {
            Pattern pattern = Pattern.compile("(\\d{1,4})(-\\d{1,2})?(-\\d{1,2})?(T\\d{1,2})?(:\\d{1,2})?");
            Matcher matcher = pattern.matcher(value);
            if (matcher.matches()) {
                Partial partial = new Partial();
                for (int index = 0; index < PARTIAL_FIELDS.length; index++) {
                    String group = matcher.group(index + 1);
                    if (group == null) {
                        continue;
                    }
                    char first = group.charAt(0);
                    if (first < '0' || first > '9') {
                        group = group.substring(1, group.length());
                    }
                    partial = partial.with(PARTIAL_FIELDS[index], Integer.parseInt(group));
                }
                return partial;
            }
            throw new CommonReportSet.TypeIllegalValueException("PartialDateTime", value);
        }

        /**
         * @param value
         * @return parsed period from string
         * @throws CommonReportSet.TypeIllegalValueException
         */
        public static Period convertStringToPeriod(String value)
        {
            try {
                Period period = Period.parse(value);
                return period;
            }
            catch (Exception exception) {
                throw new CommonReportSet.TypeIllegalValueException(Period.class.getSimpleName(), value);
            }
        }

        /**
         * Method for converting {@link String} to {@link Interval}.
         * <p/>
         * The class {@link Interval} itself is not able to preserve chronology (e.g., "+01:00") when parsing
         * {@link Interval} by {@link Interval#parse(String)} (the format is "{@code <start>/<end>}").
         * And thus to preserve the chronology we must implement the parsing by hand, and this implementation use
         * the format "{@code <start>/<end>}" where the {@code <start>} and {@code <end>} is parsed by
         * {@link #convertStringToDateTime(String)} from the parsed {@link DateTime}s is constructed
         * the resulting {@link Interval}.
         *
         * @param value string value to be converted to the {@link Interval}
         * @return parsed {@link Interval} from given {@code value}
         * @throws CommonReportSet.TypeIllegalValueException
         */
        public static Interval convertStringToInterval(String value)
        {
            String[] parts = value.split("/");
            if (parts.length == 2) {
                String startString = parts[0];
                String endString = parts[1];
                DateTime start;
                DateTime end;
                if (startString.equals(Temporal.INFINITY_ALIAS)) {
                    start = Temporal.DATETIME_INFINITY_START;
                }
                else {
                    start = convertStringToDateTime(startString);
                }
                if (endString.equals(Temporal.INFINITY_ALIAS)) {
                    end = Temporal.DATETIME_INFINITY_END;
                }
                else {
                    end = convertStringToDateTime(endString);
                }
                try {
                    return new Interval(start, end);
                }
                catch (IllegalArgumentException exception) {
                    throw new CommonReportSet.TypeIllegalValueException(Interval.class.getSimpleName(), value);
                }
            }
            throw new CommonReportSet.TypeIllegalValueException(Interval.class.getSimpleName(), value);
        }

        /**
         * Method for converting {@link Interval} to {@link String}.
         *
         * @param interval to be converted to {@link String}
         * @return converted {@link Interval} to {@link String}
         * @see #convertStringToInterval(String)
         */
        public static String convertIntervalToString(Interval interval)
        {
            String startString;
            String endString;
            if (interval.getStartMillis() == DATETIME_INFINITY_START_MILLIS) {
                startString = Temporal.INFINITY_ALIAS;
            }
            else {
                startString = interval.getStart().toString();
            }
            if (interval.getEndMillis() == DATETIME_INFINITY_END_MILLIS) {
                endString = Temporal.INFINITY_ALIAS;
            }
            else {
                endString = interval.getEnd().toString();
            }
            return String.format("%s/%s", startString, endString);
        }
    }
}
