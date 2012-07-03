package cz.cesnet.shongo.controller.api.util;

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
