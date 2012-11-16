package cz.cesnet.shongo.api.xmlrpc;

import cz.cesnet.shongo.api.util.Converter;
import cz.cesnet.shongo.api.util.Options;
import cz.cesnet.shongo.fault.CommonFault;
import cz.cesnet.shongo.fault.FaultException;
import org.apache.xmlrpc.common.TypeConverter;
import org.apache.xmlrpc.common.TypeConverterFactoryImpl;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * TypeConverterFactory that allows {@link AtomicType}, {@link StructType} and enums as method parameters
 * and return values.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TypeConverterFactory extends TypeConverterFactoryImpl
{
    /**
     * Option whether store changes for object when it is converted map.
     */
    private Options options;

    /**
     * Converter for {@link Interval}.
     */
    private TypeConverter intervalConverter = new IntervalConverter();

    /**
     * Converter for {@link DateTime}.
     */
    private TypeConverter dateTimeConverter = new DateTimeConverter();

    /**
     * Converter for {@link Period}.
     */
    private TypeConverter periodConverter = new PeriodConverter();

    /**
     * Converter for {@link Map}.
     */
    private TypeConverter mapTypeConverter;

    /**
     * Constructor.
     *
     * @param options sets the {@link #options}
     */
    public TypeConverterFactory(Options options)
    {
        this.options = options;
        mapTypeConverter = new MapTypeConverter(options);
    }

    @Override
    public TypeConverter getTypeConverter(Class pClass)
    {
        if (pClass.isEnum()) {
            @SuppressWarnings("unchecked")
            Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) pClass;
            return EnumTypeConverter.getInstance(enumClass);
        }
        else if (AtomicType.class.isAssignableFrom(pClass)) {
            return AtomicTypeConverter.getInstance(pClass);
        }
        else if (Interval.class.isAssignableFrom(pClass)) {
            return intervalConverter;
        }
        else if (DateTime.class.isAssignableFrom(pClass)) {
            return dateTimeConverter;
        }
        else if (Period.class.isAssignableFrom(pClass)) {
            return periodConverter;
        }
        else if (StructType.class.isAssignableFrom(pClass)) {
            return StructTypeConverter.getInstance(pClass, options);
        }
        else if (Map.class.isAssignableFrom(pClass)) {
            return mapTypeConverter;
        }
        return super.getTypeConverter(pClass);
    }

    /**
     * Converter for enum types.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class EnumTypeConverter implements TypeConverter
    {
        private final Class<? extends Enum> clazz;

        private EnumTypeConverter(Class<? extends Enum> pClass)
        {
            clazz = pClass;
        }

        @Override
        public boolean isConvertable(Object pObject)
        {
            return (pObject instanceof String) || clazz.isAssignableFrom(pObject.getClass());
        }

        @Override
        public Object convert(Object pObject)
        {
            if (pObject instanceof String) {
                String value = (String) pObject;
                try {
                    return Converter.Atomic.convertStringToEnum(value, clazz);
                }
                catch (FaultException exception) {
                    throw new RuntimeException(exception);
                }
            }
            return pObject;
        }

        @Override
        public Object backConvert(Object result)
        {
            return result.toString();
        }

        /**
         * Cache for {@link EnumTypeConverter}.
         */
        private static Map<Class, EnumTypeConverter> cache = new HashMap<Class, EnumTypeConverter>();

        /**
         * @param pClass for which the converter should be returned
         * @return {@link EnumTypeConverter} for given {@code pClass}
         */
        public static EnumTypeConverter getInstance(Class<? extends Enum> pClass)
        {
            EnumTypeConverter enumTypeConverter = cache.get(pClass);
            if (enumTypeConverter == null) {
                enumTypeConverter = new EnumTypeConverter(pClass);
                cache.put(pClass, enumTypeConverter);
            }
            return enumTypeConverter;
        }
    }

    /**
     * Converter for atomic types.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class AtomicTypeConverter implements TypeConverter
    {
        private final Class clazz;

        private AtomicTypeConverter(Class pClass)
        {
            clazz = pClass;
        }

        @Override
        public boolean isConvertable(Object pObject)
        {
            return pObject == null || (pObject instanceof String) || clazz.isInstance(pObject);
        }

        @Override
        public Object convert(Object pObject)
        {
            if (pObject instanceof String) {
                String value = (String) pObject;
                AtomicType atomicType = null;
                try {
                    atomicType = (AtomicType) clazz.newInstance();
                }
                catch (java.lang.Exception exception) {
                    throw new RuntimeException(new FaultException(CommonFault.CLASS_CANNOT_BE_INSTANCED, clazz));
                }
                atomicType.fromString(value);
                return atomicType;
            }
            return pObject;
        }

        @Override
        public Object backConvert(Object result)
        {
            return result.toString();
        }

        /**
         * Cache for {@link AtomicTypeConverter}.
         */
        private static Map<Class, AtomicTypeConverter> cache = new HashMap<Class, AtomicTypeConverter>();

        /**
         * @param pClass for which the converter should be returned
         * @return {@link EnumTypeConverter} for given {@code pClass}
         */
        public static AtomicTypeConverter getInstance(Class pClass)
        {
            AtomicTypeConverter atomicTypeConverter = cache.get(pClass);
            if (atomicTypeConverter == null) {
                atomicTypeConverter = new AtomicTypeConverter(pClass);
                cache.put(pClass, atomicTypeConverter);
            }
            return atomicTypeConverter;
        }
    }

    /**
     * Converter for {@link Interval}.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class IntervalConverter implements TypeConverter
    {
        @Override
        public boolean isConvertable(Object pObject)
        {
            return pObject == null || (pObject instanceof String) || pObject instanceof Interval;
        }

        @Override
        public Object convert(Object pObject)
        {
            if (pObject instanceof String) {

                try {
                    return Converter.Atomic.convertStringToInterval((String) pObject);
                }
                catch (FaultException exception) {
                    throw new RuntimeException(exception);
                }
            }
            return pObject;
        }

        @Override
        public Object backConvert(Object result)
        {
            if (result == null) {
                return null;
            }
            return Converter.Atomic.convertIntervalToString((Interval) result);
        }
    }

    /**
     * Converter for {@link DateTime}.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class DateTimeConverter implements TypeConverter
    {
        @Override
        public boolean isConvertable(Object pObject)
        {
            return pObject == null || (pObject instanceof String) || pObject instanceof DateTime;
        }

        @Override
        public Object convert(Object pObject)
        {
            if (pObject instanceof String) {

                try {
                    return Converter.Atomic.convertStringToDateTime((String) pObject);
                }
                catch (FaultException exception) {
                    throw new RuntimeException(exception);
                }
            }
            return pObject;
        }

        @Override
        public Object backConvert(Object result)
        {
            if (result == null) {
                return null;
            }
            return result.toString();
        }
    }

    /**
     * Converter for {@link DateTime}.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class PeriodConverter implements TypeConverter
    {
        @Override
        public boolean isConvertable(Object pObject)
        {
            return pObject == null || (pObject instanceof String) || pObject instanceof Period;
        }

        @Override
        public Object convert(Object pObject)
        {
            if (pObject instanceof String) {

                try {
                    return Converter.Atomic.convertStringToPeriod((String) pObject);
                }
                catch (FaultException exception) {
                    throw new RuntimeException(exception);
                }
            }
            return pObject;
        }

        @Override
        public Object backConvert(Object result)
        {
            if (result == null) {
                return null;
            }
            return result.toString();
        }
    }

    /**
     * ComplexType converter.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class StructTypeConverter implements TypeConverter
    {
        /**
         * Option whether store changes for object when it is converted map.
         */
        private Options options;

        /**
         * Type of object.
         */
        private final Class type;

        /**
         * Constructor.
         *
         * @param options sets the {@link #options}
         */
        private StructTypeConverter(Class type, Options options)
        {
            this.type = type;
            this.options = options;
        }

        @Override
        public boolean isConvertable(Object pObject)
        {
            return pObject == null || type.isAssignableFrom(pObject.getClass()) || pObject instanceof Map;
        }

        @Override
        public Object convert(Object pObject)
        {
            if (pObject instanceof Map) {
                try {
                    return Converter.convertFromBasic(pObject, type, options);
                }
                catch (FaultException exception) {
                    throw new RuntimeException(exception);
                }
            }
            return pObject;
        }

        @Override
        public Object backConvert(Object pObject)
        {
            try {
                return Converter.convertToBasic(pObject, options);
            }
            catch (FaultException exception) {
                throw new RuntimeException(exception);
            }
        }

        /**
         * Cache for {@link StructTypeConverter}.
         */
        private static Map<Options, Map<Class, StructTypeConverter>> cache =
                new Hashtable<Options, Map<Class, StructTypeConverter>>();

        /**
         * @param pClass for which the converter should be returned
         * @param options for converting
         * @return {@link StructTypeConverter} for given {@code pClass} and {@code options}
         */
        public static StructTypeConverter getInstance(Class pClass, Options options)
        {
            Map<Class, StructTypeConverter> cacheByOptions = cache.get(options);
            if (cacheByOptions == null) {
                cacheByOptions = new HashMap<Class, StructTypeConverter>();
                cache.put(options, cacheByOptions);
            }
            StructTypeConverter structTypeConverter = cacheByOptions.get(pClass);
            if (structTypeConverter == null) {
                structTypeConverter = new StructTypeConverter(pClass, options);
                cacheByOptions.put(pClass, structTypeConverter);
            }
            return structTypeConverter;
        }
    }

    /**
     * Map converter.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class MapTypeConverter implements TypeConverter
    {
        /**
         * Option whether store changes for object when it is converted map.
         */
        private Options options;

        /**
         * Constructor.
         *
         * @param options sets the {@link #options}
         */
        public MapTypeConverter(Options options)
        {
            this.options = options;
        }

        @Override
        public boolean isConvertable(Object pObject)
        {
            return pObject == null || Map.class.isAssignableFrom(pObject.getClass())
                    || StructType.class.isAssignableFrom(pObject.getClass());
        }

        @Override
        public Object convert(Object pObject)
        {
            if (pObject instanceof StructType) {
                try {
                    return Converter.convertToBasic(pObject, options);
                }
                catch (FaultException exception) {
                    throw new RuntimeException(exception);
                }
            }
            return pObject;
        }

        @Override
        public Object backConvert(Object pObject)
        {
            return pObject;
        }
    }
}
