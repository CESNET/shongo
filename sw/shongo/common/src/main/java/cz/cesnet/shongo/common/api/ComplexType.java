package cz.cesnet.shongo.common.api;

import cz.cesnet.shongo.common.util.Converter;
import org.apache.commons.beanutils.PropertyUtils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    public boolean isFilled(String property)
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
        PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors(this);
        for (PropertyDescriptor propertyDescriptor : descriptors) {
            String property = propertyDescriptor.getName();
            try {
                Object value = PropertyUtils.getProperty(this, property);
                if ( value == null ) {
                    continue;
                }
                else if ( value instanceof ComplexType ) {
                    value = ((ComplexType)value).toMap();
                }
                else if ( value instanceof Object[] ) {
                    Object[] oldArray = (Object[])value;
                    Object[] newArray = new Object[oldArray.length];
                    for ( int index = 0; index < oldArray.length; index++ ) {
                        Object object = oldArray[index];
                        if ( object instanceof ComplexType) {
                            object = ((ComplexType)object).toMap();
                        }
                        newArray[index] = object;
                    }
                    value = newArray;
                }
                
                map.put(property, value);
            }
            catch (Exception exception) {
                throw new FaultException(exception);
            }
        }

        map.put("class", Converter.getShortClassName(getClass()));

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

            // Get allowed types for property
            Class[] allowedTypes = getAllowedPropertyTypes(property);
            // Convert value to single proper type
            if (allowedTypes.length == 1) {
                try {
                    value = Converter.convert(value, allowedTypes[0]);
                }
                catch (IllegalArgumentException exception) {
                    throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_TYPE_MISMATCH, property,
                            Converter.getShortClassName(getClass()),
                            Converter.getShortClassName(allowedTypes[0]),
                            Converter.getShortClassName(value.getClass()));
                }
            }
            // Try to convert value to any proper type
            else if (allowedTypes.length > 1) {
                Object allowedValue = null;
                for (Class allowedType : allowedTypes) {
                    try {
                        allowedValue = Converter.convert(value, allowedType);
                        break;
                    }
                    catch (Exception exception) {
                    }
                }
                if (allowedValue != null) {
                    value = allowedValue;
                }
                else {
                    StringBuilder builder = new StringBuilder();
                    for (Class allowedType : allowedTypes) {
                        if (builder.length() > 0) {
                            builder.append("|");
                        }
                        builder.append(Converter.getShortClassName(allowedType));
                    }
                    throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_TYPE_MISMATCH, property,
                            Converter.getShortClassName(getClass()),
                            builder.toString(),
                            Converter.getShortClassName(value.getClass()));
                }
            }
            else {
                throw new FaultException(Fault.Common.UNKNOWN_FAULT,
                        String.format("Property '%s' in class '%' has none proper type.",
                                Converter.getShortClassName(getClass())));
            }

            // Set the value to property
            try {
                PropertyUtils.setProperty(this, property, value);
            }
            catch (Exception exception) {
                throw new FaultException(exception,
                        String.format("Attribute '%s' in class '%s' cannot be set.", property,
                                Converter.getShortClassName(getClass())));
            }

            // Mark property as filled
            filledProperties.add(property);
        }
    }

    /**
     * @param property
     * @return array of allowed types for given property
     * @throws FaultException when property doesn't exist
     */
    private Class[] getAllowedPropertyTypes(String property) throws FaultException
    {
        try {
            PropertyDescriptor propertyDescriptor = PropertyUtils.getPropertyDescriptor(this, property);
            if (propertyDescriptor != null) {
                Method writeMethod = propertyDescriptor.getWriteMethod();
                if (writeMethod != null) {
                    AllowedTypes allowedTypes = writeMethod.getAnnotation(AllowedTypes.class);
                    if (allowedTypes != null) {
                        return allowedTypes.value();
                    }
                }
                return new Class[]{propertyDescriptor.getPropertyType()};
            }
        }
        catch (Exception exception) {
        }
        throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_NOT_DEFINED, property,
                Converter.getShortClassName(getClass()));
    }
}
