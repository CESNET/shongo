package cz.cesnet.shongo.api.util;

import cz.cesnet.shongo.api.AtomicType;
import cz.cesnet.shongo.fault.CommonFault;
import cz.cesnet.shongo.fault.FaultException;
import org.joda.time.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cz.cesnet.shongo.api.util.ClassHelper.getClassFromShortName;
import static cz.cesnet.shongo.api.util.ClassHelper.getClassShortName;

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
    public static Object convert(Object value, Property property) throws IllegalArgumentException, FaultException
    {
        return convert(value, property.getType(), property.getValueAllowedTypes(), property, DEFAULT_OPTIONS);
    }

    /**
     * Convert given {@link TypeFlags#BASIC} {@code value} to:
     * 1) {@link Object} if given {@code value} is {@link Map} and has defined "class"
     * or return the same value.
     *
     * @param value   to be converted
     * @param options {@link Options}
     * @return converted given {@code value}
     * @throws FaultException if the conversion failed
     */
    public static Object convertFromBasic(Object value, Options options) throws FaultException
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
     * @throws FaultException if the conversion failed
     */
    public static Object convertFromBasic(Object value, Class targetType, Options options) throws FaultException
    {
        Class valueType = value.getClass();
        int valueTypeFlags = TypeFlags.get(valueType);
        if (!TypeFlags.isBasic(valueTypeFlags)) {
            throw new IllegalArgumentException(
                    String.format("Type '%s' isn't basic." + value.getClass().getCanonicalName()));
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
    public static Object convertToBasic(Object value, Options options) throws FaultException
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
     * ({@link ChangesTrackingObject#markPropertyAsFilled(String)}
     * 3) {@link Object[]} or {@link Collection} which is not empty
     *
     * @param object  to be converted
     * @param options see {@link Options}
     * @return map which contains object's properties
     * @throws FaultException when the conversion fails
     */
    private static Map convertObjectToMap(Object object, Options options) throws FaultException
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
            if (options.isStoreChanges() && TypeFlags.isArrayOrCollectionOrMap(property.getTypeFlags())) {
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
                                Converter.convertToBasic(collectionChanges.newItems, options));
                    }
                    if (collectionChanges.deletedItems.size() > 0) {
                        mapItemChanges.put(ChangesTrackingObject.COLLECTION_DELETED,
                                Converter.convertToBasic(collectionChanges.deletedItems, options));
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
                            Converter.convertToBasic(modifiedItems, options));
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
            value = Converter.convertToBasic(value, options);

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
     * @throws FaultException when the conversion fails
     */
    private static Object convertMapToObject(Map map, Class targetType, Options options) throws FaultException
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
                    throw new FaultException(CommonFault.CLASS_NOT_DEFINED, className);
                }
                if (!declaredType.isAssignableFrom(targetType)) {
                    throw new FaultException(CommonFault.UNKNOWN, "Cannot convert map to object of class '%s'"
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
                throw new FaultException(CommonFault.CLASS_CANNOT_BE_INSTANCED, targetType);
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
                    if (TypeFlags.isArray(propertyTypeFlags)) {
                    }
                    else if (TypeFlags.isCollection(propertyTypeFlags)) {
                    }
                    else if (TypeFlags.isMap(propertyTypeFlags)) {
                    }
                }

                // Parse collection changes
                if (value instanceof Map && TypeFlags.isArrayOrCollectionOrMap(propertyTypeFlags)) {
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
                        if (TypeFlags.isArray(propertyTypeFlags)) {
                            int size = (newItems != null ? ((Object[]) newItems).length : 0)
                                    + (modifiedItems != null ? ((Object[]) modifiedItems).length : 0);
                            int index = 0;
                            Object[] array = ClassHelper.createArray(propertyType.getComponentType(), size);
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
                        else if (TypeFlags.isCollection(propertyTypeFlags)) {
                            Collection<Object> collection = ClassHelper.createCollection(propertyType, 0);
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

            return object;
        }
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
     * @throws FaultException           when the conversion fails from some reason
     */
    private static Object convert(Object value, Class targetType, Class[] targetAllowedTypes, Property property,
            Options options)
            throws IllegalArgumentException, FaultException
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
                    Map map = (Map) value;
                    Map newMap = new HashMap<Object, Object>();
                    for (Object itemKey : map.keySet()) {
                        Object itemValue = map.get(itemKey);
                        itemKey = convert(itemKey, property.getKeyAllowedType(), null, null, options);
                        ;
                        itemValue = convert(itemValue, Object.class, targetAllowedTypes, null, options);
                        ;
                        newMap.put(itemKey, itemValue);
                    }
                    return newMap;
                }
            }
            // Do nothing
            return value;
        }
        // Convert from basic types
        else if (TypeFlags.isPrimitive(valueTypeFlags)) {
            if (TypeFlags.isPrimitive(targetTypeFlags)) {
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
                return Atomic.convertStringToEnum((String) value, (Class<Enum>) targetType);
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
        // Convert array types
        else if (value instanceof Object[]) {
            if (targetType.isArray()) {
                // Convert array to specific type
                Class componentType = targetType.getComponentType();
                Object[] arrayValue = (Object[]) value;
                Object[] newArray = ClassHelper.createArray(componentType, arrayValue.length);
                for (int index = 0; index < arrayValue.length; index++) {
                    Object item = convert(arrayValue[index], componentType, targetAllowedTypes, null, options);
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
                Collection<Object> collection = ClassHelper.createCollection(targetType, arrayValue.length);
                for (Object item : arrayValue) {
                    item = convert(item, Object.class, targetAllowedTypes, null, options);
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
     * @see #convert(Object, Class, Class[], Property, Options)
     */
    private static Object convert(Object value, Property property, Options options)
            throws IllegalArgumentException, FaultException
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
        private static DateTimeFieldType[] PARTIAL_FIELDS = new DateTimeFieldType[]{
                DateTimeFieldType.year(),
                DateTimeFieldType.monthOfYear(),
                DateTimeFieldType.dayOfMonth(),
                DateTimeFieldType.hourOfDay(),
                DateTimeFieldType.minuteOfHour()
        };

        /**
         * Convert string to enum type.
         *
         * @param value
         * @param enumClass
         * @return enum value for given string from specified enum class
         * @throws cz.cesnet.shongo.fault.FaultException
         *
         */
        public static <T extends Enum<T>> T convertStringToEnum(String value, Class<T> enumClass)
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
         * @throws cz.cesnet.shongo.fault.FaultException
         *          when parsing fails
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
         * @param value
         * @return parsed partial date/time from string
         * @throws cz.cesnet.shongo.fault.FaultException
         *          when parsing fails
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
         * @throws cz.cesnet.shongo.fault.FaultException
         *          when parsing fails
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
         * @throws cz.cesnet.shongo.fault.FaultException
         *          when parsing fails
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
         * @param interval
         * @return converted interval to string
         */
        public static String convertIntervalToString(Interval interval)
        {
            return String.format("%s/%s", interval.getStart().toString(), interval.toPeriod().toString());
        }
    }
}
