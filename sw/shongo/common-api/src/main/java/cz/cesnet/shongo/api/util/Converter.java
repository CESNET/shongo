package cz.cesnet.shongo.api.util;

import cz.cesnet.shongo.api.AtomicType;
import cz.cesnet.shongo.api.Fault;
import cz.cesnet.shongo.api.FaultException;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import java.lang.reflect.Array;
import java.util.*;

import static cz.cesnet.shongo.api.util.ClassHelper.getClassFromShortName;
import static cz.cesnet.shongo.api.util.ClassHelper.getClassShortName;

/**
 * Helper class for converting types.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Converter
{
    /**
     * Convert given value to target type with list of allowed types.
     *
     * @param value
     * @param targetType
     * @param allowedTypes
     * @param name
     * @param options
     * @return converted value
     * @throws FaultException
     * @throws IllegalArgumentException
     */
    public static Object convert(Object value, Class targetType, Class[] allowedTypes, String name, Options options)
            throws FaultException, IllegalArgumentException
    {
        if (value == null) {
            return null;
        }

        if (allowedTypes != null && allowedTypes.length > 0) {
            // When some allowed types are present
            if (!(value instanceof Object[] || value instanceof Collection)) {
                // When not converting array/collection
                if (allowedTypes.length == 1) {
                    // If only one allowed type is present set the single allowed type as target type
                    targetType = allowedTypes[0];
                }
                else {
                    // Iterate through each allowed type and try to convert the value to it
                    Object allowedValue = null;
                    for (Class allowedType : allowedTypes) {
                        try {
                            allowedValue = Converter.convert(value, allowedType, null, null, options);
                            break;
                        }
                        catch (Exception exception) {
                        }
                    }
                    if (allowedValue != null) {
                        return allowedValue;
                    }
                    else {
                        throw new IllegalArgumentException();
                    }
                }
            }
        }

        // If types are compatible
        if (targetType.isAssignableFrom(value.getClass())) {
            // Do nothing
            return value;
        }
        // Convert from primitve types
        else if (primitiveClassess.contains(value.getClass())) {
            if (targetType.isPrimitive()) {
                return value;
            }
            else if (targetType.equals(String.class)) {
                return value.toString();
            }
        }
        // Convert from date
        else if (value instanceof Date && DateTime.class.isAssignableFrom(targetType)) {
            return new DateTime(value);
        }
        // Convert atomic types
        else if (value instanceof String) {
            // If enum is required
            if (targetType.isEnum() && value instanceof String) {
                return Converter.convertStringToEnum((String) value, (Class<Enum>) targetType);
            }
            // If atomic type is required
            else if (AtomicType.class.isAssignableFrom(targetType) && value instanceof String) {
                AtomicType atomicType = null;
                try {
                    atomicType = (AtomicType) targetType.newInstance();
                }
                catch (Exception exception) {
                    throw new RuntimeException(new FaultException(Fault.Common.CLASS_CANNOT_BE_INSTANCED,
                            targetType));
                }
                atomicType.fromString((String) value);
                return atomicType;
            }
            // If period is required
            else if (Period.class.isAssignableFrom(targetType)) {
                return convertStringToPeriod((String) value);
            }
            // If date/time is required
            else if (DateTime.class.isAssignableFrom(targetType)) {
                return convertStringToDateTime((String) value);
            }
            // If interval is required
            else if (Interval.class.isAssignableFrom(targetType)) {
                return convertStringToInterval((String) value);
            }
        }
        // Convert array types
        else if (value instanceof Object[]) {
            if (targetType.isArray()) {
                // Convert array to specific type
                Class componentType = targetType.getComponentType();
                Object[] arrayValue = (Object[]) value;
                Object[] newArray = createArray(componentType, arrayValue.length);
                for (int index = 0; index < arrayValue.length; index++) {
                    Object item = convert(arrayValue[index], componentType, allowedTypes, null, options);
                    if (item == null) {
                        throw new FaultException(Fault.Common.COLLECTION_ITEM_NULL, name);
                    }
                    newArray[index] = item;
                }
                return newArray;
            }
            else if (Collection.class.isAssignableFrom(targetType)) {
                // Convert collection to specific type
                Object[] arrayValue = (Object[]) value;
                Collection<Object> collection = createCollection(targetType, arrayValue.length);
                for (Object item : arrayValue) {
                    item = convert(item, Object.class, allowedTypes, null, options);
                    if (item == null) {
                        throw new FaultException(Fault.Common.COLLECTION_ITEM_NULL, name);
                    }
                    collection.add(item);
                }
                return collection;
            }
        }
        // Convert map to specific map
        else if (value instanceof Map && Map.class.isAssignableFrom(targetType)) {
            Map map = null;
            try {
                map = (Map) targetType.newInstance();

            }
            catch (Exception exception) {
                throw new RuntimeException(new FaultException(Fault.Common.CLASS_CANNOT_BE_INSTANCED,
                        targetType));
            }
            map.putAll((Map) value);
            return map;
        }
        // If map is given convert to object
        else if (value instanceof Map) {
            return convertMapToObject((Map) value, targetType, options);
        }
        throw new IllegalArgumentException(String.format("Cannot convert value of type '%s' to '%s'.",
                value.getClass().getCanonicalName(), targetType.getCanonicalName()));
    }

    /**
     * Convert given value to the specified type.
     *
     * @param value
     * @param targetType
     * @return converted value to specified class
     * @throws IllegalArgumentException when the value cannot be converted to specified type
     * @throws FaultException           when the conversion fails
     */
    public static <T> T convert(Object value, Class<T> targetType) throws FaultException
    {
        return (T) convert(value, targetType, null, null, Options.SERVER);
    }

    /**
     * Convert enum value to another enum value.
     *
     * @param value     "Enum from" value
     * @param enumClass "Enum to" class
     * @return converted enum to value
     * @throws FaultException when the "enum to" cannot have given "enum from" value
     */
    public static <T extends java.lang.Enum<T>, F extends java.lang.Enum<F>> T convert(F value, Class<T> enumClass)
            throws FaultException
    {
        return convertStringToEnum(value.toString(), enumClass);
    }

    /**
     * Convert string to enum type.
     *
     * @param value
     * @param enumClass
     * @return enum value for given string from specified enum class
     * @throws FaultException
     */
    public static <T extends java.lang.Enum<T>> T convertStringToEnum(String value, Class<T> enumClass)
            throws FaultException
    {
        try {
            return Enum.valueOf(enumClass, value);
        }
        catch (IllegalArgumentException exception) {
            throw new FaultException(Fault.Common.ENUM_VALUE_NOT_DEFINED, value,
                    getClassShortName(enumClass));
        }
    }

    /**
     * @param value
     * @return parsed date/time from string
     * @throws FaultException when parsing fails
     */
    public static DateTime convertStringToDateTime(String value) throws FaultException
    {
        try {
            DateTime dateTime = DateTime.parse(value);
            return dateTime;
        }
        catch (Exception exception) {
            throw new FaultException(Fault.Common.DATETIME_PARSING_FAILED, value);
        }
    }

    /**
     * @param value
     * @return parsed period from string
     * @throws FaultException when parsing fails
     */
    public static Period convertStringToPeriod(String value) throws FaultException
    {
        try {
            Period period = Period.parse(value);
            return period;
        }
        catch (Exception exception) {
            throw new FaultException(Fault.Common.PERIOD_PARSING_FAILED, value);
        }
    }

    /**
     * @param value
     * @return parsed interval from string
     * @throws FaultException when parsing fails
     */
    public static Interval convertStringToInterval(String value) throws FaultException
    {
        String[] parts = value.split("/");
        if (parts.length == 2) {
            Interval interval = new Interval(convertStringToDateTime(parts[0]), convertStringToPeriod(parts[1]));
            return interval;
        }
        throw new FaultException(Fault.Common.INTERVAL_PARSING_FAILED, value);
    }

    /**
     * @param map
     * @param options
     * @return new instance of object that is filled by attributes from given map (must contain 'class attribute')
     * @throws FaultException
     */
    public static Object convertMapToObject(Map map, Options options) throws FaultException
    {
        // Get object class
        String className = (String) map.get("class");
        if (className == null) {
            throw new FaultException(Fault.Common.UNKNOWN_FAULT, "Map must contains 'class' attribute!");
        }
        Class objectClass = null;
        try {
            objectClass = getClassFromShortName(className);
        }
        catch (ClassNotFoundException exception) {
            throw new FaultException(Fault.Common.CLASS_NOT_DEFINED, className);
        }
        return convertMapToObject(map, objectClass, options);
    }

    /**
     * @param map
     * @param objectClass
     * @param options
     * @return new instance of given object class that is filled by attributes from given map
     * @throws FaultException
     */
    public static <T> T convertMapToObject(Map map, Class<T> objectClass, Options options) throws FaultException
    {
        // Null or empty map means "null" object
        if (map == null || map.size() == 0) {
            return null;
        }
        else {
            // Check proper class
            if (map.containsKey("class")) {
                String className = (String) map.get("class");
                if (!className.equals(getClassShortName(objectClass))) {
                    throw new FaultException(Fault.Common.UNKNOWN_FAULT,
                            "Cannot convert map to object of class '%s' because map specifies different class '%s'.",
                            getClassShortName(objectClass), className);
                }
            }

            // Create new instance of object
            Object object = null;
            try {
                object = objectClass.newInstance();
            }
            catch (Exception exception) {
                throw new FaultException(Fault.Common.CLASS_CANNOT_BE_INSTANCED, objectClass);
            }

            ChangesTrackingObject changesTrackingObject =
                    ((object instanceof ChangesTrackingObject) ? (ChangesTrackingObject) object : null);

            // Clear all filled properties
            if (changesTrackingObject != null) {
                changesTrackingObject.clearMarks();
            }

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
                Property property = Property.getPropertyNotNull(object.getClass(), propertyName);
                Class type = property.getType();
                Class[] allowedTypes = property.getAllowedTypes();

                // Parse collection changes
                if (value instanceof Map && property.isArrayOrCollection()) {
                    Map collectionChanges = (Map) value;
                    Object newItems = null;
                    Object modifiedItems = null;
                    Object deletedItems = null;
                    if (collectionChanges.containsKey(ChangesTrackingObject.COLLECTION_NEW)) {
                        newItems = Converter.convert(collectionChanges.get(ChangesTrackingObject.COLLECTION_NEW),
                                type, allowedTypes, propertyName, options);
                    }
                    if (collectionChanges.containsKey(ChangesTrackingObject.COLLECTION_MODIFIED)) {
                        modifiedItems = Converter
                                .convert(collectionChanges.get(ChangesTrackingObject.COLLECTION_MODIFIED),
                                        type, allowedTypes, propertyName, options);
                    }
                    if (collectionChanges.containsKey(ChangesTrackingObject.COLLECTION_DELETED)) {
                        deletedItems = Converter
                                .convert(collectionChanges.get(ChangesTrackingObject.COLLECTION_DELETED),
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
                                    if (changesTrackingObject != null) {
                                        changesTrackingObject.markCollectionItemAsNew(propertyName, newItem);
                                    }
                                }
                            }
                            if (modifiedItems != null) {
                                for (Object modifiedItem : (Object[]) modifiedItems) {
                                    array[index++] = modifiedItem;
                                }
                            }
                            if (deletedItems != null) {
                                for (Object deletedItem : (Object[]) deletedItems) {
                                    if (changesTrackingObject != null) {
                                        changesTrackingObject.markCollectionItemAsDeleted(propertyName, deletedItem);
                                    }
                                }
                            }
                            value = array;
                        }
                        else if (property.isCollection()) {
                            Collection<Object> collection = Converter.createCollection(type, 0);
                            if (newItems != null) {
                                for (Object newItem : (Collection) newItems) {
                                    collection.add(newItem);
                                    if (changesTrackingObject != null) {
                                        changesTrackingObject.markCollectionItemAsNew(propertyName, newItem);
                                    }
                                }
                            }
                            if (modifiedItems != null) {
                                for (Object modifiedItem : (Collection) modifiedItems) {
                                    collection.add(modifiedItem);
                                }
                            }
                            if (deletedItems != null) {
                                for (Object deletedItem : (Collection) deletedItems) {
                                    if (changesTrackingObject != null) {
                                        changesTrackingObject.markCollectionItemAsDeleted(propertyName, deletedItem);
                                    }
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
                            object.getClass(), requiredType, givenType);
                }

                // Set the value to property
                property.setValue(object, value, options.isForceAccessible());

                // Mark property as filled
                if (changesTrackingObject != null) {
                    changesTrackingObject.markPropertyAsFilled(propertyName);
                }
            }

            return (T) object;
        }
    }

    /**
     * Set of primitive type classes.
     */
    private static Set<Class> primitiveClassess = new HashSet<Class>()
    {{
            add(Boolean.class);
            add(Character.class);
            add(Byte.class);
            add(Short.class);
            add(Integer.class);
            add(Long.class);
            add(Float.class);
            add(Double.class);
            add(Void.class);
        }};

    /**
     * Convert given object if possible to {@link Map} or {@link Object[]} (recursive).
     *
     * @param object
     * @param options
     * @return {@link Map} or {@link Object[]} or given value
     */
    public static Object convertToMapOrArray(Object object, Options options) throws FaultException
    {
        if (object == null || object instanceof Map) {
            return object;
        }
        else if (object instanceof Collection) {

            Collection collection = (Collection) object;
            Object[] newArray = new Object[collection.size()];
            int index = 0;
            for (Object item : collection) {
                newArray[index] = convertToMapOrArray(item, options);
                index++;
            }
            return newArray;
        }
        else if (object instanceof Object[]) {
            Object[] oldArray = (Object[]) object;
            Object[] newArray = new Object[oldArray.length];
            for (int index = 0; index < oldArray.length; index++) {
                newArray[index] = convertToMapOrArray(oldArray[index], options);
            }
            return newArray;
        }
        else if (isAtomic(object) || primitiveClassess.contains(object.getClass())) {
            return object;
        }
        return convertObjectToMap(object, options);
    }

    /**
     * @param object
     * @param options
     * @return map containing attributes of given object
     * @throws FaultException
     */

    /**
     * Convert object to a map. Map will contain all object's properties that are:
     * 1) simple (not {@link Object[]} or {@link Collection}) with not {@link null} value
     * 2) simple with {@link null} value but marked as filled through
     * ({@link ChangesTrackingObject#markPropertyAsFilled(String)}
     * 3) {@link Object[]} or {@link Collection} which is not empty
     *
     * @param object
     * @param options see {@link Options}
     * @return map which contains object's properties
     * @throws FaultException when the conversion fails
     */
    public static Map convertObjectToMap(Object object, Options options) throws FaultException
    {
        ChangesTrackingObject changesTrackingObject =
                ((object instanceof ChangesTrackingObject) ? (ChangesTrackingObject) object : null);

        Map<String, Object> map = new HashMap<String, Object>();
        String[] propertyNames = Property.getPropertyNames(object.getClass());
        for (String propertyName : propertyNames) {
            Property property = Property.getProperty(object.getClass(), propertyName);
            if (property == null) {
                throw new FaultException("Cannot get property '%s' from class '%s'.", propertyName, object.getClass());
            }
            Object value = property.getValue(object);
            if (value == null) {
                if (changesTrackingObject == null || !options.isStoreChanges()
                        || !changesTrackingObject.isPropertyFilled(propertyName)) {
                    continue;
                }
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
                ChangesTrackingObject.CollectionChanges collectionChanges = changesTrackingObject != null ? changesTrackingObject
                        .getCollectionChanges(propertyName) : null;
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
                        mapCollection.put(ChangesTrackingObject.COLLECTION_NEW,
                                Converter.convertToMapOrArray(collectionChanges.newItems, options));
                    }
                    if (collectionChanges.deletedItems.size() > 0) {
                        mapCollection.put(ChangesTrackingObject.COLLECTION_DELETED,
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
                    mapCollection.put(ChangesTrackingObject.COLLECTION_MODIFIED,
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
        map.put("class", getClassShortName(object.getClass()));
        return map;
    }

    /**
     * @param object Instance of atomic type
     * @return converted atomic type to string
     */
    public static String convertAtomicToString(Object object)
    {
        if (object instanceof Interval) {
            Interval interval = (Interval) object;
            return String.format("%s/%s", interval.getStart().toString(), interval.toPeriod().toString());
        }
        return object.toString();
    }

    /**
     * @param object
     * @return true if object is of atomic type (e.g., {@link String}, {@link AtomicType}, {@link Enum},
     *         {@link Period} or {@link DateTime}),
     *         false otherwise
     */
    public static boolean isAtomic(Object object)
    {
        if (object instanceof String || object instanceof Enum) {
            return true;
        }
        if (object instanceof AtomicType) {
            return true;
        }
        if (object instanceof Period || object instanceof DateTime || object instanceof Interval) {
            return true;
        }
        return false;
    }

    /**
     * @param componentType
     * @param size
     * @return new instance of array of given size
     */
    public static Object[] createArray(Class componentType, int size)
    {
        return (Object[]) Array.newInstance(componentType, size);
    }

    /**
     * @param type
     * @param size
     * @return new instance of collection of given size
     * @throws FaultException
     */
    public static Collection<Object> createCollection(Class type, int size) throws FaultException
    {
        if (List.class.isAssignableFrom(type)) {
            return new ArrayList<Object>(size);
        }
        else if (Set.class.isAssignableFrom(type)) {
            return new HashSet<Object>(size);
        }
        else if (Collection.class.equals(type)) {
            return new ArrayList<Object>(size);
        }
        throw new FaultException(Fault.Common.CLASS_CANNOT_BE_INSTANCED, type);
    }
}
