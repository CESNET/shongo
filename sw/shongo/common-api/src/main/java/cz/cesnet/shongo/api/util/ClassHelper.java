package cz.cesnet.shongo.api.util;

import cz.cesnet.shongo.fault.CommonFault;
import cz.cesnet.shongo.fault.FaultException;

import java.util.ArrayList;

/**
 * Class help for API types. Converts long class names to short class names and vice versa.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ClassHelper
{
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
        shortClassName = shortClassName.replace(".", "$");
        for (String item : getPackages()) {
            try {
                Class clazz = Class.forName(item + "." + shortClassName);
                return clazz;
            }
            catch (ClassNotFoundException exception) {
            }
        }
        return Class.forName("cz.cesnet.shongo." + shortClassName);
    }

    /**
     * Gets all packages named cz.cesnet.shongo.*.api
     *
     * @return
     */
    public static String[] getPackages()
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
        return packages;
    }

    /**
     * @param type for the new instance
     * @return new instance of given {@code type}.
     * @throws FaultException when class cannot be instanced
     */
    public static <T> T createInstanceFromClass(Class<T> type) throws FaultException
    {
        T instance = null;
        try {
            instance = type.newInstance();
        }
        catch (Exception exception) {
            throw new FaultException(CommonFault.CLASS_CANNOT_BE_INSTANCED, type);
        }
        return instance;
    }

    /**
     * @param type for the new instance
     * @return new instance of given {@code type}.
     * @throws RuntimeException when class cannot be instanced
     */
    public static <T> T createInstanceFromClassRuntime(Class<T> type)
    {
        T instance = null;
        try {
            instance = type.newInstance();
        }
        catch (Exception exception) {
            throw new IllegalStateException(new FaultException(CommonFault.CLASS_CANNOT_BE_INSTANCED, type));
        }
        return instance;
    }
}
