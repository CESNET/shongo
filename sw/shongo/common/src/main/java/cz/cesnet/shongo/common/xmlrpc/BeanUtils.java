package cz.cesnet.shongo.common.xmlrpc;

import cz.cesnet.shongo.common.api.AtomicType;
import cz.cesnet.shongo.common.api.Fault;
import cz.cesnet.shongo.common.api.FaultException;
import cz.cesnet.shongo.common.api.ComplexType;
import cz.cesnet.shongo.common.util.Converter;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Bean utils that are able to populate a bean from a Map and
 * to get Map from a given Bean. It also provides a special
 * implementation of ConvertUtils bean which is able to
 * perform type conversion between strings, enums and custom
 * XML-RPC atomic types.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class BeanUtils extends BeanUtilsBean
{
    private static Logger logger = LoggerFactory.getLogger(BeanUtils.class);

    /**
     * Constructor
     */
    public BeanUtils()
    {
        // Set convert utils that are able to convert String to Enum and AtomicType
        super(new ConvertUtilsBean()
        {
            @Override
            public Object convert(String value, Class clazz)
            {
                if (clazz.isEnum()) {
                    try {
                        return Converter.stringToEnum(value, clazz);
                    }
                    catch (FaultException exception) {
                        throw new RuntimeException(exception);
                    }
                }
                else if (AtomicType.class.isAssignableFrom(clazz)) {
                    AtomicType atomicType = null;
                    try {
                        atomicType = (AtomicType) clazz.newInstance();
                    }
                    catch (java.lang.Exception exception) {
                        throw new RuntimeException(new FaultException(Fault.Common.CLASS_CANNOT_BE_INSTANCED,
                                Converter.getShortClassName(clazz.getCanonicalName())));
                    }
                    atomicType.fromString(value);
                    return atomicType;
                }
                else {
                    return super.convert(value, clazz);
                }
            }
        });
    }

    /**
     * Populate bean from map
     *
     * @param bean
     * @param properties
     * @throws IllegalAccessException
     * @throws java.lang.reflect.InvocationTargetException
     *
     */
    public void populateRecursive(Object bean, Map properties)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
    {
        // Do nothing unless both arguments have been specified
        if ((bean == null) || (properties == null)) {
            return;
        }

        // Loop through the property name/value pairs to be set
        Iterator entries = properties.entrySet().iterator();
        while (entries.hasNext()) {
            // Identify the property name and value(s) to be assigned
            Map.Entry entry = (Map.Entry) entries.next();
            String name = (String) entry.getKey();
            if (name == null || name.equals("class")) {
                continue;
            }
            Object value = entry.getValue();

            PropertyDescriptor propertyDescriptor = getPropertyUtils().getPropertyDescriptor(bean, name);
            if (propertyDescriptor != null) {
                Class propertyClass = propertyDescriptor.getPropertyType();
                // Convert object array to concrete type of array
                if (propertyClass.isArray() && value instanceof Object[]) {
                    Object[] valueArray = (Object[]) value;
                    try {
                        value = Arrays.copyOf(valueArray, valueArray.length, propertyClass);
                    }
                    catch (ClassCastException exception) {
                        logger.error("Failed to copy array.", exception);
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
    public Map describeRecursive(Object bean)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
    {
        if (bean == null || bean instanceof DynaBean) {
            return super.describe(bean);
        }

        Map map = new HashMap();
        PropertyDescriptor[] descriptors = getPropertyUtils().getPropertyDescriptors(bean);
        Class clazz = bean.getClass();
        for (int i = 0; i < descriptors.length; i++) {
            String name = descriptors[i].getName();
            if (MethodUtils.getAccessibleMethod(clazz, descriptors[i].getReadMethod()) != null) {
                Object value = getPropertyUtils().getNestedProperty(bean, name);
                // Skip null values
                if (value == null) {
                    continue;
                }
                // Type attribute must be recursively described
                else if (value instanceof ComplexType) {
                    value = describeRecursive(value);
                }
                // Array is passed only when not empty
                else if (value instanceof Object[]) {
                    Object[] arrayValue = (Object[]) value;
                    if (arrayValue.length == 0) {
                        continue;
                    }
                    value = arrayValue;
                }
                // Map is passed only when not empty
                else if (value instanceof Map) {
                    Map mapValue = (Map) value;
                    if (map.size() == 0) {
                        continue;
                    }
                    value = mapValue;
                }
                // Other attributes are processed by default
                else {
                    value = getConvertUtils().convert(value);
                }
                map.put(name, value);
            }
        }



        return (map);

    }

    /**
     * Instance
     */
    private static BeanUtils instance = null;

    /**
     * Get single instance
     *
     * @return instance
     */
    public static BeanUtils getInstance()
    {
        if (instance == null) {
            instance = new BeanUtils();
        }
        return instance;
    }
}
