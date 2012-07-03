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
        if (allowedTypes != null && allowedTypes.length > 0) {
            if (allowedTypes.length == 1) {
                if (!(value instanceof Object[] || value instanceof Collection)) {
                    targetType = allowedTypes[0];
                }
            }
            else {
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
                Object[] newArray = (Object[]) Array.newInstance(componentType, arrayValue.length);
                for (int index = 0; index < arrayValue.length; index++) {
                    newArray[index] = convert(arrayValue[index], componentType, allowedTypes);
                }
                value = newArray;
            }
            else if (Collection.class.isAssignableFrom(targetType)) {
                // Convert array to specific type
                Object[] arrayValue = (Object[]) value;
                List<Object> newList = new ArrayList<Object>(arrayValue.length);
                for (Object item : arrayValue) {
                    newList.add(convert(item, Object.class, allowedTypes));
                }
                value = newList;
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
            fromMap(complexType, map);

            return (T) object;
        }
    }

    /**
     * @param object
     * @return map containing attributes of given object
     * @throws FaultException
     */
    public static Map convertObjectToMap(Object object) throws FaultException
    {
        if (!(object instanceof ComplexType)) {
            throw new FaultException(Fault.Common.UNKNOWN_FAULT,
                    String.format("Cannot convert '%s' to map because the object doesn't support the conversion!",
                            getClassShortName(object.getClass())));
        }
        ComplexType complexType = ComplexType.class.cast(object);
        return toMap(complexType);
    }

    /**
     * Fill given {@link ComplexType} from the given map.
     *
     * @param complexType
     * @param map
     * @throws FaultException
     */
    public static void fromMap(ComplexType complexType, Map map) throws FaultException
    {
        // Clear all filled properties
        complexType.clearPropertyFilledMarks();

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
            Property propertyDefinition = Property.getPropertyNotNull(complexType.getClass(), property);
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
                        complexType.getClass(),
                        type,
                        value.getClass());
            }

            // Set the value to property
            Property.setPropertyValue(complexType, property, value);

            // Mark property as filled
            complexType.markPropertyFilled(property);
        }
    }

    /**
     * Convert given {@link ComplexType} to a map.
     *
     * @param complexType
     * @return map
     * @throws FaultException
     */
    public static Map toMap(ComplexType complexType) throws FaultException
    {
        Map<String, Object> map = new HashMap<String, Object>();
        String[] propertyNames = Property.getPropertyNames(complexType.getClass());
        for (String property : propertyNames) {
            Object value = Property.getPropertyValue(complexType, property);
            if (value == null) {
                continue;
            }
            else if (value instanceof ComplexType) {
                value = convertObjectToMap((ComplexType) value);
            }
            else if (value instanceof Object[]) {
                Object[] oldArray = (Object[]) value;
                Object[] newArray = new Object[oldArray.length];
                for (int index = 0; index < oldArray.length; index++) {
                    Object item = oldArray[index];
                    if (item instanceof ComplexType) {
                        item = convertObjectToMap((ComplexType) item);
                    }
                    newArray[index] = item;
                }
                value = newArray;
            }
            map.put(property, value);
        }
        map.put("class", getClassShortName(complexType.getClass()));
        return map;
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
}
