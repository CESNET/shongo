package cz.cesnet.shongo.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Array;
import java.util.*;

/**
 * Represents a type that can be serialized
 * to/from {@link java.util.Map}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public abstract class ComplexType
{
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Required
    {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface NotEmpty
    {
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface AllowedTypes
    {
        Class[] value();

        Class[] defaultValue() default {};
    }

    /**
     * Set of fields which was marked as filled.
     */
    Set<String> filledProperties = new HashSet<String>();

    /**
     * @param property
     * @return true if given field was marked as filled,
     *         false otherwise
     */
    public boolean isPropertyFilled(String property)
    {
        return filledProperties.contains(property);
    }

    /**
     * Mark given property as filled.
     *
     * @param property
     */
    public void markPropertyFilled(String property)
    {
        filledProperties.add(property);
    }

    /**
     * Clear all marked properties
     */
    public void clearPropertyFilledMarks()
    {
        filledProperties.clear();
    }

    /**
     * @param collection
     * @param componentType
     * @return array converted from collection
     */
    public static <T> T[] toArray(Collection<T> collection, Class<T> componentType)
    {
        @SuppressWarnings("unchecked")
        T[] array = (T[]) Array.newInstance(componentType, collection.size());
        return collection.toArray(array);
    }

    /**
     * @param array
     * @return list converted from array
     */
    public static <T> List<T> fromArray(T[] array)
    {
        return new ArrayList<T>(Arrays.asList(array));
    }
}
