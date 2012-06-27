package cz.cesnet.shongo.common.util;

import cz.cesnet.shongo.common.xmlrpc.BeanUtils;
import cz.cesnet.shongo.common.xmlrpc.Fault;
import cz.cesnet.shongo.common.xmlrpc.FaultException;

import java.util.ArrayList;
import java.util.Map;

/**
 * Helper class for converting types.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Converter
{
    /**
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
                    getShortClassName(enumClass));
        }
    }

    /**
     * @param map
     * @param objectClass
     * @return new instance of given object class that is filled by attributes from given map
     * @throws FaultException
     */
    public static Object convertMapToObject(Map map, Class objectClass) throws FaultException
    {
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
            Object object = null;
            try {
                object = objectClass.newInstance();
            }
            catch (Exception exception) {
                throw new FaultException(Fault.Common.CLASS_CANNOT_BE_INSTANCED,
                        objectClass.getCanonicalName());
            }
            try {
                BeanUtils.getInstance().populateRecursive(object, map);
            }
            catch (Exception exception) {
                if (exception instanceof FaultException) {
                    throw (FaultException) exception;
                }
                if (exception.getCause() instanceof FaultException) {
                    throw (FaultException) exception.getCause();
                }
                throw new FaultException(Fault.Common.UNKNOWN_FAULT);
            }
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
        try {
            return BeanUtils.getInstance().describeRecursive(object);
        }
        catch (Exception exception) {
            throw new FaultException(Fault.Common.UNKNOWN_FAULT, String.format(
                    "Failed to convert object of class '%s' to map."),
                    getShortClassName(object.getClass()));
        }
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
