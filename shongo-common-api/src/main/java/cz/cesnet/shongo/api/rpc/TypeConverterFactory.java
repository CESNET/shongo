package cz.cesnet.shongo.api.rpc;

import cz.cesnet.shongo.api.AtomicType;
import cz.cesnet.shongo.api.ComplexType;
import cz.cesnet.shongo.api.Converter;
import org.apache.xmlrpc.common.TypeConverter;
import org.apache.xmlrpc.common.TypeConverterFactoryImpl;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.Period;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * TypeConverterFactory that allows {@link AtomicType}, {@link ComplexType} and enums as method parameters
 * and return values.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TypeConverterFactory extends TypeConverterFactoryImpl
{
    /**
     * Converter for {@link Interval}.
     */
    private TypeConverter intervalConverter = new IntervalConverter();

    /**
     * Converter for {@link DateTime}.
     */
    private TypeConverter dateTimeConverter = new DateTimeConverter();

    /**
     * Converter for {@link LocalDate}.
     */
    private TypeConverter localDateConverter = new LocalDateConverter();

    /**
     * Converter for {@link Period}.
     */
    private TypeConverter periodConverter = new PeriodConverter();

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
        else if (DateTime.class.isAssignableFrom(type)) {
            return dateTimeConverter;
        }
        else if (LocalDate.class.isAssignableFrom(type)) {
            return localDateConverter;
        }
        else if (Period.class.isAssignableFrom(type)) {
            return periodConverter;
        }
        else if (Interval.class.isAssignableFrom(type)) {
            return intervalConverter;
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
        else if (AtomicType.class.isAssignableFrom(type)) {
            return AtomicTypeConverter.getInstance(type);
        }
        else if (ComplexType.class.isAssignableFrom(type)) {
            return ComplexTypeConverter.getInstance(type);
        }
        return super.getTypeConverter(type);
    }

    @Override
    public TypeConverter getTypeConverter(Class type)
    {
        return getTypeConverter(type, null);
    }

    /**
     * {@link TypeConverter} for {@link Enum}.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class EnumTypeConverter implements TypeConverter
    {
        /**
         * {@link Enum} class which will be converted.
         */
        private final Class<? extends Enum> enumClass;

        /**
         * Constructor.
         *
         * @param enumClass sets the {@link #enumClass}
         */
        private EnumTypeConverter(Class<? extends Enum> enumClass)
        {
            this.enumClass = enumClass;
        }

        @Override
        public boolean isConvertable(Object pObject)
        {
            return pObject == null || (pObject instanceof String) || enumClass.isAssignableFrom(pObject.getClass());
        }

        @Override
        public Object convert(Object pObject)
        {
            if (pObject instanceof String) {
                return Converter.convertStringToEnum((String) pObject, enumClass);
            }
            return pObject;
        }

        @Override
        public Object backConvert(Object result)
        {
            return Converter.convertEnumToString((Enum) result);
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
     * {@link TypeConverter} for {@link DateTime}.
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
                return Converter.convertStringToDateTime((String) pObject);
            }
            return pObject;
        }

        @Override
        public Object backConvert(Object result)
        {
            return Converter.convertDateTimeToString((DateTime) result);
        }
    }

    /**
     * {@link TypeConverter} for {@link DateTime}.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class LocalDateConverter implements TypeConverter
    {
        @Override
        public boolean isConvertable(Object pObject)
        {
            return pObject == null || (pObject instanceof String) || pObject instanceof LocalDate;
        }

        @Override
        public Object convert(Object pObject)
        {
            if (pObject instanceof String) {
                return Converter.convertStringToLocalDate((String) pObject);
            }
            return pObject;
        }

        @Override
        public Object backConvert(Object result)
        {
            return Converter.convertDateTimeToString((DateTime) result);
        }
    }

    /**
     * {@link TypeConverter} for {@link Period}.
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
                return Converter.convertStringToPeriod((String) pObject);
            }
            return pObject;
        }

        @Override
        public Object backConvert(Object result)
        {
            return Converter.convertPeriodToString((Period) result);
        }
    }

    /**
     * {@link TypeConverter} for {@link Interval}.
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
                return Converter.convertStringToInterval((String) pObject);
            }
            return pObject;
        }

        @Override
        public Object backConvert(Object result)
        {
            return Converter.convertIntervalToString((Interval) result);
        }
    }

    /**
     * {@link TypeConverter} for {@link Collection}.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class CollectionTypeConverter implements TypeConverter
    {
        /**
         * {@link Collection} class.
         */
        private final Class<? extends Collection> collectionClass;

        /**
         * {@link Collection} component class.
         */
        private final Class componentClass;

        /**
         * Constructor.
         *
         * @param collectionClass sets the {@link #collectionClass}
         * @param componentClass  sets the {@link #componentClass}
         */
        private CollectionTypeConverter(Class<? extends Collection> collectionClass, Class componentClass)
        {
            this.collectionClass = collectionClass;
            this.componentClass = componentClass;
        }

        @Override
        public boolean isConvertable(Object object)
        {
            return object == null || object instanceof Object[] || collectionClass.isAssignableFrom(object.getClass());
        }

        @Override
        public Object convert(Object object)
        {
            if (object != null) {
                return Converter.convertToCollection(object, collectionClass, componentClass);
            }
            return object;
        }

        @Override
        public Object backConvert(Object result)
        {
            return Converter.convertCollectionToArray((Collection) result);
        }
    }

    /**
     * {@link TypeConverter} for {@link AtomicType}.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class AtomicTypeConverter implements TypeConverter
    {
        /**
         * {@link AtomicType} class.
         */
        private final Class atomicTypeClass;

        /**
         * Constructor.
         *
         * @param atomicTypeClass sets the {@link #atomicTypeClass}
         */
        private AtomicTypeConverter(Class atomicTypeClass)
        {
            this.atomicTypeClass = atomicTypeClass;
        }

        @Override
        public boolean isConvertable(Object pObject)
        {
            return pObject == null || (pObject instanceof String) || atomicTypeClass.isInstance(pObject);
        }

        @Override
        public Object convert(Object pObject)
        {
            if (pObject instanceof String) {
                return Converter.convertStringToAtomicType((String) pObject, atomicTypeClass);
            }
            return pObject;
        }

        @Override
        public Object backConvert(Object result)
        {
            return Converter.convertAtomicTypeToString((AtomicType) result);
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
     * {@link TypeConverter} for {@link ComplexType}.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class ComplexTypeConverter implements TypeConverter
    {
        /**
         * Class of {@link cz.cesnet.shongo.api.ComplexType}.
         */
        private final Class<? extends ComplexType> complexTypeClass;

        /**
         * Constructor.
         *
         * @param complexTypeClass sets the {@link #complexTypeClass}
         */
        public ComplexTypeConverter(Class<? extends ComplexType> complexTypeClass)
        {
            this.complexTypeClass = complexTypeClass;
        }

        @Override
        public boolean isConvertable(Object object)
        {
            return object == null || object instanceof ComplexType || object instanceof Map;
        }

        @Override
        public Object convert(Object object)
        {
            if (object instanceof Map) {
                object = Converter.convertMapToComplexType((Map) object, complexTypeClass);
            }
            return object;
        }

        @Override
        public Object backConvert(Object result)
        {
            return Converter.convertComplexTypeToMap((ComplexType) result);
        }

        /**
         * Cache for {@link ComplexTypeConverter}s.
         */
        private static Map<Class<? extends ComplexType>, ComplexTypeConverter> cache =
                new HashMap<Class<? extends ComplexType>, ComplexTypeConverter>();

        /**
         * @param complexClass for which the converter should be returned
         * @return {@link ComplexTypeConverter} for given {@code complexTypeClass}
         */
        public static ComplexTypeConverter getInstance(Class<? extends ComplexType> complexClass)
        {
            ComplexTypeConverter complexTypeConverter = cache.get(complexClass);
            if (complexTypeConverter == null) {
                complexTypeConverter = new ComplexTypeConverter(complexClass);
                cache.put(complexClass, complexTypeConverter);
            }
            return complexTypeConverter;
        }
    }
}
