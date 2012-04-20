package cz.cesnet.shongo.common.xmlrpc;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

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
        // Set convert utils that are able to convert String to Enum
        super(new ConvertUtilsBean()
        {
            @Override
            public Object convert(String value, Class clazz)
            {
                if (clazz.isEnum()) {
                    try {
                        return Enum.valueOf(clazz, value);
                    }
                    catch (IllegalArgumentException exception) {
                        throw new RuntimeException(
                                new FaultException(Fault.Common.EnumNotDefined, value, getShortClassName(
                                        clazz.getCanonicalName()))
                        );
                    }
                }
                else if (AtomicType.class.isAssignableFrom(clazz)) {
                    AtomicType atomicType = null;
                    try {
                        atomicType = (AtomicType) clazz.newInstance();
                    }
                    catch (java.lang.Exception exception) {
                        throw new RuntimeException(new FaultException(Fault.Common.ClassCannotBeInstanced,
                                BeanUtils.getShortClassName(clazz.getCanonicalName())));
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
     * Get short class name from full class name
     *
     * @param fullClassName
     * @return full class name
     */
    public static String getShortClassName(String fullClassName)
    {
        int position = fullClassName.lastIndexOf(".");
        if (position != -1) {
            return fullClassName.substring(position + 1, fullClassName.length());
        }
        return fullClassName;
    }

    /**
     * List of packages
     */
    static String[] packages;

    /**
     * Get full class name from short class name
     *
     * @param shortClassName
     * @return short class name
     */
    public static String getFullClassName(String shortClassName)
    {
        if (packages == null) {
            ArrayList<String> list = new ArrayList<String>();
            for (Package item : Package.getPackages()) {
                String name = item.getName();
                if (name.startsWith("cz.cesnet.shongo.") && name.endsWith(".api")) {
                    list.add(name);
                }
            }
            packages = list.toArray(new String[list.size()]);
        }

        for (String item : packages) {
            try {
                Class clazz = Class.forName(item + "." + shortClassName);
                return clazz.getCanonicalName();
            }
            catch (ClassNotFoundException exception) {
            }
        }
        return "cz.cesnet.shongo." + shortClassName;
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

            System.out.printf("%s '%s'\n", name, value);
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
                else if (value instanceof StructType) {
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

        // Update class
        String className = (String) map.get("class");
        map.put("class", getShortClassName(className.replace("class ", "")));

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
