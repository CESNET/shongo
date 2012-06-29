package cz.cesnet.shongo.common.util;

import cz.cesnet.shongo.common.api.AtomicType;
import cz.cesnet.shongo.common.api.ComplexType;
import cz.cesnet.shongo.common.api.Fault;
import cz.cesnet.shongo.common.api.FaultException;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Helper class for converting types.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Converter
{
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
                catch (java.lang.Exception exception) {
                    throw new RuntimeException(new FaultException(Fault.Common.CLASS_CANNOT_BE_INSTANCED,
                            Converter.getClassShortName(targetType)));
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
            ComplexType type = null;
            try {
                type = ComplexType.class.cast(targetType.newInstance());
            }
            catch (Exception exception) {
                exception.printStackTrace();
                throw new FaultException(Fault.Common.CLASS_CANNOT_BE_INSTANCED, getClassShortName(targetType));
            }
            type.fromMap((Map) value);
            value = type;
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
    public static Object convert(Object value, Class targetType) throws FaultException
    {
        return convert(value, targetType, null);
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
            objectClass = Converter.getClassFromShortName(className);
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
    public static Object convertMapToObject(Map map, Class objectClass) throws FaultException
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
                throw new FaultException(Fault.Common.CLASS_CANNOT_BE_INSTANCED,
                        objectClass.getCanonicalName());
            }

            ComplexType complexType = ComplexType.class.cast(object);
            complexType.fromMap(map);

            return object;
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
        return complexType.toMap();
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
     * Get short name from class.
     *
     * @param clazz
     * @return class short name
     */
    public static String getClassShortName(Class clazz)
    {
        if (clazz.getEnclosingClass() != null) {
            return clazz.getEnclosingClass().getSimpleName() + "." + clazz.getSimpleName();
        }
        else {
            return clazz.getSimpleName();
        }
    }

    /**
     * List of all API packages in classpath (initialized only once).
     */
    static String[] packages;

    /**
     * Get full class name from short class name
     *
     * @param shortClassName
     * @return full class name
     */
    public static Class getClassFromShortName(String shortClassName) throws ClassNotFoundException
    {
        if (packages == null) {
            ArrayList<String> list = new ArrayList<String>();
            for (Package item : Package.getPackages()) {
                String name = item.getName();
                if (name.startsWith("cz.cesnet.shongo.") && name.endsWith(".api")) {
                    list.add(name);
                }
            }
            packages = list.toArray(new String[list.size()]);
        }
        shortClassName = shortClassName.replace(".", "$");
        for (String item : packages) {
            try {
                Class clazz = Class.forName(item + "." + shortClassName);
                return clazz;
            }
            catch (ClassNotFoundException exception) {
            }
        }
        return Class.forName("cz.cesnet.shongo." + shortClassName);
    }
}
