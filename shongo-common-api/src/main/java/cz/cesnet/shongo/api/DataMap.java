package cz.cesnet.shongo.api;

import com.fasterxml.jackson.databind.JsonNode;
import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.TodoImplementException;
import org.joda.time.*;

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

    private void setNotNull(String property, Object value)
    {
        if (value == null) {
            return;
        }
        data.put(property, value);
    }

    public void set(String property, String value)
    {
        setNotNull(property, value);
    }

    public void set(String property, Class value)
    {
        setNotNull(property, Converter.convertClassToString(value));
    }

    public void set(String property, boolean value)
    {
        setNotNull(property, value);
    }

    public void set(String property, Boolean value)
    {
        setNotNull(property, value);
    }

    public void set(String property, int value)
    {
        setNotNull(property, value);
    }

    public void set(String property, Integer value)
    {
        setNotNull(property, value);
    }

    public void set(String property, long value)
    {
        setNotNull(property, value);
    }

    public void set(String property, Long value)
    {
        setNotNull(property, value);
    }

    public void set(String property, byte[] value)
    {
        setNotNull(property, value);
    }

    public <E extends Enum> void set(String property, E enumValue)
    {
        setNotNull(property, Converter.convertEnumToString(enumValue));
    }

    public void set(String property, Locale locale)
    {
        setNotNull(property, Converter.convertLocaleToString(locale));
    }

    public void set(String property, DateTime dateTime)
    {
        setNotNull(property, Converter.convertDateTimeToString(dateTime));
    }

    public void set(String property, DateTimeZone dateTimeZone)
    {
        setNotNull(property, Converter.convertDateTimeZoneToString(dateTimeZone));
    }

    public void set(String property, Period period)
    {
        setNotNull(property, Converter.convertPeriodToString(period));
    }

    public void set(String property, Duration duration)
    {
        setNotNull(property, (duration != null ? duration.toPeriod() : null));
    }

    public void set(String property, Interval interval)
    {
        setNotNull(property, Converter.convertIntervalToString(interval));
    }

    public void set(String property, ReadablePartial readablePartial)
    {
        setNotNull(property, Converter.convertReadablePartialToString(readablePartial));
    }

    public void set(String property, JsonNode jsonNode)
    {
        setNotNull(property, Converter.convertJsonNodeToString(jsonNode));
    }

    public void set(String property, Collection collection)
    {
        setNotNull(property, collection);
    }

    public void set(String property, Map map)
    {
        setNotNull(property, map);
    }

    public void set(String property, ComplexType complexType)
    {
        setNotNull(property, Converter.convertComplexTypeToMap(complexType));
    }

    public void set(String property, AtomicType atomicType)
    {
        setNotNull(property, Converter.convertAtomicTypeToString(atomicType));
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

    public String getString(String property, int maximumLength)
    {
        String value = Converter.convertToString(data.get(property));
        try {
            return Converter.checkMaximumStringLength(value, maximumLength);
        }
        catch (CommonReportSet.ValueMaximumLengthExceededException exception) {
            throw new CommonReportSet.ClassAttributeValueMaximumLengthExceededException(
                    getString("class"), property, maximumLength);
        }
    }

    public String getStringRequired(String property)
    {
        return Converter.convertToString(getRequired(property));
    }

    public String getStringRequired(String property, int maximumLength)
    {
        String value = Converter.convertToString(getRequired(property));
        try {
            return Converter.checkMaximumStringLength(value, maximumLength);
        }
        catch (CommonReportSet.ValueMaximumLengthExceededException exception) {
            throw new CommonReportSet.ClassAttributeValueMaximumLengthExceededException(
                    getString("class"), property, maximumLength);
        }
    }

    public <T> Class<? extends T> getClass(String property, Class<T> baseClass)
    {
        Object value = data.get(property);
        if (value == null) {
            return null;
        }
        return Converter.convertToClass(value, baseClass);
    }

    public <T> Set<Class<? extends T>> getClassSet(String property, Class<T> baseClass)
    {
        Object value = data.get(property);
        if (value == null) {
            return new HashSet<Class<? extends T>>();
        }
        Set<Class<? extends T>> classSet = new HashSet<Class<? extends T>>();
        for (Object item : Converter.convertToSet(value, Object.class)) {
            classSet.add(Converter.convertToClass(item, baseClass));
        }
        return classSet;
    }

    public boolean getBool(String property)
    {
        Object value = data.get(property);
        if (value == null) {
            return false;
        }
        return Converter.convertToBoolean(value);
    }

    public Boolean getBoolean(String property)
    {
        return Converter.convertToBoolean(data.get(property));
    }

    public int getInt(String property)
    {
        return Converter.convertToInteger(getRequired(property));
    }

    public int getInt(String property, int defaultValue)
    {
        Object value = data.get(property);
        if (value == null) {
            return defaultValue;
        }
        return Converter.convertToInteger(value);
    }

    public Integer getInteger(String property)
    {
        return Converter.convertToInteger(data.get(property));
    }

    public long getLongPrimitive(String property)
    {
        return Converter.convertToLong(getRequired(property));
    }

    public Long getLong(String property)
    {
        return Converter.convertToLong(data.get(property));
    }

    public byte[] getByteArray(String property)
    {
        return Converter.convertToByteArray(data.get(property));
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

    public Locale getLocale(String property)
    {
        return Converter.convertToLocale(data.get(property));
    }

    public DateTime getDateTime(String property)
    {
        return Converter.convertToDateTime(data.get(property));
    }

    public DateTime getDateTimeRequired(String property)
    {
        return Converter.convertToDateTime(getRequired(property));
    }

    public DateTimeZone getDateTimeZone(String property)
    {
        return Converter.convertToDateTimeZone(data.get(property));
    }

    public Period getPeriod(String property)
    {
        return Converter.convertToPeriod(data.get(property));
    }

    public Period getPeriodRequired(String property)
    {
        return Converter.convertToPeriod(getRequired(property));
    }

    public Duration getDuration(String property)
    {
        Period period = Converter.convertToPeriod(data.get(property));
        if (period == null) {
            return null;
        }
        return period.toStandardDuration();
    }

    public Duration getDurationRequired(String property)
    {
        Period period = Converter.convertToPeriod(getRequired(property));
        if (period == null) {
            return null;
        }
        return period.toStandardDuration();
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

    public JsonNode getJsonNode(String property)
    {
        return Converter.convertToJsonNode(getString(property));
    }

    public <T> List<T> getList(String property, Class<T> componentClass)
    {
        return Converter.convertToList(data.get(property), componentClass);
    }

    public List<String> getStringList(String property, int maximumLength)
    {
        List<String> stringList = Converter.convertToList(data.get(property), String.class);
        try {
            return Converter.checkMaximumStringLength(stringList, maximumLength);
        }
        catch (CommonReportSet.ValueMaximumLengthExceededException exception) {
            throw new CommonReportSet.ClassAttributeValueMaximumLengthExceededException(
                    getString("class"), property, maximumLength);
        }
    }

    public <T> List<T> getListRequired(String property, Class<T> componentClass)
    {
        List<T> value = Converter.convertToList(getRequired(property), componentClass);
        if (value.size() == 0) {
            throw new CommonReportSet.ClassCollectionRequiredException(complexType.getClassName(), property);
        }
        return value;
    }

    public List<String> getStringListRequired(String property, int maximumLength)
    {
        List<String> stringList = Converter.convertToList(getRequired(property), String.class);
        if (stringList.size() == 0) {
            throw new CommonReportSet.ClassCollectionRequiredException(complexType.getClassName(), property);
        }
        try {
            return Converter.checkMaximumStringLength(stringList, maximumLength);
        }
        catch (CommonReportSet.ValueMaximumLengthExceededException exception) {
            throw new CommonReportSet.ClassAttributeValueMaximumLengthExceededException(
                    getString("class"), property, maximumLength);
        }
    }

    public List<Object> getList(String property, Class... componentClasses)
    {
        return Converter.convertToList(data.get(property), componentClasses);
    }

    public List<Object> getListRequired(String property, Class... componentClasses)
    {
        List<Object> value = Converter.convertToList(getRequired(property), componentClasses);
        if (value.size() == 0) {
            throw new CommonReportSet.ClassCollectionRequiredException(complexType.getClassName(), property);
        }
        return value;
    }

    public <T> Set<T> getSet(String property, Class<T> componentClass)
    {
        return Converter.convertToSet(data.get(property), componentClass);
    }

    public Set<String> getStringSet(String property, int maximumLength)
    {
        Set<String> stringSet = Converter.convertToSet(data.get(property), String.class);
        try {
            return Converter.checkMaximumStringLength(stringSet, maximumLength);
        }
        catch (CommonReportSet.ValueMaximumLengthExceededException exception) {
            throw new CommonReportSet.ClassAttributeValueMaximumLengthExceededException(
                    getString("class"), property, maximumLength);
        }
    }

    public <T> Set<T> getSetRequired(String property, Class<T> componentClass)
    {
        Set<T> value = Converter.convertToSet(getRequired(property), componentClass);
        if (value.size() == 0) {
            throw new CommonReportSet.ClassCollectionRequiredException(complexType.getClassName(), property);
        }
        return value;
    }

    public Set<String> getStringSetRequired(String property, int maximumLength)
    {
        Set<String> stringSet = Converter.convertToSet(getRequired(property), String.class);
        if (stringSet.size() == 0) {
            throw new CommonReportSet.ClassCollectionRequiredException(complexType.getClassName(), property);
        }
        try {
            return Converter.checkMaximumStringLength(stringSet, maximumLength);
        }
        catch (CommonReportSet.ValueMaximumLengthExceededException exception) {
            throw new CommonReportSet.ClassAttributeValueMaximumLengthExceededException(
                    getString("class"), property, maximumLength);
        }
    }

    public <K, V> Map<K, V> getMap(String property, Class<K> keyClass, Class<V> valueClass)
    {
        return Converter.convertToMap(data.get(property), keyClass, valueClass);
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

    public Object getVariant(String property, Class... requiredClasses)
    {
        return Converter.convert(data.get(property), requiredClasses);
    }

    public Object getVariantRequired(String property, Class... requiredClasses)
    {
        return Converter.convert(getRequired(property), requiredClasses);
    }

    @Override
    public String toString()
    {
        return data.toString();
    }


}
