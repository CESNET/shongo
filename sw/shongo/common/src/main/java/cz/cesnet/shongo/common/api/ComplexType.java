package cz.cesnet.shongo.common.api;

import cz.cesnet.shongo.common.util.Converter;
import cz.cesnet.shongo.common.util.Property;

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
     * Set of fields which was filled during the last invocation of {@link #fromMap(java.util.Map)}
     */
    private Set<String> filledProperties = new HashSet<String>();

    /**
     * @param property
     * @return true if given field was filled during the last invocation of {@link #fromMap(java.util.Map)},
     *         false otherwise
     */
    public boolean isPropertyFilled(String property)
    {
        return filledProperties.contains(property);
    }

    /**
     * Convert type to {@link Map}.
     *
     * @return {@link Map}
     * @throws FaultException when conversion fails
     */
    public Map toMap() throws FaultException
    {
        Map<String, Object> map = new HashMap<String, Object>();
        String[] propertyNames = Property.getPropertyNames(getClass());
        for (String property : propertyNames) {
            Object value = Property.getPropertyValue(this, property);
            if (value == null) {
                continue;
            }
            else if (value instanceof ComplexType) {
                value = ((ComplexType) value).toMap();
            }
            else if (value instanceof Object[]) {
                Object[] oldArray = (Object[]) value;
                Object[] newArray = new Object[oldArray.length];
                for (int index = 0; index < oldArray.length; index++) {
                    Object object = oldArray[index];
                    if (object instanceof ComplexType) {
                        object = ((ComplexType) object).toMap();
                    }
                    newArray[index] = object;
                }
                value = newArray;
            }
            map.put(property, value);
        }
        map.put("class", Converter.getClassShortName(getClass()));
        return map;
    }

    /**
     * Fill type from {@link Map}.
     *
     * @param map {@link Map} from which the object should be filled
     * @throws FaultException when filling failed
     */
    public void fromMap(Map map) throws FaultException
    {
        // Clear all filled properties
        filledProperties.clear();

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
            Property propertyDefinition = Property.getPropertyNotNull(getClass(), property);
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
                throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_TYPE_MISMATCH, property,
                        getClass(),
                        type,
                        value.getClass());
            }

            // Set the value to property
            Property.setPropertyValue(this, property, value);

            // Mark property as filled
            filledProperties.add(property);
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
