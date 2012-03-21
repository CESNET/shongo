package cz.cesnet.shongo.common;

import cz.cesnet.shongo.Fault;
import cz.cesnet.shongo.FaultException;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.MethodUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
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
