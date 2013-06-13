package cz.cesnet.shongo.map;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.TodoImplementException;
import cz.cesnet.shongo.api.util.ClassHelper;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static cz.cesnet.shongo.api.util.ClassHelper.getClassShortName;

/**
 * TODO: refactorize API
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DataMap
{
    /*private Map<String, Object> data;

    public DataMap()
    {
        data = new HashMap<String, Object>();
    }

    public DataMap(Map<String, Object> data)
    {
        this.data = data;
    }

    public Map<String, Object> getData()
    {
        return data;
    }

    public void set(String property, String value)
    {
        data.put(property, value);
    }

    public void set(String property, int value)
    {
        data.put(property, value);
    }

    public void set(String property, Integer value)
    {
        data.put(property, value);
    }

    public <E extends Enum> void set(String property, E enumValue)
    {
        data.put(property, enumValue.toString());
    }

    public void set(String property, DateTime dateTime)
    {
        data.put(property, dateTime.toString());
    }

    public void set(String property, AbstractObject abstractObject)
    {
        data.put(property, abstractObject);
    }

    public void set(String property, Collection collection)
    {
        data.put(property, collection);
    }

    public String getString(String property)
    {
        return (String) data.get(property);
    }

    public int getInt(String property)
    {
        return (Integer) data.get(property);
    }

    public Integer getInteger(String property)
    {
        return (Integer) data.get(property);
    }

    public DateTime getDateTime(String property)
    {
        return Converter.convert(data.get(property), DateTime.class);
    }

    public <E extends Enum<E>> E getEnum(String property, Class<E> enumClass)
    {
        return Converter.convert(data.get(property), enumClass);
    }

    public <T extends AbstractObject> T getObject(String property, Class<T> objectType)
    {
        return Converter.convert(data.get(property), objectType);
    }

    public <T> Collection<T> getCollection(String property, Class<T> itemClass)
    {
        Object value = data.get(property);
        if (value instanceof Object[]) {
            Collection<T> collection = new LinkedList<T>();
            for (Object item : (Object[])value) {
                collection.add(Converter.convert(item, itemClass));
            }
            return collection;
        }
        else {
            throw new TodoImplementException(value.getClass().getName());
        }
    }

    public static class Converter
    {
        private static final long DATETIME_INFINITY_START_MILLIS = Temporal.DATETIME_INFINITY_START.getMillis();

        private static final long DATETIME_INFINITY_END_MILLIS = Temporal.DATETIME_INFINITY_END.getMillis();

        @SuppressWarnings("unchecked")
        public static <T> T convert(Object value, Class<T> targetClass)
        {
            if (AbstractObject.class.isAssignableFrom(targetClass)) {
                Class<? extends AbstractObject> abstractObjectClass = (Class<? extends AbstractObject>) targetClass;
                if (value == null) {
                    return null;
                }
                else if (value instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) value;
                    return (T) convert(map, abstractObjectClass);
                }
            }
            throw new TodoImplementException(value.getClass().getName() + " -> " + targetClass.getName());
        }

        public static <E extends Enum<E>> E convert(Object value, Class<E> enumClass)
        {
            if (enumClass.isInstance(value)) {
                return enumClass.cast(value);
            }
            else {
                return convert(value.toString(), enumClass);
            }
        }

        public static <E extends Enum<E>> E convert(String value, Class<E> enumClass)
        {
            try {
                return Enum.valueOf(enumClass, value);
            }
            catch (IllegalArgumentException exception) {
                throw new CommonReportSet.TypeIllegalValueException(getClassShortName(enumClass), value);
            }
        }

        public static DateTime convert(Object value, Class<DateTime> dateTimeClass)
        {
            if (dateTimeClass.isInstance(value)) {
                return dateTimeClass.cast(value);
            }
            else {
                return convert(value.toString(), dateTimeClass);
            }
        }

        public static DateTime convert(String value, Class<DateTime> dateTimeClass)
        {
            org.joda.time.DateTime dateTime;
            try {
                dateTime = ISODateTimeFormat.dateTimeParser().parseDateTime(value);
            }
            catch (Exception exception) {
                throw new CommonReportSet.TypeIllegalValueException(org.joda.time.DateTime.class.getSimpleName(), value);
            }
            final long millis = dateTime.getMillis();
            if (millis < DATETIME_INFINITY_START_MILLIS || millis > DATETIME_INFINITY_END_MILLIS) {
                throw new CommonReportSet.TypeIllegalValueException(org.joda.time.DateTime.class.getSimpleName(), value);
            }
            return dateTime;
        }

        public static <T extends AbstractObject> T convert(Map<String, Object> map, Class<T> targetClass)
        {

            Class newValueClass = targetClass;
            if (map.containsKey(AbstractObject.CLASS_PROPERTY)) {
                String className = (String) map.get(AbstractObject.CLASS_PROPERTY);
                try {
                    newValueClass = Class.forName("cz.cesnet.shongo.controller.api.map." + className);
                }
                catch (ClassNotFoundException exception) {
                    throw new CommonReportSet.ClassUndefinedException(className);
                }
            }
            AbstractObject abstractObject = (AbstractObject) ClassHelper.createInstanceFromClass(newValueClass);
            abstractObject.fromData(new DataMap(map));
            if (!targetClass.isInstance(abstractObject)) {
                throw new CommonReportSet.TypeMismatchException(
                        targetClass.getSimpleName(), newValueClass.getSimpleName());
            }
            @SuppressWarnings("unchecked")
            T targetValue = (T) abstractObject;
            return targetValue;
        }
    }*/
}
