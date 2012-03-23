package cz.cesnet.shongo;

import cz.cesnet.shongo.common.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Map of attributes for some object of type T
 *
 * @param <T>
 *
 * @author Martin Srom
 */
public class AttributeMap<T> extends HashMap<String, Object>
{
    /**
     * Get object property descriptor
     *
     * @param object
     * @param name
     * @return property descriptor
     */
    private PropertyDescriptor getProperty(T object, String name) throws FaultException {
        // Get property descriptor
        PropertyDescriptor propertyDescriptor = null;
        try {
            propertyDescriptor = PropertyUtils.getPropertyDescriptor(object, name);
        } catch (Exception exception) {
        }

        // If property cannot be set, throw attribute not defined
        if ( propertyDescriptor == null || propertyDescriptor.getWriteMethod() == null ) {
            throw new FaultException(Fault.Common.AttributeNotDefined, name,
                    BeanUtils.getClassName(object.getClass().getCanonicalName()));
        }
        return propertyDescriptor;
    }

    /**
     * Get object instance with filled attributes, perform check whether all
     * attributes are defined.
     *
     * @param objectClass object class
     * @return object instance
     */
    public T getObject(Class<T> objectClass) throws FaultException {
        String className = BeanUtils.getClassName(objectClass.getCanonicalName());
        T object = null;
        try {
            object = objectClass.newInstance();
        } catch ( Exception exception ) {
            throw new FaultException(Fault.Common.ClassCannotBeInstanced, className);
        }
        populateObject(object);
        return object;
    }

    /**
     * Populate object by attributes in map
     *
     * @param object
     */
    public void populateObject(T object) throws FaultException {
        String className = BeanUtils.getClassName(object.getClass().getCanonicalName());
        for ( Map.Entry<String, Object> entry : this.entrySet() ) {
            if ( entry.getKey().equals("class") )
                continue;

            // Get property
            PropertyDescriptor propertyDescriptor = getProperty(object, entry.getKey());

            // Get property class
            Class propertyClass = propertyDescriptor.getPropertyType();

            // Convert value to property type
            Object value = entry.getValue();
            // We want only conversion from string (other conversions e.g. XX -> String
            // use toString which we don't want)
            if ( value instanceof String ) {
                try {
                value = BeanUtils.getInstance().getConvertUtils().convert((String)value, propertyClass);
                } catch ( BeanUtils.Exception exception ) {
                    if ( exception.getException() instanceof FaultException )
                        throw (FaultException)exception.getException();
                    else
                        throw new FaultException(Fault.Common.UnknownFault, exception.getException().getMessage());
                }
            }

            // Set property value
            try {
                BeanUtils.getInstance().getPropertyUtils().setProperty(object, entry.getKey(), value);
            } catch (IllegalArgumentException exception) {
                Class valueClass = entry.getValue().getClass();
                // Throw attribute type mismatch exception
                throw new FaultException(Fault.Common.AttributeTypeMismatch, entry.getKey(), className,
                        BeanUtils.getClassName(propertyClass.getCanonicalName()),
                        BeanUtils.getClassName(valueClass.getCanonicalName()));
            } catch (Exception exception) {
                throw new FaultException(Fault.Common.UnknownFault, exception.getMessage());
            }
        }
    }
}
