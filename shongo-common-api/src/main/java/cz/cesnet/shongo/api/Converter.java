package cz.cesnet.shongo.api;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.TodoImplementException;
import org.joda.time.*;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TODO:
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Converter
{
    public static final int ENUM_VALUE_MAXIMUM_LENGTH = 64;
    public static final int READABLE_PARTIAL_MAXIMUM_LENGTH = 32;
    public static final int LOCAL_DATE_MAXIMUM_LENGTH = 10;
    public static final int PERIOD_MAXIMUM_LENGTH = 64;
    public static final int LOCALE_MAXIMUM_LENGTH = 16;
    public static final int DATE_TIME_ZONE_MAXIMUM_LENGTH = 32;

    private static Logger logger = LoggerFactory.getLogger(Converter.class);

    private static final long DATETIME_INFINITY_START_MILLIS = Temporal.DATETIME_INFINITY_START.getMillis();

    private static final long DATETIME_INFINITY_END_MILLIS = Temporal.DATETIME_INFINITY_END.getMillis();

    private static final DateTimeFormatter DATE_TIME_FORMATTER = ISODateTimeFormat.dateTimeParser();

    /**
     * Convert given {@code value} to {@link String}.
     *
     * @param value
     * @return converted {@link String} value
     */
    public static String convertToString(Object value)
    {
        if (value == null) {
            return null;
        }
        else if (value instanceof String) {
            return (String) value;
        }
        else if (value.getClass().isPrimitive() || value instanceof Integer || value instanceof Long ) {
            return value.toString();
        }
        else if (value instanceof Enum ) {
            return convertEnumToString((Enum) value);
        }
        else if (value instanceof DateTime ) {
            return convertDateTimeToString((DateTime) value);
        }
        else if (value instanceof Period ) {
            return convertPeriodToString((Period) value);
        }
        else if (value instanceof Interval ) {
            return convertIntervalToString((Interval) value);
        }
        else {
            throw new TodoImplementException(value.getClass());
        }
    }

    /**
     * Convert given {@code value} to {@link Boolean}.
     *
     * @param value
     * @return converted {@link Boolean} value
     */
    public static Boolean convertToBoolean(Object value)
    {
        if (value == null) {
            return  null;
        }
        else if (value instanceof Boolean) {
            return (Boolean) value;
        }
        else if (value instanceof Integer) {
            return ((Integer) value) != 0;
        }
        throw new TodoImplementException(value.getClass());
    }

    /**
     * Convert given {@code value} to {@link Integer}.
     *
     * @param value
     * @return converted {@link Integer} value
     */
    public static Integer convertToInteger(Object value)
    {
        if (value == null) {
            return  null;
        }
        else if (value instanceof Integer) {
            return (Integer) value;
        }
        throw new TodoImplementException(value.getClass());
    }

    /**
     * Convert given {@code value} to {@link Long}.
     *
     * @param value
     * @return converted {@link Long} value
     */
    public static Long convertToLong(Object value)
    {
        if (value == null) {
            return  null;
        }
        else if (value instanceof Long) {
            return (Long) value;
        }
        throw new TodoImplementException(value.getClass());
    }

    /**
     * Convert given {@code value} to {@link byte[]}.
     *
     * @param value
     * @return converted {@link byte[]} value
     */
    public static byte[] convertToByteArray(Object value)
    {
        if (value == null) {
            return  null;
        }
        else if (value instanceof byte[]) {
            return (byte[]) value;
        }
        throw new TodoImplementException(value.getClass());
    }

    /**
     * Convert given {@link Class} {@code value} to {@link String}.
     *
     * @param value
     * @return converted {@link String} value
     */
    public static String convertClassToString(Class value)
    {
        if (value == null) {
            return null;
        }
        return ClassHelper.getClassShortName(value);
    }

    /**
     * Convert given {@code value} to {@link Class}.
     *
     * @param value
     * @return converted {@link Enum} value
     */
    public static Class convertToClass(Object value)
    {
        if (value == null) {
            return null;
        }
        else if (value instanceof String) {
            String className = (String) value;
            return convertStringToClass(className, Object.class);
        }
        else {
            throw new TodoImplementException(value.getClass());
        }
    }

    /**
     * Convert given {@code value} to {@link Class}.
     *
     * @param value
     * @param baseClass
     * @return converted {@link Enum} value
     */
    public static <T> Class<? extends T> convertToClass(Object value, Class<T> baseClass)
    {
        if (value == null) {
            return null;
        }
        else if (value instanceof String) {
            String className = (String) value;
            return convertStringToClass(className, baseClass);
        }
        else {
            throw new TodoImplementException(value.getClass());
        }
    }

    /**
     * Convert given {@link String} {@code value} to {@link Class} value of given {@code baseClass}.
     *
     * @param value
     * @param baseClass
     * @return converted {@link Enum} value
     */
    public static <T> Class<? extends T> convertStringToClass(String value, Class<T> baseClass)
    {
        try {
            return (Class<? extends T>) ClassHelper.getClassFromShortName(value);
        }
        catch (ClassNotFoundException e) {
            throw new CommonReportSet.ClassUndefinedException(value);
        }
    }

    /**
     * Convert given {@link Enum} {@code value} to {@link String}.
     *
     * @param value
     * @return converted {@link String} value
     */
    public static String convertEnumToString(Enum value)
    {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * Convert given {@code value} to {@link Enum} value of given {@code enumClass}.
     *
     * @param value
     * @param enumClass
     * @return converted {@link Enum} value
     */
    public static <E extends Enum<E>> E convertToEnum(Object value, Class<E> enumClass)
    {
        if (value == null) {
            return null;
        }
        else if (enumClass.isInstance(value)) {
            return enumClass.cast(value);
        }
        else {
            String stringValue = checkMaximumStringLength(value.toString(), ENUM_VALUE_MAXIMUM_LENGTH);
            return convertStringToEnum(stringValue, enumClass);
        }
    }

    /**
     * Convert given {@link String} {@code value} to {@link Enum} value of given {@code enumClass}.
     *
     * @param value
     * @param enumClass
     * @return converted {@link Enum} value
     */
    public static <E extends Enum<E>> E convertStringToEnum(String value, Class<E> enumClass)
    {
        try {
            return Enum.valueOf(enumClass, value);
        }
        catch (IllegalArgumentException exception) {
            throw new CommonReportSet.TypeIllegalValueException(ClassHelper.getClassShortName(enumClass), value);
        }
    }

    /**
     * Convert given {@link Locale} {@code value} to {@link String}.
     *
     * @param value
     * @return converted {@link String} value
     */
    public static String convertLocaleToString(Locale value)
    {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * Convert given {@code value} to {@link Locale} value.
     *
     * @param value
     * @return converted {@link Locale} value
     */
    public static Locale convertToLocale(Object value)
    {
        if (value == null) {
            return null;
        }
        else if (value instanceof Locale) {
            return (Locale) value;
        }
        else {
            String stringValue = checkMaximumStringLength(value.toString(), LOCALE_MAXIMUM_LENGTH);
            return convertStringToLocale(stringValue);
        }
    }

    /**
     * Convert given {@link String} {@code value} to {@link Locale} value.
     *
     * @param value
     * @return converted {@link Locale} value
     */
    public static Locale convertStringToLocale(String value)
    {
        String[] parts = value.split("_");
        switch (parts.length) {
            case 1:
                return new Locale(parts[0]);
            case 2:
                return new Locale(parts[0], parts[1]);
            case 3:
                return new Locale(parts[0], parts[1], parts[2]);
            default:
                throw new CommonReportSet.TypeIllegalValueException(Locale.class.getSimpleName(), value);
        }
    }

    /**
     * Convert given {@link DateTime} {@code value} to {@link String}.
     *
     * @param value
     * @return converted {@link String} value
     */
    public static String convertDateTimeToString(DateTime value)
    {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * Convert given {@link LocalDate} {@code value} to {@link String}.
     *
     * @param value
     * @return converted {@link String} value
     */
    public static String convertLocalDateToString(LocalDate value)
    {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * Convert given {@code value} to {@link DateTime} value.
     *
     * @param value
     * @return converted {@link DateTime} value
     */
    public static DateTime convertToDateTime(Object value)
    {
        if (value == null) {
            return null;
        }
        else if (value instanceof DateTime) {
            return (DateTime) value;
        }
        else {
            return convertStringToDateTime(value.toString());
        }
    }

    /**
     * Convert given {@link String} {@code value} to {@link DateTime} value.
     *
     * @param value
     * @return converted {@link DateTime} value
     */
    public static DateTime convertStringToDateTime(String value)
    {
        DateTime dateTime;
        try {
            dateTime = DATE_TIME_FORMATTER.parseDateTime(value);
        }
        catch (Exception exception) {
            throw new CommonReportSet.TypeIllegalValueException(DateTime.class.getSimpleName(), value);
        }
        final long millis = dateTime.getMillis();
        if (millis < DATETIME_INFINITY_START_MILLIS || millis > DATETIME_INFINITY_END_MILLIS) {
            throw new CommonReportSet.TypeIllegalValueException(DateTime.class.getSimpleName(), value);
        }
        return dateTime;
    }

    /**
     * Convert given {@link String} {@code value} to {@link LocalDate} value.
     *
     * @param value
     * @return converted {@link LocalDate} value
     */
    public static LocalDate convertStringToLocalDate(String value)
    {
        LocalDate localDate;
        try {
            localDate = DATE_TIME_FORMATTER.parseLocalDate(value);
        }
        catch (Exception exception) {
            throw new CommonReportSet.TypeIllegalValueException(LocalDate.class.getSimpleName(), value);
        }
        final long millis = localDate.toDateTimeAtStartOfDay().getMillis();
        if (millis < DATETIME_INFINITY_START_MILLIS || millis > DATETIME_INFINITY_END_MILLIS) {
            throw new CommonReportSet.TypeIllegalValueException(LocalDate.class.getSimpleName(), value);
        }
        return localDate;
    }

    /**
     * Convert given {@link DateTimeZone} {@code value} to {@link String}.
     *
     * @param value
     * @return converted {@link String} value
     */
    public static String convertDateTimeZoneToString(DateTimeZone value)
    {
        if (value == null) {
            return null;
        }
        return value.getID();
    }

    /**
     * Convert given {@code value} to {@link DateTimeZone} value.
     *
     * @param value
     * @return converted {@link DateTimeZone} value
     */
    public static DateTimeZone convertToDateTimeZone(Object value)
    {
        if (value == null) {
            return null;
        }
        else if (value instanceof DateTimeZone) {
            return (DateTimeZone) value;
        }
        else {
            String stringValue = checkMaximumStringLength(value.toString(), DATE_TIME_ZONE_MAXIMUM_LENGTH);
            return convertStringToDateTimeZone(stringValue);
        }
    }

    /**
     * Convert given {@link String} {@code value} to {@link DateTimeZone} value.
     *
     * @param value
     * @return converted {@link DateTimeZone} value
     */
    public static DateTimeZone convertStringToDateTimeZone(String value)
    {
        DateTimeZone dateTimeZone;
        try {
            dateTimeZone = DateTimeZone.forID(value);
        }
        catch (Exception exception) {
            throw new CommonReportSet.TypeIllegalValueException(DateTimeZone.class.getSimpleName(), value);
        }
        return dateTimeZone;
    }

    /**
     * Convert given {@link Period} {@code value} to {@link String}.
     *
     * @param value
     * @return converted {@link String} value
     */
    public static String convertPeriodToString(Period value)
    {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * Convert given {@code value} to {@link Period} value.
     *
     * @param value
     * @return converted {@link Period} value
     */
    public static Period convertToPeriod(Object value)
    {
        if (value == null) {
            return null;
        }
        else if (value instanceof Period) {
            return (Period) value;
        }
        else {
            String stringValue = checkMaximumStringLength(value.toString(), PERIOD_MAXIMUM_LENGTH);
            return convertStringToPeriod(stringValue);
        }
    }

    /**
     * Convert given {@link String} {@code value} to {@link Period} value.
     *
     * @param value
     * @return converted {@link Period} value
     */
    public static Period convertStringToPeriod(String value)
    {
        Period period;
        try {
            period = Period.parse(value);
        }
        catch (Exception exception) {
            throw new CommonReportSet.TypeIllegalValueException(Period.class.getSimpleName(), value);
        }
        return period;
    }

    /**
     * Convert given {@link Interval} {@code value} to {@link String}.
     *
     * @param value
     * @return converted {@link String} value
     */
    public static String convertIntervalToString(Interval value)
    {
        if (value == null) {
            return null;
        }
        String startString;
        String endString;
        if (value.getStartMillis() == DATETIME_INFINITY_START_MILLIS) {
            startString = Temporal.INFINITY_ALIAS;
        }
        else {
            startString = value.getStart().toString();
        }
        if (value.getEndMillis() == DATETIME_INFINITY_END_MILLIS) {
            endString = Temporal.INFINITY_ALIAS;
        }
        else {
            endString = value.getEnd().toString();
        }
        return String.format("%s/%s", startString, endString);
    }

    public static String convertIntervalToStringUTC(Interval value)
    {
        Interval intervalUTC = new Interval(value.getStartMillis(), value.getEndMillis());
        return convertIntervalToString(intervalUTC);
    }

    /**
     * Convert given {@code value} to {@link Interval} value.
     *
     * @param value
     * @return converted {@link Interval} value
     */
    public static Interval convertToInterval(Object value)
    {
        if (value == null) {
            return null;
        }
        else if (value instanceof Interval) {
            return (Interval) value;
        }
        else {
            return convertStringToInterval(value.toString());
        }
    }

    /**
     * Convert given {@link String} {@code value} to {@link Interval} value.
     *
     * @param value
     * @return converted {@link Interval} value
     */
    public static Interval convertStringToInterval(String value)
    {
        String[] parts = value.split("/");
        if (parts.length == 2) {
            String startString = parts[0];
            String endString = parts[1];
            DateTime start;
            DateTime end;
            if (startString.equals(Temporal.INFINITY_ALIAS)) {
                start = Temporal.DATETIME_INFINITY_START;
            }
            else {
                start = convertStringToDateTime(startString);
            }
            if (endString.equals(Temporal.INFINITY_ALIAS)) {
                end = Temporal.DATETIME_INFINITY_END;
            }
            else {
                end = convertStringToDateTime(endString);
            }
            try {
                return new Interval(start, end);
            }
            catch (IllegalArgumentException exception) {
                throw new CommonReportSet.TypeIllegalValueException(Interval.class.getSimpleName(), value);
            }
        }
        throw new CommonReportSet.TypeIllegalValueException(Interval.class.getSimpleName(), value);
    }

    /**
     * Convert given {@link ReadablePartial} {@code value} to {@link String}.
     *
     * @param value
     * @return converted {@link String} value
     */
    public static String convertReadablePartialToString(ReadablePartial value)
    {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * Convert given {@code value} to {@link ReadablePartial} value.
     *
     * @param value
     * @return converted {@link ReadablePartial} value
     */
    public static ReadablePartial convertToReadablePartial(Object value)
    {
        if (value == null) {
            return null;
        }
        else if (value instanceof ReadablePartial) {
            return (ReadablePartial) value;
        }
        else {
            String stringValue = checkMaximumStringLength(value.toString(), READABLE_PARTIAL_MAXIMUM_LENGTH);
            return convertStringToReadablePartial(stringValue);
        }
    }

    /**
     * Convert given {@code value} to {@link LocalDate} value.
     *
     * @param value
     * @return converted {@link LocalDate} value
     */
    public static LocalDate convertToLocalDate(Object value)
    {
        if (value == null) {
            return null;
        }
        else if (value instanceof LocalDate) {
            return (LocalDate) value;
        }
        else {
            String stringValue = checkMaximumStringLength(value.toString(), LOCAL_DATE_MAXIMUM_LENGTH);
            return convertStringToLocalDate(stringValue);
        }
    }

    /**
     * Array of supported partial fields (in the same order as in the partial regex pattern).
     */
    private static final DateTimeFieldType[] PARTIAL_FIELDS = new DateTimeFieldType[]{
            DateTimeFieldType.year(),
            DateTimeFieldType.monthOfYear(),
            DateTimeFieldType.dayOfMonth(),
            DateTimeFieldType.hourOfDay(),
            DateTimeFieldType.minuteOfHour()
    };

    /**
     * Convert given {@link String} {@code value} to {@link ReadablePartial} value.
     *
     * @param value
     * @return converted {@link ReadablePartial} value
     */
    public static ReadablePartial convertStringToReadablePartial(String value)
    {
        Pattern pattern = Pattern.compile("(\\d{1,4})(-\\d{1,2})?(-\\d{1,2})?(T\\d{1,2})?(:\\d{1,2})?");
        Matcher matcher = pattern.matcher(value);
        if (matcher.matches()) {
            Partial partial = new Partial();
            for (int index = 0; index < PARTIAL_FIELDS.length; index++) {
                String group = matcher.group(index + 1);
                if (group == null) {
                    continue;
                }
                char first = group.charAt(0);
                if (first < '0' || first > '9') {
                    group = group.substring(1, group.length());
                }
                partial = partial.with(PARTIAL_FIELDS[index], Integer.parseInt(group));
            }
            return partial;
        }
        throw new CommonReportSet.TypeIllegalValueException("PartialDateTime", value);
    }

    /**
     * Convert given {@link Collection} {@code value} to {@link Object[]}.
     *
     * @param value
     * @return converted {@link Object[]} value
     */
    public static Object[] convertCollectionToArray(Collection value)
    {
        return value.toArray();
    }

    /**
     * Convert given {@code value} to {@link Collection} value of given {@code collectionClass}
     * with items of given {@code componentClass}.
     *
     * @param value
     * @param collectionClass
     * @param componentClasses
     * @return converted {@link Interval} value
     */
    public static <T extends Collection> T convertToCollection(Object value, Class<T> collectionClass,
            Class... componentClasses)
    {
        if (value == null) {
            return collectionClass.cast(ClassHelper.createCollection(collectionClass, 0));
        }
        else if (value instanceof Collection) {
            Collection<Object> collection = ClassHelper.createCollection(collectionClass, 0);
            for (Object collectionItem : (Collection) value) {
                if (collectionItem == null) {
                    throw new CommonReportSet.CollectionItemNullException((String) null);
                }
                collection.add(convert(collectionItem, componentClasses));
            }
            return collectionClass.cast(collection);
        }
        else if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            Collection<Object> collection = ClassHelper.createCollection(collectionClass, 0);
            for (Object arrayItem : array) {
                if (arrayItem == null) {
                    throw new CommonReportSet.CollectionItemNullException((String) null);
                }
                collection.add(convert(arrayItem, componentClasses));
            }
            return collectionClass.cast(collection);
        }
        else {
            throw new TodoImplementException(value.getClass());
        }
    }

    /**
     * Convert given {@code value} to {@link Set} value with items of given {@code componentClass}.
     *
     * @param value
     * @param componentClass
     * @return converted {@link Interval} value
     */
    public static <T> Set<T> convertToSet(Object value, Class<T> componentClass)
    {
        @SuppressWarnings("unchecked")
        Set<T> set = (Set<T>) convertToCollection(value, Set.class, componentClass);
        return set;
    }

    /**
     * Convert given {@code value} to {@link List} value with items of given {@code componentClass}.
     *
     * @param value
     * @param componentClass
     * @return converted {@link Interval} value
     */
    public static <T> List<T> convertToList(Object value, Class<T> componentClass)
    {
        @SuppressWarnings("unchecked")
        List<T> list = (List<T>) convertToCollection(value, List.class, componentClass);
        return list;
    }

    /**
     * Convert given {@code value} to {@link List} value with items of any of given {@code componentClasses}.
     *
     * @param value
     * @param componentClasses
     * @return converted {@link Interval} value
     */
    public static List<Object> convertToList(Object value, Class... componentClasses)
    {
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) convertToCollection(value, List.class, componentClasses);
        return list;
    }

    /**
     * Convert given {@code value} to {@link Map} value with keys of given {@code keyClass} and values
     * of given {@code valueClass}.
     *
     * @param value
     * @param keyClass
     * @param valueClass
     * @return converted {@link Interval} value
     */
    public static <K, V> Map<K, V> convertToMap(Object value, Class<K> keyClass, Class<V> valueClass)
    {
        if (value == null) {
            return new HashMap<K, V>();
        }
        else if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> mapValue = (Map<Object, Object>) value;
            Map<K, V> map = new HashMap<K, V>();
            for (Map.Entry entry : mapValue.entrySet()) {
                K entryKey = convert(entry.getKey(), keyClass);
                V entryValue = convert(entry.getValue(), valueClass);
                map.put(entryKey, entryValue);
            }
            return map;
        }
        else {
            throw new TodoImplementException(value.getClass());
        }
    }

    /**
     * Convert given {@link AtomicType} {@code value} to {@link String}.
     *
     * @param value
     * @return converted {@link String} value
     */
    public static String convertAtomicTypeToString(AtomicType value)
    {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    /**
     * Convert given {@link String} {@code value} to {@link AtomicType} value of given {@code atomicTypeClass}.
     *
     * @param value
     * @param atomicTypeClass
     * @return converted {@link AtomicType} value
     */
    public static <T extends AtomicType> T convertStringToAtomicType(String value, Class<T> atomicTypeClass)
    {
        if (value == null) {
            return null;
        }
        T atomicType = null;
        try {
            atomicType = atomicTypeClass.getDeclaredConstructor().newInstance();
        }
        catch (java.lang.Exception exception) {
            throw new CommonReportSet.ClassInstantiationErrorException(ClassHelper.getClassShortName(atomicTypeClass));
        }
        atomicType.fromString(value);
        return atomicType;
    }

    /**
     * Convert given {@link ComplexType} {@code value} to {@link Map}.
     *
     * @param value
     * @return converted {@link Map} value
     */
    public static Map convertComplexTypeToMap(ComplexType value)
    {
        if (value == null) {
            return null;
        }
        return value.toData().getData();
    }

    /**
     * Convert given {@link Map} {@code value} to {@link ComplexType} value of given {@code complexTypeClass}.
     *
     * @param value
     * @param complexTypeClass
     * @return converted {@link ComplexType} value
     */
    public static <T extends ComplexType> T convertMapToComplexType(Map value, Class<T> complexTypeClass)
    {
        Class newValueClass = complexTypeClass;
        if (value.containsKey(ComplexType.CLASS_PROPERTY)) {
            String className = (String) value.get(ComplexType.CLASS_PROPERTY);
            try {
                newValueClass = ClassHelper.getClassFromShortName(className);
            }
            catch (ClassNotFoundException exception) {
                throw new CommonReportSet.ClassUndefinedException(className);
            }
        }
        ComplexType complexType = (ComplexType) ClassHelper.createInstanceFromClass(newValueClass);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) value;
        complexType.fromData(new DataMap(complexType, map));
        if (!complexTypeClass.isInstance(complexType)) {
            throw new CommonReportSet.TypeMismatchException(
                    ClassHelper.getClassShortName(complexTypeClass), ClassHelper.getClassShortName(newValueClass));
        }
        return complexTypeClass.cast(complexType);
    }

    /**
     * Convert given {@code value} to given {@code targetClass}.
     *
     * @param value
     * @param targetClass
     * @return converted {@code value} to {@code targetClass}
     */
    @SuppressWarnings("unchecked")
    public static <T> T convert(Object value, Class<T> targetClass)
    {
        if (targetClass == null) {
            throw new IllegalArgumentException("Target class cannot be null");
        }
        else if (targetClass.isInstance(value)) {
            return (T) value;
        }
        else if (String.class.equals(targetClass)) {
            return (T) convertToString(value);
        }
        else if (Enum.class.isAssignableFrom(targetClass)) {
            return (T) convertToEnum(value, (Class<Enum>) targetClass);
        }
        else if (DateTime.class.equals(targetClass)) {
            return (T) convertToDateTime(value);
        }
        else if (Period.class.equals(targetClass)) {
            return (T) convertToPeriod(value);
        }
        else if (Interval.class.equals(targetClass)) {
            return (T) convertToInterval(value);
        }
        else if (LocalDate.class.isAssignableFrom(targetClass)) {
            return (T) convertToLocalDate(value);
        }
        else if (ReadablePartial.class.isAssignableFrom(targetClass)) {
            return (T) convertToReadablePartial(value);
        }
        else if (Long.class.equals(targetClass) && value instanceof Number) {
            return (T) Long.valueOf(((Number) value).longValue());
        }
        else if (ComplexType.class.isAssignableFrom(targetClass)) {
            Class<? extends ComplexType> abstractObjectClass = (Class<? extends ComplexType>) targetClass;
            if (value == null) {
                return null;
            }
            else if (value instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) value;
                return (T) convertMapToComplexType(map, abstractObjectClass);
            }
        }
        else if (Class.class.equals(targetClass)) {
            return (T) convertToClass(value);
        }
        String from = (value != null ? value.getClass().getName() : "null");
        String to = targetClass.getName();
        throw new TodoImplementException(from + " -> " + to);
    }

    /**
     * Convert given {@code value} to any of given {@code targetClasses}.
     *
     * @param value
     * @param targetClasses
     * @return converted {@code value} any of {@code targetClasses}
     */
    public static Object convert(Object value, Class... targetClasses)
    {
        if (targetClasses.length == 1) {
            return convert(value, targetClasses[0]);
        }
        else if (targetClasses.length > 1) {
            List<Exception> exceptionList = new ArrayList<Exception>(targetClasses.length);
            for (Class targetClass : targetClasses) {
                try {
                    return Converter.convert(value, targetClass);
                }
                catch (Exception exception) {
                    exceptionList.add(exception);
                }
            }
            for (int index = 0; index < exceptionList.size(); index++) {
                logger.debug(String.format("Cannot convert value '%s' to '%s'.",
                        value.getClass().getCanonicalName(), targetClasses[index].getCanonicalName()),
                        exceptionList.get(index));
            }
            throw new CommonReportSet.ClassAttributeTypeMismatchException(
                    null, null, null, ClassHelper.getClassShortName(value.getClass()));
        }
        else {
            throw new IllegalArgumentException("Required classes must not be empty.");
        }
    }

    /**
     * @param value         to be checked
     * @param maximumLength to be checked in given {@code value}
     * @return given {@code value}
     */
    public static String checkMaximumStringLength(String value, int maximumLength)
            throws CommonReportSet.ValueMaximumLengthExceededException
    {
        if (value == null) {
            return null;
        }
        if (value.length() > maximumLength) {
            throw new CommonReportSet.ValueMaximumLengthExceededException(value, maximumLength);
        }
        return value;
    }

    /**
     * @param values         to be checked
     * @param maximumLength to be checked in given {@code value}
     * @return given {@code value}
     */
    public static List<String> checkMaximumStringLength(List<String> values, int maximumLength)
            throws CommonReportSet.ValueMaximumLengthExceededException
    {
        for (String value : values) {
            checkMaximumStringLength(value, maximumLength);
        }
        return values;
    }

    /**
     * @param values         to be checked
     * @param maximumLength to be checked in given {@code value}
     * @return given {@code value}
     */
    public static Set<String> checkMaximumStringLength(Set<String> values, int maximumLength)
            throws CommonReportSet.ValueMaximumLengthExceededException
    {
        for (String value : values) {
            checkMaximumStringLength(value, maximumLength);
        }
        return values;
    }
}
