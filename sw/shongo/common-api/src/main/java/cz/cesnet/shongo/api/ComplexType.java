package cz.cesnet.shongo.api;

import cz.cesnet.shongo.util.Property;

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
    /**
     * Annotation used for properties to mark them as required.
     */
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Required
    {
    }

    /**
     * Annotation used for properties to restrict allowed types.
     */
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
    public boolean isPropertyMarkedFilled(String property)
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
     * Checks whether all properties with {@link Required} annotation are marked as filled (recursive).
     *
     * @throws FaultException
     */
    public void checkRequiredPropertiesFilled() throws FaultException
    {
        checkRequiredPropertiesFilled(this);
    }

    /**
     * Check {@link Required} in all properties of {@link ComplexType} or in all items of
     * arrays and collections (recursive).
     *
     * @param object
     * @throws FaultException
     */
    public static void checkRequiredPropertiesFilled(Object object) throws FaultException
    {
        if (object instanceof ComplexType) {
            ComplexType complexType = (ComplexType) object;
            Class type = complexType.getClass();
            String[] propertyNames = Property.getPropertyNames(type);
            for (String name : propertyNames) {
                Property property = Property.getProperty(type, name);
                Required required = property.getAnnotation(Required.class);
                if (property.isArray()) {
                    Object[] array = (Object[]) property.getValue(complexType);
                    if (required != null && array.length == 0) {
                        throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_COLLECTION_IS_REQUIRED, name, type);
                    }
                    for (Object item : array) {
                        checkRequiredPropertiesFilled(item);
                    }

                }
                else if (property.isCollection()) {
                    Collection collection = (Collection) property.getValue(complexType);
                    if (required != null && collection.isEmpty()) {
                        throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_COLLECTION_IS_REQUIRED, name, type);
                    }
                    for (Object item : collection) {
                        checkRequiredPropertiesFilled(item);
                    }
                }
                else if (!complexType.isPropertyMarkedFilled(name) || property.isEmpty(complexType)) {
                    throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_IS_REQUIRED, name, type);
                }
            }
        }
        else if (object instanceof Object[]) {
            Object[] array = (Object[]) object;
            for (Object item : array) {
                checkRequiredPropertiesFilled(item);
            }
        }
        else if (object instanceof Collection) {
            Collection collection = (Collection) object;
            for (Object item : collection) {
                checkRequiredPropertiesFilled(item);
            }
        }
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
