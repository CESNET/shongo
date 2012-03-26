package cz.cesnet.shongo.common;

import cz.cesnet.shongo.Fault;
import cz.cesnet.shongo.FaultException;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.MethodUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Bean utils
 *
 * @author Martin Srom
 */
public class BeanUtils extends BeanUtilsBean
{
    /**
     * Constructor
     */
    public BeanUtils() {
        // Set convert utils that are able to convert String to Enum
        super(new ConvertUtilsBean(){
            @Override
            public Object convert(String value, Class clazz) {
                if ( clazz.isEnum() ){
                    try {
                        return Enum.valueOf(clazz, value);
                    } catch (java.lang.IllegalArgumentException exception) {
                        throw new Exception(
                            new FaultException(Fault.Common.EnumNotDefined, value, getClassName(clazz.getCanonicalName()))
                        );
                    }
                } else {
                    return super.convert(value, clazz);
                }
            }
        });
    }

    /**
     * BeanUtils runtime exception that holds another not runtime exception
     *
     * @author Martin Srom
     */
    public static class Exception extends RuntimeException
    {
        public Exception(java.lang.Exception exception) {
            super(exception);
        }

        public java.lang.Exception getException() {
            return (java.lang.Exception)getCause();
        }
    }

    /**
     * Get short class name
     *
     * @param className
     * @return
     */
    public static String getClassName(String className) {
        return className.replace("cz.cesnet.shongo.", "");
    }

    /**
     * Populate bean from map
     *
     * @param bean
     * @param properties
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public void populateRecursive(Object bean, Map properties) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        // Do nothing unless both arguments have been specified
        if ((bean == null) || (properties == null)) {
            return;
        }

        // Loop through the property name/value pairs to be set
        Iterator entries = properties.entrySet().iterator();
        while (entries.hasNext()) {
            // Identify the property name and value(s) to be assigned
            Map.Entry entry = (Map.Entry)entries.next();
            String name = (String) entry.getKey();
            if (name == null || name == "class" ) {
                continue;
            }
            Object value = entry.getValue();

            PropertyDescriptor propertyDescriptor = getPropertyUtils().getPropertyDescriptor(bean, name);
            if ( propertyDescriptor != null) {
                Class propertyClass = propertyDescriptor.getPropertyType();
                // Convert object array to concrete type of array
                if ( propertyClass.isArray() && value instanceof Object[] ) {
                    Object[] valueArray = (Object[])value;
                    try {
                        value = Arrays.copyOf(valueArray, valueArray.length, propertyClass);
                        System.out.println(value.toString());
                        System.out.println(valueArray.toString());
                    } catch (ClassCastException exception) {
                        exception.printStackTrace();
                    }
                }
            }

            // Perform the assignment for this property
            setProperty(bean, name, value);
        }
    }

    /**
     * Recursive describe bean to map
     *
     * @param bean
     * @return map
     */
    public Map describeRecursive(Object bean) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if ( bean == null || bean instanceof DynaBean ) {
            return super.describe(bean);
        }

        Map map = new HashMap();
        PropertyDescriptor[] descriptors = getPropertyUtils().getPropertyDescriptors(bean);
        Class clazz = bean.getClass();
        for (int i = 0; i < descriptors.length; i++) {
            String name = descriptors[i].getName();
            if ( MethodUtils.getAccessibleMethod(clazz, descriptors[i].getReadMethod()) != null) {
                Object value = getPropertyUtils().getNestedProperty(bean, name);
                // Skip null values
                if ( value == null) {
                    continue;
                }
                // Type attribute must be recursively described
                else if ( value instanceof Type ) {
                    value = describeRecursive(value);
                }
                // Array is passed only when not empty
                else if ( value instanceof Object[] ) {
                    Object[] arrayValue = (Object[])value;
                    if ( arrayValue.length == 0 )
                        continue;
                    value = arrayValue;
                }
                // Map is passed only when not empty
                else if ( value instanceof Map ) {
                    Map mapValue = (Map)value;
                    if ( map.size() == 0 )
                        continue;
                    value = mapValue;
                }
                // Other attributes are processed by default
                else {
                    value = getConvertUtils().convert(value);
                }
                map.put(name, value);
            }
        }

        // Update class
        String className = (String)map.get("class");
        map.put("class", getClassName(className.replace("class ", "")));

        return (map);

    }

    /** Instance */
    private static BeanUtils instance = null;

    /**
     * Get single instance
     *
     * @return instance
     */
    public static BeanUtils getInstance() {
        if ( instance == null ) {
            instance = new BeanUtils();
        }
        return instance;
    }
}
