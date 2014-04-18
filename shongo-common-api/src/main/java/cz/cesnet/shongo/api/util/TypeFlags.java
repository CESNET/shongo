package cz.cesnet.shongo.api.util;

import cz.cesnet.shongo.api.AtomicType;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.ReadablePartial;

import java.util.*;

/**
 * Utility class which can be used to determine whether some {@link Class} is, e.g., {@link #BASIC} or
 * {@link #ARRAY}, etc.).
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TypeFlags
{
    /**
     * Specifies whether type is primitive Java type (e.g., integer, double, etc.).
     */
    public static final int PRIMITIVE = 0x00000001;

    /**
     * Specifies whether class is array (extends {@link Object[]}).
     */
    public static final int ARRAY = 0x00000002;

    /**
     * Specifies whether class is {@link java.util.Collection}.
     */
    public static final int COLLECTION = 0x00000004;

    /**
     * Specifies whether class is {@link java.util.Map}.
     */
    public static final int MAP = 0x000000008;

    /**
     * Specifies whether class is type which can be converted to any single {@link #PRIMITIVE} type.
     */
    public static final int ATOMIC = 0x000000010;

    /**
     * Specifies whether type is common java type ({@link #PRIMITIVE}, {@link #ARRAY},
     * {@link List}, {@link #MAP} or {@link String}).
     */
    public static final int BASIC = 0x00000020;

    /**
     * Cache of type flags.
     */
    private static Map<Class, Integer> typeFlagsByType = new HashMap<Class, Integer>();

    /**
     * @param type for which the flags should be returned
     * @return flags for given {@code type}
     */
    public static int get(Class type)
    {
        Integer typeFlags = typeFlagsByType.get(type);
        if (typeFlags != null) {
            return typeFlags;
        }

        // Initialize new type flags
        typeFlags = 0;
        // Temporary variables for determining flags
        boolean isArrayOrCollectionOrMap = false;
        boolean isArrayOrListOrMap = false;
        boolean isPrimitive = false;

        // Determine types containing items
        if (type.isArray()) {
            typeFlags |= ARRAY;
            isArrayOrCollectionOrMap = true;
            isArrayOrListOrMap = true;
        }
        else if (Collection.class.isAssignableFrom(type)) {
            typeFlags |= COLLECTION;
            isArrayOrCollectionOrMap = true;
            if (List.class.isAssignableFrom(type)) {
                isArrayOrListOrMap = true;
            }
        }
        else if (Map.class.isAssignableFrom(type)) {
            typeFlags |= MAP;
            isArrayOrCollectionOrMap = true;
            isArrayOrListOrMap = true;
        }

        // Determine primitive basic types
        if (type.isPrimitive() || PRIMITIVE_CLASSES.contains(type)) {
            typeFlags |= PRIMITIVE;
            isPrimitive = true;
        }

        // Determine atomic types
        if (!isArrayOrCollectionOrMap) {
            if (isPrimitive || ATOMIC_CLASSES.FINAL.contains(type)) {
                typeFlags |= ATOMIC;
            }
            else {
                for (Class atomicBaseClass : ATOMIC_CLASSES.BASE) {
                    if (atomicBaseClass.isAssignableFrom(type)) {
                        typeFlags |= ATOMIC;
                        break;
                    }
                }
            }
        }

        if (isPrimitive || isArrayOrListOrMap || type.equals(String.class)) {
            typeFlags |= BASIC;
        }

        typeFlagsByType.put(type, typeFlags);

        return typeFlags;
    }

    /**
     * @param object for whose type the flags should be returned
     * @return flags for type of given {@code object}
     */
    public static int get(Object object)
    {
        if (object == null) {
            return 0;
        }
        return get(object.getClass());
    }

    /**
     * @param typeFlags to be checked
     * @return true whether given {@code typeFlags} has {@link #PRIMITIVE} flag, false otherwise
     */
    public static boolean isPrimitive(int typeFlags)
    {
        return (typeFlags & PRIMITIVE) != 0;
    }

    /**
     * @param typeFlags to be checked
     * @return true whether given {@code typeFlags} has {@link #ARRAY} flag, false otherwise
     */
    public static boolean isArray(int typeFlags)
    {
        return (typeFlags & ARRAY) != 0;
    }

    /**
     * @param typeFlags to be checked
     * @return true whether given {@code typeFlags} has {@link #COLLECTION} flag, false otherwise
     */
    public static boolean isCollection(int typeFlags)
    {
        return (typeFlags & COLLECTION) != 0;
    }

    /**
     * @param typeFlags to be checked
     * @return true whether given {@code typeFlags} has {@link #MAP} flag, false otherwise
     */
    public static boolean isMap(int typeFlags)
    {
        return (typeFlags & MAP) != 0;
    }

    /**
     * @param typeFlags to be checked
     * @return true whether given {@code typeFlags} has {@link #ATOMIC} flag, false otherwise
     */
    public static boolean isAtomic(int typeFlags)
    {
        return (typeFlags & ATOMIC) != 0;
    }

    /**
     * @param typeFlags to be checked
     * @return true whether given {@code typeFlags} has {@link #BASIC} flag, false otherwise
     */
    public static boolean isBasic(int typeFlags)
    {
        return (typeFlags & BASIC) != 0;
    }

    /**
     * @param typeFlags to be checked
     * @return true whether given {@code typeFlags} has {@link #ARRAY} or {@link #COLLECTION} flag,
     *         false otherwise
     */
    public static boolean isArrayOrCollection(int typeFlags)
    {
        return isArray(typeFlags) || isCollection(typeFlags);
    }

    /**
     * @param typeFlags to be checked
     * @return true whether given {@code typeFlags} has {@link #ARRAY}, {@link #COLLECTION} or {@link java.util.Map} flag,
     *         false otherwise
     */
    public static boolean isArrayOrCollectionOrMap(int typeFlags)
    {
        return isArray(typeFlags) || isCollection(typeFlags) || isMap(typeFlags);
    }

    /**
     * Set of primitive Java classes.
     */
    private static final Set<Class> PRIMITIVE_CLASSES = new HashSet<Class>()
    {{
            add(Boolean.class);
            add(Character.class);
            add(Byte.class);
            add(Short.class);
            add(Integer.class);
            add(Long.class);
            add(Float.class);
            add(Double.class);
            add(Void.class);
        }};

    /**
     * Sets of types considered as {@link #ATOMIC}.
     */
    public static class ATOMIC_CLASSES
    {
        /**
         * Set of types considered as {@link #ATOMIC} and all types which extends them are also considered as
         * {@link #ATOMIC}.
         */
        private static final Set<Class> BASE = new HashSet<Class>()
        {{
                add(Enum.class);
                add(AtomicType.class);
                add(ReadablePartial.class);
            }};

        /**
         * Set of types considered as {@link #ATOMIC}.
         */
        private static final Set<Class> FINAL = new HashSet<Class>()
        {{
                add(Class.class);
                add(String.class);
                add(Period.class);
                add(DateTime.class);
                add(Interval.class);
            }};
    }
}
