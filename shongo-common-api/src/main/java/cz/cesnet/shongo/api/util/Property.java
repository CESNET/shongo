package cz.cesnet.shongo.api.util;

import cz.cesnet.shongo.CommonReportSet;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

import static cz.cesnet.shongo.api.ClassHelper.getClassShortName;

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
     * @see TypeFlags
     */
    private int typeFlags;

    /**
     * Array of types which are allowed to be used as :
     * 1) value in the atomic property
     * 2) item in the array or {@link Collection} property
     * 3) value in the {@link Map} property
     */
    private Class[] valueAllowedTypes;

    /**
     * Type which is allowed to be used as key {@link Map} property.
     */
    private Class keyAllowedType;

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
     */
    public void setValue(Object object, Object value, boolean forceAccessible)
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
                throw new CommonReportSet.ClassAttributeReadonlyException(classType.getSimpleName(), name);
            }
        }
        catch (CommonReportSet.ClassAttributeReadonlyException exception) {
            throw exception;
        }
        catch (IllegalArgumentException exception) {
            throw new CommonReportSet.ClassAttributeTypeMismatchException(classType.getSimpleName(), name,
                    getType().getSimpleName(), value.getClass().getSimpleName());
        }
        catch (Exception exception) {
            throw new PropertyException(exception, "Cannot set value of attribute '%s' in class '%s'!", name, classType);
        }
    }

    /**
     * Get value for the property from given object.
     *
     * @param object Object from which the value is retrieved.
     * @return value that was retrieved
     * @throws PropertyException when the value cannot be retrieved
     */
    public Object getValue(Object object) throws PropertyException
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
        throw new PropertyException(thrownException,
                "Cannot get attribute '%s' from object of type '%s'.", name, classType);
    }

    /**
     * @return {@link #name}
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return {@link #type}
     */
    public Class getType()
    {
        return type;
    }

    /**
     * @return {@link #typeFlags}
     */
    public int getTypeFlags()
    {
        return typeFlags;
    }

    /**
     * @return {@link #valueAllowedTypes}
     */
    public Class[] getValueAllowedTypes()
    {
        return valueAllowedTypes;
    }

    /**
     * @return {@link #keyAllowedType}
     */
    public Class getKeyAllowedType()
    {
        return keyAllowedType;
    }

    /**
     * @param value
     * @return true if given value is empty (it equals to {@code null} or it is an empty array, {@link Collection} or
     *         {@link Map), false otherwise
     */
    public boolean isEmptyValue(Object value)
    {
        if (value == null) {
            return true;
        }
        if (TypeFlags.isArray(typeFlags)) {
            return ((Object[]) value).length == 0;
        }
        else if (TypeFlags.isCollection(typeFlags)) {
            return ((Collection) value).size() == 0;
        }
        else if (TypeFlags.isMap(typeFlags)) {
            return ((Map) value).size() == 0;
        }
        return false;
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
     * @return {@link #field}
     */
    public Field getField()
    {
        return field;
    }

    /**
     * @param type
     * @return {@link Class} from given type
     */
    private static Class getTypeAsClass(Type type)
    {
        if (type instanceof Class) {
            return (Class) type;
        }
        else if (type instanceof ParameterizedType) {
            type = ((ParameterizedType) type).getRawType();
            if (type instanceof Class) {
                return (Class) type;
            }
        }
        else if (type instanceof TypeVariable) {
            return Object.class;
        }
        throw new PropertyException("Cannot get class from type %s.", type.toString());
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
                throw new PropertyException(
                        "Property '%s' in object of class '%s' should have same type in getter and setter.",
                        name, getClassShortName(classType));
            }
        }
        else {
            // Set the property type
            this.type = type;
            this.typeFlags = TypeFlags.get(type);
        }

        // Type of value in Array, Collection or Map
        Class valueAllowedType = null;
        // Type of key in Map
        Class keyAllowedType = null;

        // If type is array or collection
        if (TypeFlags.isArray(typeFlags)) {
            valueAllowedType = this.type.getComponentType();
        }
        else if (TypeFlags.isCollection(typeFlags)) {
            if (!(genericType instanceof ParameterizedType)) {
                throw new PropertyException("Array or collection class should be ParameterizedType.");
            }
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            Type[] arguments = parameterizedType.getActualTypeArguments();
            // We support only one argument
            if (arguments.length != 1) {
                throw new PropertyException("Array or collection class should have one generic argument.");
            }
            valueAllowedType = getTypeAsClass(arguments[0]);
        }
        // If type is map
        else if (TypeFlags.isMap(typeFlags)) {
            if (!(genericType instanceof ParameterizedType)) {
                throw new PropertyException("Map class should be ParameterizedType.");
            }
            ParameterizedType parameterizedType = (ParameterizedType) genericType;
            Type[] arguments = parameterizedType.getActualTypeArguments();
            // We support only one argument
            if (arguments.length != 2) {
                throw new PropertyException("Map class should have two generic arguments.");
            }
            keyAllowedType = getTypeAsClass(arguments[0]);
            valueAllowedType = getTypeAsClass(arguments[1]);
        }

        // Check or set value allowed type
        if (valueAllowedType != null) {
            // If value allowed types already exist
            if (this.valueAllowedTypes != null) {
                // Check it it same as new value allowed type
                if (this.valueAllowedTypes.length != 1 || !this.valueAllowedTypes[0].equals(valueAllowedType)) {
                    throw new PropertyException("Property '%s' in object of class '%s' should have same generic type.",
                            name, getClassShortName(classType));
                }
            }
            else {
                this.valueAllowedTypes = new Class[]{valueAllowedType};
            }
        }
        // Check or set key allowed type
        if (keyAllowedType != null) {
            // If key allowed type already exist
            if (this.keyAllowedType != null) {
                // Check it it same as new key allowed type
                if (!this.keyAllowedType.equals(keyAllowedType)) {
                    throw new PropertyException("Property '%s' in object of class '%s' should have same generic type.",
                            name, getClassShortName(classType));
                }
            }
            else {
                this.keyAllowedType = keyAllowedType;
            }
        }
    }

    /**
     * Cache of properties.
     */
    private static Map<Class, Map<String, Property>> propertyCache = new HashMap<Class, Map<String, Property>>();

    /**
     * Instance of {@link Property} which represents property which doesn't exists in {@link #propertyCache}.
     */
    private static final Property PROPERTY_NOT_FOUND = new Property();

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
            // Not found property represents null
            if (property.equals(PROPERTY_NOT_FOUND)) {
                return null;
            }
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
            List<Method> writeMethods = new ArrayList<Method>();
            for (Method method : methods) {
                if (method.getName().equals(readMethodName) && method.getParameterTypes().length == 0) {
                    property.readMethod = method;
                }
                if (method.getName().equals(writeMethodName) && method.getParameterTypes().length == 1) {
                    writeMethods.add(method);
                }
            }
            if (writeMethods.size() > 0) {
                // Select first found write method
                if (writeMethods.size() == 1 || property.readMethod == null) {
                    property.writeMethod = writeMethods.get(0);
                }
                // Select write method which matches readMethod return type
                else {
                    Class readMethodReturnType = property.readMethod.getReturnType();
                    for (Method writeMethod : writeMethods) {
                        Class writeMethodParameterType = writeMethod.getParameterTypes()[0];
                        if (writeMethodParameterType.equals(readMethodReturnType)) {
                            property.writeMethod = writeMethod;
                            break;
                        }
                    }
                    if (property.writeMethod == null) {
                        throw new PropertyException("No write method '%s' has one parameter of type '%s'"
                                + " (which comes from the return type of the getter).",
                                writeMethodName, readMethodReturnType.getCanonicalName());
                    }
                }
            }
            currentType = currentType.getSuperclass();
        }

        // If getter and field are null, property was not found
        if ((property.field == null || !Modifier.isPublic(property.field.getModifiers()))
                && (property.readMethod == null || !Modifier.isPublic(property.readMethod.getModifiers()))) {
            typeCache.put(name, PROPERTY_NOT_FOUND);
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
                    throw new PropertyException("Setter should have one parameter.");
                }
                property.setType(property.writeMethod.getParameterTypes()[0],
                        property.writeMethod.getGenericParameterTypes()[0]);
            }
        }
        // Or from field
        else if (property.field != null) {
            property.setType(property.field.getType(), property.field.getGenericType());
        }

        // Put new property to cache
        typeCache.put(name, property);

        return property;
    }

    /**
     * @param type
     * @param name
     * @return {@link Property} with given name from specified type
     * @throws CommonReportSet.ClassAttributeUndefinedException when property doesn't exist
     */
    public static Property getPropertyNotNull(Class type, String name)
            throws CommonReportSet.ClassAttributeUndefinedException
    {
        Property property = getProperty(type, name);
        if (property == null) {
            throw new CommonReportSet.ClassAttributeUndefinedException(type.getSimpleName(), name);
        }
        return property;
    }

    /**
     * Cache of property names in classes.
     */
    private static Map<Class, Set<String>> classPropertyNamesCache = new HashMap<Class, Set<String>>();

    /**
     * @param type which should be searched for properties
     * @return array of property names declared in given {@code type}
     */
    public static Set<String> getClassPropertyNames(Class type)
    {
        Set<String> propertyNames = classPropertyNamesCache.get(type);
        if (propertyNames == null) {
            propertyNames = new HashSet<String>();
            Field[] declaredFields = type.getDeclaredFields();
            for (Field field : declaredFields) {
                if (Modifier.isPublic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
                    propertyNames.add(field.getName());
                }
            }
            Method[] methods = type.getDeclaredMethods();
            for (Method method : methods) {
                String methodName = method.getName();
                int parameterCount = method.getParameterTypes().length;
                if (methodName.startsWith("get") && parameterCount == 0 && Modifier.isPublic(method.getModifiers())) {
                    String name = methodName.substring(3);
                    name = name.substring(0, 1).toLowerCase() + name.substring(1);
                    propertyNames.add(name);
                }
                if (methodName.startsWith("is") && parameterCount == 0 && Modifier.isPublic(method.getModifiers())) {
                    String name = methodName.substring(2);
                    name = name.substring(0, 1).toLowerCase() + name.substring(1);
                    propertyNames.add(name);
                }
            }
            classPropertyNamesCache.put(type, propertyNames);
        }
        return propertyNames;
    }

    /**
     * Cache of property names in class hierarchy (include subclasses, etc).
     */
    private static Map<Class, Set<String>> classHierarchyPropertyNamesCache = new HashMap<Class, Set<String>>();

    /**
     * @param type which should be searched for properties
     * @return array of property names declared in given {@code type} and all super types
     */
    public static Set<String> getClassHierarchyPropertyNames(Class type)
    {
        Set<String> propertyNames = classHierarchyPropertyNamesCache.get(type);
        if (propertyNames == null) {
            propertyNames = new HashSet<String>();
            // For each class in hierarchy
            Class currentType = type;
            while (currentType != null) {
                if (currentType.equals(Object.class)) {
                    break;
                }
                propertyNames.addAll(getClassPropertyNames(currentType));
                currentType = currentType.getSuperclass();
            }
            classHierarchyPropertyNamesCache.put(type, propertyNames);
        }
        return propertyNames;
    }

    /**
     * @param type      which should be searched for properties
     * @param breakType and all it's super types will not be serched for properties
     * @return array of property names declared in given {@code type} and all super types
     */
    public static Set<String> getClassHierarchyPropertyNames(Class type, Class breakType)
    {
        Set<String> propertyNames = new HashSet<String>();
        // For each class in hierarchy
        Class currentType = type;
        while (currentType != null) {
            if (currentType.equals(Object.class) || breakType.equals(currentType)) {
                break;
            }
            propertyNames.addAll(getClassPropertyNames(currentType));
            currentType = currentType.getSuperclass();
        }
        return propertyNames;
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
     */
    public static void setPropertyValue(Object object, String name, Object value, boolean forceAccessible)
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
     */
    public static void setPropertyValue(Object object, String name, Object value)
    {
        setPropertyValue(object, name, value, false);
    }

    /**
     * Get property value from given object.
     *
     * @param object
     * @param name
     * @return property value
     */
    public static Object getPropertyValue(Object object, String name)
    {
        Property property = getPropertyNotNull(object.getClass(), name);
        return property.getValue(object);
    }

    /**
     * @param type
     * @param name
     * @return result from {@link Property#getType()} ()} for property
     */
    public static Class getPropertyType(Class type, String name)
    {
        Property property = getPropertyNotNull(type, name);
        return property.getType();
    }

    /**
     * @param type
     * @param name
     * @return result from {@link Property#getValueAllowedTypes()} for property
     */
    public static Class[] getPropertyValueAllowedTypes(Class type, String name)
    {
        Property property = getPropertyNotNull(type, name);
        return property.getValueAllowedTypes();
    }

    /**
     * @param type
     * @param name
     * @param annotationClass
     * @return annotation for given type, it's property and annotation class
     */
    public static <T extends Annotation> T getPropertyAnnotation(Class type, String name, Class<T> annotationClass)
    {
        Property property = getPropertyNotNull(type, name);
        return property.getAnnotation(annotationClass);
    }
}
