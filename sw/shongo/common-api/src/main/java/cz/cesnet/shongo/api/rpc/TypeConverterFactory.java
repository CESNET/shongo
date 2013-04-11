package cz.cesnet.shongo.api.rpc;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.api.util.Converter;
import cz.cesnet.shongo.api.util.Options;
import org.apache.xmlrpc.common.TypeConverter;
import org.apache.xmlrpc.common.TypeConverterFactoryImpl;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.Period;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
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

    /**
     * @param type
     * @param genericType
     * @return {@link TypeConverter} for given {@code type} and {@code genericType}
     */
    public TypeConverter getTypeConverter(Class type, Type genericType)
    {
        if (type.isEnum()) {
            @SuppressWarnings("unchecked")
            Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) type;
            return EnumTypeConverter.getInstance(enumClass);
        }
        else if (AtomicType.class.isAssignableFrom(type)) {
            return AtomicTypeConverter.getInstance(type);
        }
        else if (Interval.class.isAssignableFrom(type)) {
            return intervalConverter;
        }
        else if (DateTime.class.isAssignableFrom(type)) {
            return dateTimeConverter;
        }
        else if (Period.class.isAssignableFrom(type)) {
            return periodConverter;
        }
        else if (StructType.class.isAssignableFrom(type)) {
            return StructTypeConverter.getInstance(type, options);
        }
        else if (Map.class.isAssignableFrom(type)) {
            return mapTypeConverter;
        }
        else if (Collection.class.isAssignableFrom(type)) {
            Class componentType = Object.class;
            if (genericType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericType;
                Type[] arguments = parameterizedType.getActualTypeArguments();
                if (arguments.length == 1 && arguments[0] instanceof Class) {
                    componentType = (Class) arguments[0];
                }

            }
            return new CollectionTypeConverter(type, componentType);
        }
        return super.getTypeConverter(type);
    }

    @Override
    public TypeConverter getTypeConverter(Class type)
    {
        return getTypeConverter(type, null);
    }

    /**
     * Converter for enum types.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class EnumTypeConverter implements TypeConverter
    {
        private final Class<? extends Enum> enumType;

        private EnumTypeConverter(Class<? extends Enum> enumType)
        {
            this.enumType = enumType;
        }

        @Override
        public boolean isConvertable(Object pObject)
        {
            return pObject == null || (pObject instanceof String) || enumType.isAssignableFrom(pObject.getClass());
        }

        @Override
        public Object convert(Object pObject)
        {
            if (pObject instanceof String) {
                String value = (String) pObject;
                return Converter.Atomic.convertStringToEnum(value, enumType);
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
                    throw new CommonReportSet.ClassInstantiationErrorException(clazz.getSimpleName());
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
                return Converter.Atomic.convertStringToInterval((String) pObject);
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
                return Converter.Atomic.convertStringToDateTime((String) pObject);
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
                return Converter.Atomic.convertStringToPeriod((String) pObject);
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
                return Converter.convertFromBasic(pObject, type, options);
            }
            return pObject;
        }

        @Override
        public Object backConvert(Object pObject)
        {
            return Converter.convertToBasic(pObject, options);
        }

        /**
         * Cache for {@link StructTypeConverter}.
         */
        private static Map<Options, Map<Class, StructTypeConverter>> cache =
                new Hashtable<Options, Map<Class, StructTypeConverter>>();

        /**
         * @param pClass  for which the converter should be returned
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
                return Converter.convertToBasic(pObject, options);
            }
            return pObject;
        }

        @Override
        public Object backConvert(Object pObject)
        {
            return pObject;
        }
    }

    /**
     * Converter for {@link Collection} types.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class CollectionTypeConverter implements TypeConverter
    {
        private final Class<? extends Collection> collectionType;
        private final Class componentType;

        private CollectionTypeConverter(Class<? extends Collection> collectionType, Class componentType)
        {
            this.collectionType = collectionType;
            this.componentType = componentType;
        }

        @Override
        public boolean isConvertable(Object object)
        {
            return object == null || object instanceof Object[]
                    || collectionType.isAssignableFrom(object.getClass());
        }

        @Override
        public Object convert(Object object)
        {
            if (object != null) {
                return Converter.convert(object, collectionType, new Class[]{componentType});
            }
            return object;
        }

        @Override
        public Object backConvert(Object result)
        {
            return ((Collection) result).toArray();
        }
    }
}
