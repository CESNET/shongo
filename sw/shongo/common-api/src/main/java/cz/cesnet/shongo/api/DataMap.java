package cz.cesnet.shongo.api;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.TodoImplementException;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.joda.time.ReadablePartial;

import java.util.*;

/**
 * Object from/to which the {@link ComplexType} can be (de-)serialized.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class DataMap
{
    private final ComplexType complexType;

    private final Map<String, Object> data;

    public DataMap(ComplexType complexType)
    {
        this.complexType = complexType;
        this.data = new HashMap<String, Object>();
    }

    public DataMap(ComplexType complexType, Map<String, Object> data)
    {
        this.complexType = complexType;
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

    public void set(String property, boolean value)
    {
        data.put(property, value);
    }

    public void set(String property, Boolean value)
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
        data.put(property, Converter.convertEnumToString(enumValue));
    }

    public void set(String property, DateTime dateTime)
    {
        data.put(property, Converter.convertDateTimeToString(dateTime));
    }

    public void set(String property, Period period)
    {
        data.put(property, Converter.convertPeriodToString(period));
    }

    public void set(String property, Interval interval)
    {
        data.put(property, Converter.convertIntervalToString(interval));
    }

    public void set(String property, ReadablePartial readablePartial)
    {
        data.put(property, Converter.convertReadablePartialToString(readablePartial));
    }

    public void set(String property, Collection collection)
    {
        data.put(property, collection);
    }

    public void set(String property, ComplexType complexType)
    {
        data.put(property, Converter.convertComplexTypeToMap(complexType));
    }

    public void set(String property, AtomicType atomicType)
    {
        data.put(property, Converter.convertAtomicTypeToString(atomicType));
    }

    private Object getRequired(String property)
    {
        Object value = data.get(property);
        if (value == null) {
            throw new CommonReportSet.ClassAttributeRequiredException(complexType.getClassName(), property);
        }
        return value;
    }

    public String getString(String property)
    {
        return Converter.convertToString(data.get(property));
    }

    public String getStringRequired(String property)
    {
        return Converter.convertToString(getRequired(property));
    }

    public boolean getBool(String property)
    {
        return Converter.convertToBoolean(getRequired(property));
    }

    public Boolean getBoolean(String property)
    {
        return Converter.convertToBoolean(data.get(property));
    }

    public int getInt(String property)
    {
        return Converter.convertToInteger(getRequired(property));
    }

    public Integer getInteger(String property)
    {
        return Converter.convertToInteger(data.get(property));
    }

    public Integer getIntegerRequired(String property)
    {
        return Converter.convertToInteger(getRequired(property));
    }

    public <E extends Enum<E>> E getEnum(String property, Class<E> enumClass)
    {
        return Converter.convertToEnum(data.get(property), enumClass);
    }

    public <E extends Enum<E>> E getEnumRequired(String property, Class<E> enumClass)
    {
        return Converter.convertToEnum(getRequired(property), enumClass);
    }

    public DateTime getDateTime(String property)
    {
        return Converter.convertToDateTime(data.get(property));
    }

    public DateTime getDateTimeRequired(String property)
    {
        return Converter.convertToDateTime(getRequired(property));
    }

    public Period getPeriod(String property)
    {
        return Converter.convertToPeriod(data.get(property));
    }

    public Period getPeriodRequired(String property)
    {
        return Converter.convertToPeriod(getRequired(property));
    }

    public Interval getInterval(String property)
    {
        return Converter.convertToInterval(data.get(property));
    }

    public Interval getIntervalRequired(String property)
    {
        return Converter.convertToInterval(getRequired(property));
    }

    public ReadablePartial getReadablePartial(String property)
    {
        return Converter.convertToReadablePartial(data.get(property));
    }

    public <T> List<T> getList(String property, Class<T> componentClass)
    {
        return Converter.convertToList(data.get(property), componentClass);
    }

    public <T> List<T> getListRequired(String property, Class<T> componentClass)
    {
        List<T> value = Converter.convertToList(getRequired(property), componentClass);
        if (value.size() == 0) {
            throw new CommonReportSet.ClassCollectionRequiredException(complexType.getClassName(), property);
        }
        return value;
    }

    public List<Object> getList(String property, Class... componentClasses)
    {
        return Converter.convertToList(data.get(property), componentClasses);
    }

    public <T> Set<T> getSet(String property, Class<T> componentClass)
    {
        return Converter.convertToSet(data.get(property), componentClass);
    }

    public <T> Set<T> getSetRequired(String property, Class<T> componentClass)
    {
        Set<T> value = Converter.convertToSet(getRequired(property), componentClass);
        if (value.size() == 0) {
            throw new CommonReportSet.ClassCollectionRequiredException(complexType.getClassName(), property);
        }
        return value;
    }



    public Map getMap(String property)
    {
        return (Map) data.get(property);
    }
    public Map getMapRequired(String property)
    {
        return (Map) getRequired(property);
    }

    public <T extends AtomicType> T getAtomicType(String property, Class<T> atomicTypeClass)
    {
        String value = getString(property);
        return Converter.convertStringToAtomicType(value, atomicTypeClass);
    }

    public <T extends ComplexType> T getComplexType(String property, Class<T> complexTypeClass)
    {
        Object value = data.get(property);
        if (value == null) {
            return null;
        }
        else if (value instanceof Map) {
            return Converter.convertMapToComplexType((Map) value, complexTypeClass);
        }
        else if (complexTypeClass.isInstance(value)) {
            return complexTypeClass.cast(value);
        }
        else {
            throw new CommonReportSet.ClassAttributeTypeMismatchException(complexType.getClassName(), property,
                    ClassHelper.getClassShortName(complexTypeClass), ClassHelper.getClassShortName(value.getClass()));
        }
    }

    public <T extends ComplexType> T getComplexTypeRequired(String property, Class<T> complexTypeClass)
    {
        Object value = getRequired(property);
        if (value instanceof Map) {
            return Converter.convertMapToComplexType((Map) value, complexTypeClass);
        }
        else if (complexTypeClass.isInstance(value)) {
            return complexTypeClass.cast(value);
        }
        else {
            throw new CommonReportSet.ClassAttributeTypeMismatchException(complexType.getClassName(), property,
                    ClassHelper.getClassShortName(complexTypeClass), ClassHelper.getClassShortName(value.getClass()));
        }
    }
}
