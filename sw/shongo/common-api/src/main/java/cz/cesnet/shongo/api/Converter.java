package cz.cesnet.shongo.api;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.Temporal;
import cz.cesnet.shongo.TodoImplementException;
import org.joda.time.*;
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
    private static Logger logger = LoggerFactory.getLogger(Converter.class);

    private static final long DATETIME_INFINITY_START_MILLIS = Temporal.DATETIME_INFINITY_START.getMillis();

    private static final long DATETIME_INFINITY_END_MILLIS = Temporal.DATETIME_INFINITY_END.getMillis();

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
        else {
            throw new TodoImplementException(value.toString());
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
            return convertStringToEnum(value.toString(), enumClass);
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
            dateTime = ISODateTimeFormat.dateTimeParser().parseDateTime(value);
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
            return convertStringToPeriod(value.toString());
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
            return convertStringToReadablePartial(value.toString());
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
            return null;
        }
        else if (value instanceof Object[]) {
            Object[] array = (Object[]) value;
            Collection<Object> collection = ClassHelper.createCollection(collectionClass, 0);
            for (Object arrayItem : array) {
                collection.add(convert(arrayItem, componentClasses));
            }
            return collectionClass.cast(collection);
        }
        throw new TodoImplementException();
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
     * Convert given {@link AtomicType} {@code value} to {@link String}.
     *
     * @param value
     * @return converted {@link String} value
     */
    public static Object convertAtomicTypeToString(AtomicType value)
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
            atomicType = atomicTypeClass.newInstance();
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
     * TODO: refactorize API
     */
    @SuppressWarnings("unchecked")
    public static <T> T convert(Object value, Class<T> targetClass)
    {
        if (targetClass.isInstance(value)) {
            return (T) value;
        }
        if (String.class.equals(targetClass)) {
            return (T) convertToString(value);
        }
        else if (Enum.class.isAssignableFrom(targetClass)) {
            return (T) convertToEnum(value, (Class<Enum>)targetClass);
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
        else if (ReadablePartial.class.isAssignableFrom(targetClass)) {
            return (T) convertToReadablePartial(value);
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
        String from = (value != null ? value.getClass().getName() : "null");
        String to = (targetClass != null ? targetClass.getName() : "null");
        throw new TodoImplementException(from + " -> " + to);
    }

    private static Object convert(Object value, Class... targetClasses)
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
            throw new IllegalArgumentException();
        }
        else {
            throw new IllegalArgumentException("Required classes must not be empty.");
        }
    }

    public static Boolean convertToBoolean(Object value)
    {
        if (value == null) {
            return  null;
        }
        else if (value instanceof Boolean) {
            return (Boolean) value;
        }
        throw new TodoImplementException(value.getClass().getCanonicalName());
    }

    public static Integer convertToInteger(Object value)
    {
        if (value == null) {
            return  null;
        }
        else if (value instanceof Integer) {
            return (Integer) value;
        }
        throw new TodoImplementException(value.getClass().getCanonicalName());
    }
}
