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
            return EnumTypeConverter.getInstance(pClass);
        }
        else if (AtomicType.class.isAssignableFrom(pClass)) {
            return new AtomicTypeConverter(pClass);
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
        private final Class clazz;

        EnumTypeConverter(Class pClass)
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

        public static EnumTypeConverter getInstance(Class pClass)
        {
            // TODO: Reuse instances for same class
            return new EnumTypeConverter(pClass);
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

        AtomicTypeConverter(Class pClass)
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
        StructTypeConverter(Class type, Options options)
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

        public static StructTypeConverter getInstance(Class pClass, Options options)
        {
            // TODO: Reuse instances for same class
            return new StructTypeConverter(pClass, options);
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
