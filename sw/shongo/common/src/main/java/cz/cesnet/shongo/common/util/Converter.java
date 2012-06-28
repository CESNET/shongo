package cz.cesnet.shongo.common.util;

import cz.cesnet.shongo.common.api.AtomicType;
import cz.cesnet.shongo.common.api.ComplexType;
import cz.cesnet.shongo.common.api.Fault;
import cz.cesnet.shongo.common.api.FaultException;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Map;

/**
 * Helper class for converting types.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Converter
{

    public static Object convert(Object value, Class targetType) throws IllegalArgumentException, FaultException
    {
        // If types are compatible
        if (targetType.isAssignableFrom(value.getClass())) {
            // Do nothing
        }
        // If enum is required and string is given
        else if (targetType.isEnum() && value instanceof String) {
            // Convert it
            value = Converter.stringToEnum((String) value, (Class<Enum>) targetType);
        }
        // If atomic type is required and string is given
        else if (AtomicType.class.isAssignableFrom(targetType) && value instanceof String) {
            AtomicType atomicType = null;
            try {
                atomicType = (AtomicType) targetType.newInstance();
            }
            catch (java.lang.Exception exception) {
                throw new RuntimeException(new FaultException(Fault.Common.CLASS_CANNOT_BE_INSTANCED,
                        Converter.getShortClassName(targetType)));
            }
            atomicType.fromString((String) value);
            return atomicType;
        }
        // If specific array is required and abstract array is passed
        else if (targetType.isArray() && value instanceof Object[]) {
            // Convert array to specific type
            Class componentType = targetType.getComponentType();
            Object[] arrayValue = (Object[]) value;
            Object[] newArray = (Object[]) Array.newInstance(componentType, arrayValue.length);
            for (int index = 0; index < arrayValue.length; index++) {
                newArray[index] = convert(arrayValue[index], componentType);
            }
            value = newArray;
        }
        // If complex type is required and map is given
        else if (ComplexType.class.isAssignableFrom(targetType) && value instanceof Map) {
            ComplexType type = null;
            try {
                type = ComplexType.class.cast(targetType.newInstance());
            }
            catch (Exception exception) {
                exception.printStackTrace();
                throw new FaultException(Fault.Common.CLASS_CANNOT_BE_INSTANCED, getShortClassName(targetType));
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
     * @param value
     * @param enumClass
     * @return enum value for given string from specified enum class
     * @throws FaultException
     */
    public static <T extends java.lang.Enum<T>> T stringToEnum(String value, Class<T> enumClass)
            throws FaultException
    {
        try {
            return Enum.valueOf(enumClass, value);
        }
        catch (IllegalArgumentException exception) {
            throw new FaultException(Fault.Common.ENUM_VALUE_NOT_DEFINED, value,
                    getShortClassName(enumClass));
        }
    }

    /**
     * @param value
     * @return parsed date/time from string
     * @throws FaultException when parsing fails
     */
    public static DateTime stringToDateTime(String value) throws FaultException
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
    public static Period stringToPeriod(String value) throws FaultException
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
     * @param objectClass
     * @return new instance of given object class that is filled by attributes from given map
     * @throws FaultException
     */
    public static Object mapToObject(Map map, Class objectClass) throws FaultException
    {
        if (!ComplexType.class.isAssignableFrom(objectClass)) {
            throw new FaultException(Fault.Common.UNKNOWN_FAULT,
                    String.format("Cannot convert map to '%s' because the target type doesn't support the conversion!",
                            getShortClassName(objectClass)));
        }
        // Null or empty map means "null" object
        if (map == null || map.size() == 0) {
            return null;
        }
        else {
            // Check proper class
            if (map.containsKey("class")) {
                String className = (String) map.get("class");
                if (!className.equals(getShortClassName(objectClass))) {
                    throw new FaultException(Fault.Common.UNKNOWN_FAULT, String.format(
                            "Cannot convert map to object of class '%s' because map specifies different class '%s'."),
                            getShortClassName(objectClass), className);
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
    public static Map objectToMap(Object object) throws FaultException
    {
        if (!(object instanceof ComplexType)) {
            throw new FaultException(Fault.Common.UNKNOWN_FAULT,
                    String.format("Cannot convert '%s' to map because the object doesn't support the conversion!",
                            getShortClassName(object.getClass())));
        }
        ComplexType complexType = ComplexType.class.cast(object);
        return complexType.toMap();
    }

    /**
     * Get short class name from full class name.
     *
     * @param fullClassName
     * @return short class name
     */
    public static String getShortClassName(String fullClassName)
    {
        int position = fullClassName.lastIndexOf(".");
        if (position != -1) {
            return fullClassName.substring(position + 1, fullClassName.length());
        }
        return fullClassName;
    }

    /**
     * Get short class name from class.
     *
     * @param clazz
     * @return short class name
     */
    public static String getShortClassName(Class clazz)
    {
        if (clazz.getEnclosingClass() != null) {
            return clazz.getEnclosingClass().getSimpleName() + "." + clazz.getSimpleName();
        }
        else {
            return clazz.getSimpleName();
        }
    }

    /**
     * List of packages
     */
    static String[] packages;

    /**
     * Get full class name from short class name
     *
     * @param shortClassName
     * @return full class name
     */
    public static String getFullClassName(String shortClassName)
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

        for (String item : packages) {
            try {
                Class clazz = Class.forName(item + "." + shortClassName);
                return clazz.getCanonicalName();
            }
            catch (ClassNotFoundException exception) {
            }
        }
        return "cz.cesnet.shongo." + shortClassName;
    }
}
