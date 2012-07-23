package cz.cesnet.shongo.api.util;

import cz.cesnet.shongo.api.Fault;
import cz.cesnet.shongo.api.FaultException;
import cz.cesnet.shongo.api.annotation.AllowedTypes;
import cz.cesnet.shongo.api.annotation.ReadOnly;
import cz.cesnet.shongo.api.annotation.Required;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

import static cz.cesnet.shongo.api.util.ClassHelper.getClassShortName;

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
     * True if {@link ReadOnly} annotation is present for the property, false otherwise.
     */
    private boolean readOnly;

    /**
     * @return {@link #readOnly}
     */
    public boolean isReadOnly()
    {
        return readOnly;
    }

    /**
     * Set value for the property to given object.
     *
     * @param object Object to which the value should be set
     * @param value  Value to set
     * @throws FaultException when the value cannot be set
     */
    public void setValue(Object object, Object value, boolean forceAccessible) throws FaultException
    {
        try {
            if (writeMethod != null && (Modifier.isPublic(writeMethod.getModifiers()) || forceAccessible)) {
                if (forceAccessible) {
                    writeMethod.setAccessible(true);
                    writeMethod.invoke(object, value);
                    writeMethod.setAccessible(false);
                }
                else {
                    writeMethod.invoke(object, value);
                }
                return;
            }
            else if (field != null && (Modifier.isPublic(field.getModifiers()) || forceAccessible)) {
                if (forceAccessible) {
                    field.setAccessible(true);
                    field.set(object, value);
                    field.setAccessible(false);
                }
                else {
                    field.set(object, value);
                }
                return;
            }
            else if (readMethod != null) {
                throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_READ_ONLY, name, classType);
            }
        }
        catch (FaultException exception) {
            throw exception;
        }
        catch (IllegalArgumentException exception) {
            throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_TYPE_MISMATCH,
                    name, classType, getType(), value.getClass());
        }
        catch (Exception exception) {
            throw new FaultException(exception, "Cannot set value of attribute '%s' in class '%s'!", name, classType);
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
        Exception thrownException = null;
        try {
            if (readMethod != null && Modifier.isPublic(readMethod.getModifiers())) {
                return readMethod.invoke(object);
            }
            else if (field != null && Modifier.isPublic(field.getModifiers())) {
                return field.get(object);
            }
        }
        catch (Exception exception) {
            thrownException = exception;
        }
        throw new FaultException(thrownException, "Cannot get attribute '%s' from object of type '%s'.", name, classType);
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
     * @return true if property is annotated with {@link Required},
     *         false otherwise
     */
    public boolean isRequired()
    {
        return getAnnotation(Required.class) != null;
    }

    /**
     * @return true if property is of {@link Object[]} type,
     *         false otherwise
     */
    public boolean isArray()
    {
        return type.isArray();
    }

    /**
     * @return true if property is of {@link Collection} type,
     *         false otherwise
     */
    public boolean isCollection()
    {
        return Collection.class.isAssignableFrom(type);
    }

    /**
     * @return true if property is of {@link Object[]} or {@link Collection} type,
     *         false otherwise
     */
    public boolean isArrayOrCollection()
    {
        return isArray() || isCollection();
    }

    /**
     * @param annotationClass
     * @return property annotation if exists,
     *         null otherwise
     */
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass)
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
                        name, getClassShortName(classType)));
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
                            name, getClassShortName(classType)));
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

        // Get access method names
        String upperName = name.substring(0, 1).toUpperCase() + name.substring(1);
        String readMethodName = "get" + upperName;
        String writeMethodName = "set" + upperName;

        // Get field and access methods
        Class currentType = type;
        while (currentType != null) {
            try {
                Field field = currentType.getDeclaredField(name);
                if (field != null && !Modifier.isFinal(field.getModifiers())) {
                    property.field = field;
                }
            }
            catch (NoSuchFieldException e) {
            }

            Method[] methods = currentType.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(readMethodName) && method.getParameterTypes().length == 0) {
                    property.readMethod = method;
                }
                if (method.getName().equals(writeMethodName) && method.getParameterTypes().length == 1) {
                    property.writeMethod = method;
                }
            }
            currentType = currentType.getSuperclass();
        }

        // If all values are null, property was not found
        if ((property.field == null || !Modifier.isPublic(property.field.getModifiers()))
                && (property.readMethod == null || !Modifier.isPublic(property.readMethod.getModifiers()))
                && (property.writeMethod == null || !Modifier.isPublic(property.writeMethod.getModifiers()))) {
            // TODO: Cache also not found properties
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
        AllowedTypes allowedTypes = property.getAnnotation(AllowedTypes.class);
        // Explicitly by annotation
        if (allowedTypes != null) {
            if (allowedTypes.value().length > 0) {
                property.allowedTypes = allowedTypes.value();
            }
        }

        // Determine read-only
        ReadOnly readOnly = property.getAnnotation(ReadOnly.class);
        if (readOnly != null) {
            property.readOnly = true;
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
            throw new FaultException(Fault.Common.CLASS_ATTRIBUTE_NOT_DEFINED, name, type);
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
        Class currentType = type;
        while (currentType != null) {
            if (currentType.equals(ChangesTrackingObject.class) || currentType.equals(Object.class)) {
                break;
            }
            Field[] declaredFields = currentType.getDeclaredFields();
            for (Field field : declaredFields) {
                if (Modifier.isPublic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
                    propertyNames.add(field.getName());
                }
            }
            Method[] methods = currentType.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();
                int parameterCount = method.getParameterTypes().length;
                if (((methodName.startsWith("get") && parameterCount == 0)
                             || (methodName.startsWith("set") && parameterCount == 1))
                        && Modifier.isPublic(method.getModifiers())) {
                    String name = methodName.substring(3);
                    name = name.substring(0, 1).toLowerCase() + name.substring(1);
                    propertyNames.add(name);
                }
            }
            currentType = currentType.getSuperclass();
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
     * @param forceAccessible
     * @throws FaultException
     */
    public static void setPropertyValue(Object object, String name, Object value, boolean forceAccessible)
            throws FaultException
    {
        Property property = getPropertyNotNull(object.getClass(), name);
        property.setValue(object, value, forceAccessible);
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
        setPropertyValue(object, name, value, false);
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

    /**
     * @param type
     * @param name
     * @param annotationClass
     * @return annotation for given type, it's property and annotation class
     * @throws FaultException
     */
    public static <T extends Annotation> T getPropertyAnnotation(Class type, String name, Class<T> annotationClass)
            throws FaultException
    {
        Property property = getPropertyNotNull(type, name);
        return property.getAnnotation(annotationClass);
    }
}
