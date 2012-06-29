package cz.cesnet.shongo.common.util;

import cz.cesnet.shongo.common.api.ComplexType;
import cz.cesnet.shongo.common.api.Fault;
import cz.cesnet.shongo.common.api.FaultException;
import org.apache.commons.beanutils.PropertyUtils;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

/**
 * Class that represents a declared property in a class.
 * It can be used for accessing properties in objects.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Property
{
    /**
     * Class in which the property is declared.
     */
    private Class classType;

    /**
     * Name of the property.
     */
    private String name;

    /**
     * Type of the property.
     */
    private Class type;

    /**
     * Array of allowed types for the property or for items contained in array or collection property.
     */
    private Class[] allowedTypes;

    /**
     * Field of the property.
     */
    private Field field;

    /**
     * Getter for the property.
     */
    private Method readMethod;

    /**
     * Setter for the property.
     */
    private Method writeMethod;

    /**
     * Set value for the property to given object.
     *
     * @param object Object to which the value should be set
     * @param value  Value to set
     * @throws FaultException when the value cannot be set
     */
    public void setValue(Object object, Object value) throws FaultException
    {
        try {
            if (writeMethod != null) {
                PropertyUtils.setProperty(object, name, value);
                return;
            }
            else if (field != null) {
                field.set(object, value);
                return;
            }
            else if (readMethod != null) {
                throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_READ_ONLY, name, classType);
            }
        }
        catch (FaultException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_TYPE_MISMATCH,
                    name, classType, getType(), value.getClass());
        }
    }

    /**
     * Get value for the property from given object.
     *
     * @param object Object from which the value is retrieved.
     * @return value that was retrieved
     * @throws FaultException when the value cannot be retrieved
     */
    public Object getValue(Object object) throws FaultException
    {
        try {
            if (readMethod != null) {
                return readMethod.invoke(object);
            }
            else if (field != null) {
                return field.get(object);
            }
            else if (writeMethod != null) {
                throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_WRITE_ONLY, name, classType);
            }
        }
        catch (FaultException exception) {
            throw exception;
        }
        catch (Exception exception) {
            throw new FaultException(exception, String.format("Cannot get attribute '%s' from object of type '%s'.",
                    name, Converter.getClassShortName(classType)));
        }
        return null;
    }

    /**
     * @return {@link #type}
     */
    public Class getType()
    {
        return type;
    }

    /**
     * @return {@link #allowedTypes}
     */
    public Class[] getAllowedTypes()
    {
        return allowedTypes;
    }

    /**
     * @return {@link #allowedTypes}
     */
    public String getAllowedTypesAsString()
    {
        StringBuilder builder = new StringBuilder();
        for (Class allowedType : allowedTypes) {
            if (builder.length() > 0) {
                builder.append("|");
            }
            builder.append(Converter.getClassShortName(allowedType));
        }
        return builder.toString();
    }

    /**
     * @param annotationClass
     * @return property annotation if exists,
     *         null otherwise
     */
    private <T extends Annotation> T getAnnotation(Class<T> annotationClass)
    {
        T annotation = null;
        if (annotation == null && field != null) {
            annotation = field.getAnnotation(annotationClass);
        }
        if (annotation == null && readMethod != null) {
            annotation = readMethod.getAnnotation(annotationClass);
        }
        if (annotation == null && writeMethod != null) {
            annotation = writeMethod.getAnnotation(annotationClass);
        }
        return annotation;
    }

    /**
     * Set the property type.
     *
     * @param type        Declared property type
     * @param genericType Declared generic property type (for retrieving generic arguments)
     */
    private void setType(Class type, Type genericType)
    {
        // Check if type is same as already set
        if (this.type != null) {
            if (!this.type.equals(type)) {
                throw new IllegalStateException(String.format(
                        "Property '%s' in object of class '%s' should have same type in getter and setter.",
                        name, Converter.getClassShortName(classType)));
            }
        }
        else {
            // Set the property type
            this.type = type;
        }

        // If type is array or collection
        if (this.type.isArray() || Collection.class.isAssignableFrom(this.type)) {
            Class argumentType = null;
            if (this.type.isArray()) {
                argumentType = this.type.getComponentType();
            }
            else {
                if (!(genericType instanceof ParameterizedType)) {
                    throw new IllegalStateException("Array or collection class should be ParameterizedType.");
                }
                ParameterizedType parameterizedType = (ParameterizedType) genericType;
                Type[] arguments = parameterizedType.getActualTypeArguments();
                // We support only one argument
                if (arguments.length != 1) {
                    throw new IllegalStateException("Array or collection class should have one generic argument.");
                }
                // Argument should be Class
                if (!(arguments[0] instanceof Class)) {
                    throw new IllegalStateException("Generic argument should be class");
                }
                argumentType = (Class) arguments[0];
            }
            if (this.allowedTypes != null) {
                if (!(this.allowedTypes.length == 1 && this.allowedTypes[0].equals(argumentType))) {
                    throw new IllegalStateException(String.format(
                            "Property '%s' in object of class '%s' should have same generic type.",
                            name, Converter.getClassShortName(classType)));
                }
            }
            else {
                this.allowedTypes = new Class[]{argumentType};
            }
        }
    }

    /**
     * Cache of properties.
     */
    private static Map<Class, Map<String, Property>> propertyCache = new HashMap<Class, Map<String, Property>>();

    /**
     * @param type
     * @param name
     * @return {@link Property} with given name from specified type if exists,
     *         null otherwise
     */
    public static Property getProperty(Class type, String name)
    {
        // Get property from cache
        Map<String, Property> typeCache = propertyCache.get(type);
        if (typeCache == null) {
            typeCache = new HashMap<String, Property>();
            propertyCache.put(type, typeCache);
        }
        Property property = typeCache.get(name);
        if (property != null) {
            return property;
        }

        // If not found create new
        property = new Property();
        property.name = name;
        property.classType = type;

        // Get field
        try {
            property.field = type.getDeclaredField(name);
            if (property.field != null && !Modifier.isPublic(property.field.getModifiers())) {
                property.field = null;
            }
        }
        catch (NoSuchFieldException e) {
        }

        // Get read and write methods
        PropertyDescriptor propertyDescriptor = null;
        try {
            for (PropertyDescriptor possiblePropertyDescriptor : PropertyUtils.getPropertyDescriptors(type)) {
                if (name.equals(possiblePropertyDescriptor.getName())) {
                    propertyDescriptor = possiblePropertyDescriptor;
                    break;
                }
            }
            if (propertyDescriptor != null) {
                property.writeMethod = propertyDescriptor.getWriteMethod();
                property.readMethod = propertyDescriptor.getReadMethod();
            }
        }
        catch (Exception exception) {
        }

        // If all values are null, property was not found
        if (property.field == null && property.readMethod == null && property.writeMethod == null) {
            return null;
        }

        // Determine types from getter and setter
        if (property.readMethod != null || property.writeMethod != null) {
            if (property.readMethod != null) {
                property.setType(property.readMethod.getReturnType(), property.readMethod.getGenericReturnType());
            }
            if (property.writeMethod != null) {
                if (property.writeMethod.getParameterTypes().length != 1
                        || property.writeMethod.getGenericParameterTypes().length != 1) {
                    throw new IllegalStateException("Setter should have one parameter.");
                }
                property.setType(property.writeMethod.getParameterTypes()[0],
                        property.writeMethod.getGenericParameterTypes()[0]);
            }
        }
        // Or from field
        else if (property.field != null) {
            property.setType(property.field.getType(), property.field.getGenericType());
        }

        // Determine allowed types
        ComplexType.AllowedTypes allowedTypes = property.getAnnotation(ComplexType.AllowedTypes.class);
        // Explicitly by annotation
        if (allowedTypes != null) {
            if (allowedTypes.value().length > 0) {
                property.allowedTypes = allowedTypes.value();
            }
        }

        // Put new property to cache
        typeCache.put(name, property);

        return property;
    }

    /**
     * @param type
     * @param name
     * @return {@link Property} with given name from specified type
     * @throws FaultException when property doesn't exist
     */
    public static Property getPropertyNotNull(Class type, String name) throws FaultException
    {
        Property property = getProperty(type, name);
        if (property == null) {
            throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_NOT_DEFINED, name, Converter.getClassShortName(type));
        }
        return property;
    }

    /**
     * @param type
     * @return array of property names for given class
     * @throws FaultException
     */
    public static String[] getPropertyNames(Class type) throws FaultException
    {
        Set<String> propertyNames = new HashSet<String>();

        // Add properties by fields
        Field[] declaredFields = type.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            if (Modifier.isPublic(declaredField.getModifiers())) {
                propertyNames.add(declaredField.getName());
            }
        }

        // Add properties by getters/setters
        PropertyDescriptor[] descriptors = PropertyUtils.getPropertyDescriptors(type);
        for (PropertyDescriptor propertyDescriptor : descriptors) {
            String name = propertyDescriptor.getName();
            if (name.equals("class")) {
                continue;
            }
            if (!propertyNames.contains(name)) {
                propertyNames.add(name);
            }
        }

        return propertyNames.toArray(new String[propertyNames.size()]);
    }

    /**
     * @param type
     * @param name
     * @return true if property exist,
     *         false otherwise
     */
    public static boolean hasProperty(Class type, String name)
    {
        return getProperty(type, name) != null;
    }

    /**
     * Set property value to given object.
     *
     * @param object
     * @param name
     * @param value
     * @throws FaultException
     */
    public static void setPropertyValue(Object object, String name, Object value) throws FaultException
    {
        Property property = getPropertyNotNull(object.getClass(), name);
        property.setValue(object, value);
    }

    /**
     * Get property value from given object.
     *
     * @param object
     * @param name
     * @return property value
     * @throws FaultException
     */
    public static Object getPropertyValue(Object object, String name) throws FaultException
    {
        Property property = getPropertyNotNull(object.getClass(), name);
        return property.getValue(object);
    }

    /**
     * @param type
     * @param name
     * @return result from {@link Property#getType()} ()} for property
     * @throws FaultException
     */
    public static Class getPropertyType(Class type, String name) throws FaultException
    {
        Property property = getPropertyNotNull(type, name);
        return property.getType();
    }

    /**
     * @param type
     * @param name
     * @return result from {@link Property#getAllowedTypes()} for property
     * @throws FaultException
     */
    public static Class[] getPropertyAllowedTypes(Class type, String name) throws FaultException
    {
        Property property = getPropertyNotNull(type, name);
        return property.getAllowedTypes();
    }
}
