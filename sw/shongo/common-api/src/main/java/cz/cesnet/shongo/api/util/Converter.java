package cz.cesnet.shongo.api.util;

import cz.cesnet.shongo.api.AtomicType;
import cz.cesnet.shongo.fault.CommonFault;
import cz.cesnet.shongo.fault.FaultException;
import org.joda.time.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cz.cesnet.shongo.api.util.ClassHelper.getClassFromShortName;
import static cz.cesnet.shongo.api.util.ClassHelper.getClassShortName;

/**
 * Helper class for converting types.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Converter
{
    private static Logger logger = LoggerFactory.getLogger(Converter.class);

    /**
     * Convert given {@code value} to a {@code valueType}.
     *
     * @param value              which should be converted
     * @param targetType         to which the given {@code value} should be converted
     * @param targetAllowedTypes restricts types to which the value can be converted
     * @param property           specifies property for whose value the conversion is done
     * @return converted value
     * @throws IllegalArgumentException when the value cannot be converted to specified type
     * @throws FaultException           when the conversion fails from some reason
     */
    public static Object convert(Object value, Class targetType, Class[] targetAllowedTypes, Options options,
            Property property)
            throws IllegalArgumentException, FaultException
    {
        // Null values aren't converted
        if (value == null) {
            return null;
        }

        // Get type flags
        int valueTypeFlags = Property.TypeFlags.getTypeFlags(value.getClass());
        int targetTypeFlags = Property.TypeFlags.getTypeFlags(targetType);

        // Iterate through allowed types for value and try the conversion for each one (this operation should not be
        // performed for types containing items, it will be performed for each item in nested call)
        if (targetAllowedTypes != null && targetAllowedTypes.length > 0
                && !Property.TypeFlags.isArrayOrCollectionOrMap(targetTypeFlags)) {
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
                        allowedValue = Converter.convert(value, allowedType, null, options, null);
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
                    Map map = (Map) value;
                    Map newMap = new HashMap<Object, Object>();
                    for ( Object itemKey : map.keySet()) {
                        Object itemValue = map.get(itemKey);
                        itemKey = convert(itemKey, property.getKeyAllowedType(), null, options, null);;
                        itemValue = convert(itemValue, Object.class, targetAllowedTypes, options, null);;
                        newMap.put(itemKey, itemValue);
                    }
                    return newMap;
                }
            }
            // Do nothing
            return value;
        }
        // Convert from basic types
        else if (Property.TypeFlags.isBasic(valueTypeFlags)) {
            if (targetType.isPrimitive()) {
                return value;
            }
            else if (targetType.equals(String.class)) {
                return value.toString();
            }
            else if (targetType.equals(Boolean.class) && value instanceof Integer) {
                return ((Integer) value).intValue() != 0;
            }
        }
        // Convert from date
        else if (value instanceof Date && DateTime.class.isAssignableFrom(targetType)) {
            return new DateTime(value);
        }
        // Convert atomic types
        else if (value instanceof String) {
            // If Class is required
            if (targetType.equals(Class.class)) {
                try {
                    return ClassHelper.getClassFromShortName((String) value);
                }
                catch (ClassNotFoundException exception) {
                    throw new FaultException(CommonFault.CLASS_NOT_DEFINED, value);
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
                return Converter.convertStringToEnum((String) value, (Class<Enum>) targetType);
            }
            // If atomic type is required
            else if (AtomicType.class.isAssignableFrom(targetType) && value instanceof String) {
                AtomicType atomicType = null;
                try {
                    atomicType = (AtomicType) targetType.newInstance();
                }
                catch (Exception exception) {
                    throw new RuntimeException(new FaultException(CommonFault.CLASS_CANNOT_BE_INSTANCED,
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
            else if (ReadablePartial.class.isAssignableFrom(targetType)) {
                return convertStringToReadablePartial((String) value);
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
                    Object item = convert(arrayValue[index], componentType, targetAllowedTypes, options, null);
                    if (item == null) {
                        throw new FaultException(CommonFault.COLLECTION_ITEM_NULL, property.getName());
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
                    item = convert(item, Object.class, targetAllowedTypes, options, null);
                    if (item == null) {
                        throw new FaultException(CommonFault.COLLECTION_ITEM_NULL, property.getName());
                    }
                    collection.add(item);
                }
                return collection;
            }
        }
        // If map is given convert to object
        else if (value instanceof Map) {
            return convertMapToObject((Map) value, targetType, options);
        }
        throw new IllegalArgumentException(String.format("Cannot convert value of type '%s' to '%s'.",
                value.getClass().getCanonicalName(), targetType.getCanonicalName()));
    }

    /**
     * Convert given {@code value} to a type specified by a given {@code property}.
     *
     * @see #convert(Object, Class, Class[], Options, Property)
     */
    @SuppressWarnings("unchecked")
    public static <T> T convert(Object value, Property property, Options options)
            throws IllegalArgumentException, FaultException
    {
        return (T) convert(value, property.getType(), property.getValueAllowedTypes(), options, property);
    }

    /**
     * Convert given {@code value} to a type specified by a given {@code property}.
     *
     * @see #convert(Object, Class, Class[], Options, Property)
     */
    @SuppressWarnings("unchecked")
    public static <T> T convert(Object value, Property property) throws IllegalArgumentException, FaultException
    {
        return (T) convert(value, property.getType(), property.getValueAllowedTypes(), Options.SERVER, property);
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
            throw new FaultException(CommonFault.ENUM_VALUE_NOT_DEFINED, value,
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
            throw new FaultException(CommonFault.DATETIME_PARSING_FAILED, value);
        }
    }

    /**
     * Array of supported partial fields (in the same order as in the partial regex pattern).
     */
    private static DateTimeFieldType[] PARTIAL_FIELDS = new DateTimeFieldType[]{
            DateTimeFieldType.year(),
            DateTimeFieldType.monthOfYear(),
            DateTimeFieldType.dayOfMonth(),
            DateTimeFieldType.hourOfDay(),
            DateTimeFieldType.minuteOfHour()
    };

    /**
     * @param value
     * @return parsed partial date/time from string
     * @throws FaultException when parsing fails
     */
    public static ReadablePartial convertStringToReadablePartial(String value) throws FaultException
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
        throw new FaultException(CommonFault.PARTIAL_DATETIME_PARSING_FAILED, value);
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
            throw new FaultException(CommonFault.PERIOD_PARSING_FAILED, value);
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
        throw new FaultException(CommonFault.INTERVAL_PARSING_FAILED, value);
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
            throw new FaultException("Map must contains 'class' attribute!");
        }
        Class objectClass = null;
        try {
            objectClass = getClassFromShortName(className);
        }
        catch (ClassNotFoundException exception) {
            throw new FaultException(CommonFault.CLASS_NOT_DEFINED, className);
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
                    throw new FaultException(CommonFault.UNKNOWN,
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
                throw new FaultException(CommonFault.CLASS_CANNOT_BE_INSTANCED, objectClass);
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
                    throw new FaultException("Map must contain only string keys.");
                }
                String propertyName = (String) key;
                Object value = map.get(key);

                // Skip class property
                if (propertyName.equals("class")) {
                    continue;
                }

                Property property = Property.getPropertyNotNull(object.getClass(), propertyName);
                if (property.isReadOnly() && !options.isLoadReadOnly()) {
                    throw new FaultException(CommonFault.CLASS_ATTRIBUTE_READ_ONLY, propertyName, object.getClass());
                }

                // Get property type and type flags
                Class propertyType = property.getType();
                int propertyTypeFlags = property.getTypeFlags();

                if (value instanceof Map) {
                    Map mapValue = (Map) value;
                    if (Property.TypeFlags.isArray(propertyTypeFlags)) {
                    }
                    else if (Property.TypeFlags.isCollection(propertyTypeFlags)) {
                    }
                    else if (Property.TypeFlags.isMap(propertyTypeFlags)) {
                    }
                }


                // Parse collection changes
                if (value instanceof Map && Property.TypeFlags.isArrayOrCollectionOrMap(propertyTypeFlags)) {
                    Map collectionChanges = (Map) value;
                    Object newItems = null;
                    Object modifiedItems = null;
                    Object deletedItems = null;
                    if (collectionChanges.containsKey(ChangesTrackingObject.COLLECTION_NEW)) {
                        newItems = Converter.convert(
                                collectionChanges.get(ChangesTrackingObject.COLLECTION_NEW), property, options);
                    }
                    if (collectionChanges.containsKey(ChangesTrackingObject.COLLECTION_MODIFIED)) {
                        modifiedItems = Converter.convert(
                                collectionChanges.get(ChangesTrackingObject.COLLECTION_MODIFIED), property, options);
                    }
                    if (collectionChanges.containsKey(ChangesTrackingObject.COLLECTION_DELETED)) {
                        deletedItems = Converter.convert(
                                collectionChanges.get(ChangesTrackingObject.COLLECTION_DELETED), property, options);
                    }
                    if (newItems != null || modifiedItems != null || deletedItems != null) {
                        if (Property.TypeFlags.isArray(propertyTypeFlags)) {
                            int size = (newItems != null ? ((Object[]) newItems).length : 0)
                                    + (modifiedItems != null ? ((Object[]) modifiedItems).length : 0);
                            int index = 0;
                            Object[] array = Converter.createArray(propertyType.getComponentType(), size);
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
                        else if (Property.TypeFlags.isCollection(propertyTypeFlags)) {
                            Collection<Object> collection = Converter.createCollection(propertyType, 0);
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
                    value = Converter.convert(value, property, options);
                }
                catch (IllegalArgumentException exception) {
                    Object requiredType = propertyType;
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
                    throw new FaultException(CommonFault.CLASS_ATTRIBUTE_TYPE_MISMATCH, propertyName,
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
        else if (isAtomic(object)) {
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
        String[] propertyNames = Property.getPropertyNames(object.getClass(), ChangesTrackingObject.class);
        for (String propertyName : propertyNames) {
            Property property = Property.getProperty(object.getClass(), propertyName);
            if (property == null) {
                throw new FaultException("Cannot get property '%s' from class '%s'.", propertyName, object.getClass());
            }

            Object value = property.getValue(object);
            if (property.isEmptyValue(value)) {
                if (changesTrackingObject == null || !options.isStoreChanges()
                        || !changesTrackingObject.isPropertyFilled(propertyName)) {
                    continue;
                }
            }

            if (property.isReadOnly() && !options.isStoreReadOnly()) {
                continue;
            }

            // Store changes for items
            if (options.isStoreChanges() && Property.TypeFlags.isArrayOrCollectionOrMap(property.getTypeFlags())) {
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

                // Map of changes
                Map<String, Object> mapItemChanges = new HashMap<String, Object>();
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
                        mapItemChanges.put(ChangesTrackingObject.COLLECTION_NEW,
                                Converter.convertToMapOrArray(collectionChanges.newItems, options));
                    }
                    if (collectionChanges.deletedItems.size() > 0) {
                        mapItemChanges.put(ChangesTrackingObject.COLLECTION_DELETED,
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
                    mapItemChanges.put(ChangesTrackingObject.COLLECTION_MODIFIED,
                            Converter.convertToMapOrArray(modifiedItems, options));
                }
                // Skip empty changes
                if (mapItemChanges.isEmpty()) {
                    continue;
                }
                // Append values for Map
                if (value instanceof Map) {
                    mapItemChanges.put("__map", value);
                }
                value = mapItemChanges;
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
     * @param value of {@link Property.TypeFlags#ATOMIC} type
     * @return converted {@link Property.TypeFlags#BASIC}
     */
    public static Object convertAtomicToBasic(Object value)
    {
        int valueTypeFlags = Property.TypeFlags.getTypeFlags(value.getClass());
        if (Property.TypeFlags.isBasic(valueTypeFlags)) {
            return value;
        }
        if (value instanceof Interval) {
            Interval interval = (Interval) value;
            return convertIntervalToString(interval);
        }
        return value.toString();
    }

    /**
     * @param interval
     * @return converted interval to string
     */
    public static String convertIntervalToString(Interval interval)
    {
        return String.format("%s/%s", interval.getStart().toString(), interval.toPeriod().toString());
    }

    /**
     * @param value to be checked
     * @return true if given {@code value} is {@link Property.TypeFlags#PRIMITIVE}
     *         false otherwise
     */
    public static boolean isPrimitive(Object value)
    {
        int valueTypeFlags = Property.TypeFlags.getTypeFlags(value.getClass());
        return Property.TypeFlags.isPrimitive(valueTypeFlags);
    }

    /**
     * @param value to be checked
     * @return true if given {@code value} is {@link Property.TypeFlags#ATOMIC},
     *         false otherwise
     */
    public static boolean isAtomic(Object value)
    {
        int valueTypeFlags = Property.TypeFlags.getTypeFlags(value.getClass());
        return Property.TypeFlags.isAtomic(valueTypeFlags);
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
        throw new FaultException(CommonFault.CLASS_CANNOT_BE_INSTANCED, type);
    }
}
