package cz.cesnet.shongo.controller.api.util;

import cz.cesnet.shongo.controller.api.AtomicType;
import cz.cesnet.shongo.controller.api.ComplexType;
import cz.cesnet.shongo.controller.api.Fault;
import cz.cesnet.shongo.controller.api.FaultException;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.lang.reflect.Array;
import java.util.*;

import static cz.cesnet.shongo.controller.api.util.ClassHelper.getClassFromShortName;
import static cz.cesnet.shongo.controller.api.util.ClassHelper.getClassShortName;

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
     * @return converted value
     * @throws FaultException
     * @throws IllegalArgumentException
     */
    public static Object convert(Object value, Class targetType, Class[] allowedTypes)
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
                            allowedValue = Converter.convert(value, allowedType, null);
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
        }
        // Convert atomic types
        else if (value instanceof String) {
            // If enum is required
            if (targetType.isEnum() && value instanceof String) {
                value = Converter.convertStringToEnum((String) value, (Class<Enum>) targetType);
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
                value = atomicType;
            }
            // If period is required
            else if (Period.class.isAssignableFrom(targetType)) {
                value = convertStringToPeriod((String) value);
            }
            // If date/time is required
            else if (DateTime.class.isAssignableFrom(targetType)) {
                value = convertStringToDateTime((String) value);
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
                    newArray[index] = convert(arrayValue[index], componentType, allowedTypes);
                }
                value = newArray;
            }
            else if (Collection.class.isAssignableFrom(targetType)) {
                // Convert collection to specific type
                Object[] arrayValue = (Object[]) value;
                Collection<Object> collection = createCollection(targetType, arrayValue.length);
                for (Object item : arrayValue) {
                    collection.add(convert(item, Object.class, allowedTypes));
                }
                value = collection;
            }
        }
        // If complex type is required and map is given
        else if (ComplexType.class.isAssignableFrom(targetType) && value instanceof Map) {
            value = convertMapToObject((Map) value, targetType);
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
            value = map;
        }
        else {
            throw new IllegalArgumentException(String.format("Cannot convert value of type '%s' to '%s'.",
                    value.getClass().getCanonicalName(), targetType.getCanonicalName()));
        }
        return value;
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
        return (T) convert(value, targetType, null);
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
            return DateTime.parse(value);
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
            return Period.parse(value);
        }
        catch (Exception exception) {
            throw new FaultException(Fault.Common.PERIOD_PARSING_FAILED, value);
        }
    }

    /**
     * @param map
     * @return new instance of object that is filled by attributes from given map (must contain 'class attribute')
     * @throws FaultException
     */
    public static Object convertMapToObject(Map map) throws FaultException
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
        return convertMapToObject(map, objectClass);
    }

    /**
     * @param map
     * @param objectClass
     * @return new instance of given object class that is filled by attributes from given map
     * @throws FaultException
     */
    public static <T> T convertMapToObject(Map map, Class<T> objectClass) throws FaultException
    {
        if (!ComplexType.class.isAssignableFrom(objectClass)) {
            throw new FaultException(Fault.Common.UNKNOWN_FAULT,
                    String.format("Cannot convert map to '%s' because the target type doesn't support the conversion!",
                            getClassShortName(objectClass)));
        }
        // Null or empty map means "null" object
        if (map == null || map.size() == 0) {
            return null;
        }
        else {
            // Check proper class
            if (map.containsKey("class")) {
                String className = (String) map.get("class");
                if (!className.equals(getClassShortName(objectClass))) {
                    throw new FaultException(Fault.Common.UNKNOWN_FAULT, String.format(
                            "Cannot convert map to object of class '%s' because map specifies different class '%s'."),
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

            ComplexType complexType = ComplexType.class.cast(object);
            complexType.fromMap(map);

            return (T) object;
        }
    }

    /**
     * Convert given object if possible to {@link Map} or {@link Object[]} (recursive).
     *
     * @param storeChanges
     * @return {@link Map} or {@link Object[]} or given value
     */
    public static Object convertToMapOrArray(Object object, boolean storeChanges) throws FaultException
    {
        if (object instanceof ComplexType) {
            ComplexType complexType = ComplexType.class.cast(object);
            return complexType.toMap(storeChanges);
        }
        else if (object instanceof Collection) {

            Collection collection = (Collection) object;
            Object[] newArray = new Object[collection.size()];
            int index = 0;
            for (Object item : collection) {
                newArray[index] = convertToMapOrArray(item, storeChanges);
                index++;
            }
            return newArray;
        }
        else if (object instanceof Object[]) {
            Object[] oldArray = (Object[]) object;
            Object[] newArray = new Object[oldArray.length];
            for (int index = 0; index < oldArray.length; index++) {
                newArray[index] = convertToMapOrArray(oldArray[index], storeChanges);
            }
            return newArray;
        }
        return object;
    }

    /**
     * @param object
     * @param storeChanges
     * @return map containing attributes of given object
     * @throws FaultException
     */
    public static Map convertObjectToMap(Object object, boolean storeChanges) throws FaultException
    {
        if (!(object instanceof ComplexType)) {
            throw new FaultException(Fault.Common.UNKNOWN_FAULT,
                    String.format("Cannot convert '%s' to map because the object doesn't support the conversion!",
                            getClassShortName(object.getClass())));
        }
        ComplexType complexType = ComplexType.class.cast(object);
        return complexType.toMap(storeChanges);
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
        if (object instanceof Period || object instanceof DateTime) {
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
