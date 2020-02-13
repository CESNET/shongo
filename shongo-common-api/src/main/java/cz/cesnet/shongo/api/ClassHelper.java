package cz.cesnet.shongo.api;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.TodoImplementException;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Class helper for API types. Converts long class names to short class names and vice versa.
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
    static private String[] packages;

    /**
     * {@link Class} by API short name.
     */
    static private Map<String, Class> classByShortNameCache = new HashMap<String, Class>();

    /**
     * Set short name for given {@code type}.
     *
     * @param type
     * @param typeShortName
     * @throws RuntimeException when the short name is already set
     */
    public static void setClassShortName(Class type, String typeShortName) throws RuntimeException
    {
        if (classByShortNameCache.containsKey(typeShortName)) {
            throw new RuntimeException("Short name '" + typeShortName + "' is already set.");
        }
        classByShortNameCache.put(typeShortName, type);
    }

    /**
     * @see #setClassShortName(Class, String)
     */
    public static void registerClassShortName(Class type)
    {
        setClassShortName(type, getClassShortName(type));
    }

    /**
     * Get full class name from short class name
     *
     * @param shortClassName
     * @return full class name
     */
    public static Class getClassFromShortName(String shortClassName) throws ClassNotFoundException
    {
        Class type = classByShortNameCache.get(shortClassName);
        if (type == null) {
            shortClassName = shortClassName.replace(".", "$");
            for (String item : getPackages()) {
                try {
                    Class clazz = Class.forName(item + "." + shortClassName);
                    return clazz;
                }
                catch (ClassNotFoundException exception) {
                }
            }
            type = Class.forName("cz.cesnet.shongo." + shortClassName);
            classByShortNameCache.put(shortClassName, type);
        }
        return type;
    }

    /**
     * Gets all packages named cz.cesnet.shongo.*[.api/.api.request]
     *
     * @return
     */
    public static String[] getPackages()
    {
        if (packages == null) {
            ArrayList<String> list = new ArrayList<String>();
            for (Package item : Package.getPackages()) {
                String name = item.getName();
                if (name.startsWith("cz.cesnet.shongo.") && ((name.endsWith(".api") || name.endsWith("api.request")))) {
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
     * @throws CommonReportSet.ClassInstantiationErrorException
     *          when class cannot be instanced
     */
    public static <T> T createInstanceFromClass(Class<T> type) throws CommonReportSet.ClassInstantiationErrorException
    {
        T instance;
        try {
            instance = type.getDeclaredConstructor().newInstance();
        }
        catch (Exception exception) {
            throw new CommonReportSet.ClassInstantiationErrorException(exception, type.getSimpleName());
        }
        return instance;
    }

    /**
     * @param type for the new instance
     * @return new instance of given {@code type}.
     * @throws CommonReportSet.ClassInstantiationErrorException
     *          when class cannot be instanced
     */
    public static <T, A> T createInstanceFromClass(Class<T> type, Class<A> argumentType, A argumentValue)
            throws CommonReportSet.ClassInstantiationErrorException
    {
        T instance;
        try {
            instance = type.getDeclaredConstructor(argumentType).newInstance(argumentValue);
        }
        catch (Exception exception) {
            throw new CommonReportSet.ClassInstantiationErrorException(exception, type.getSimpleName());
        }
        return instance;
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
     * @param type type of {@link Collection}
     * @param size size of {@link Collection}
     * @return new instance of {@link Collection} of given size
     * @throws CommonReportSet.ClassInstantiationErrorException
     *
     */
    public static Collection<Object> createCollection(Class<? extends Collection> type, int size)
            throws CommonReportSet.ClassInstantiationErrorException
    {
        if (List.class.isAssignableFrom(type)) {
            return new LinkedList<Object>();
        }
        else if (Set.class.isAssignableFrom(type)) {
            return new HashSet<Object>(size);
        }
        else if (Collection.class.equals(type)) {
            return new LinkedList<Object>();
        }
        throw new CommonReportSet.ClassInstantiationErrorException(type.getSimpleName());
    }

    /**
     * @param collection to be duplicated
     * @return new instance of {@link Collection} with given items
     */
    public static <T> Collection<T> duplicateCollection(Collection<T> collection)
            throws CommonReportSet.ClassInstantiationErrorException
    {
        if (collection instanceof List) {
            return new LinkedList<T>(collection);
        }
        else if (collection instanceof Set) {
            return new HashSet<T>(collection);
        }
        else {
            throw new TodoImplementException(collection.getClass());
        }
    }
}
