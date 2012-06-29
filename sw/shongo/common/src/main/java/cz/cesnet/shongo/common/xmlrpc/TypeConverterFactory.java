package cz.cesnet.shongo.common.xmlrpc;

import cz.cesnet.shongo.common.api.AtomicType;
import cz.cesnet.shongo.common.api.ComplexType;
import cz.cesnet.shongo.common.api.Fault;
import cz.cesnet.shongo.common.api.FaultException;
import cz.cesnet.shongo.common.util.Converter;
import org.apache.xmlrpc.common.TypeConverter;
import org.apache.xmlrpc.common.TypeConverterFactoryImpl;

import java.util.Map;

/**
 * TypeConverterFactory that allows {@link AtomicType}, {@link cz.cesnet.shongo.common.api.ComplexType} and enums as method parameters
 * and return values.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TypeConverterFactory extends TypeConverterFactoryImpl
{
    private static final TypeConverter atomicTypeConverter = new AtomicTypeConverter(AtomicType.class);
    private static final TypeConverter mapTypeConverter = new MapTypeConverter();

    @Override
    public TypeConverter getTypeConverter(Class pClass)
    {
        if (pClass.isEnum()) {
            return EnumTypeConverter.getInstance(pClass);
        }
        else if (AtomicType.class.isAssignableFrom(pClass)) {
            return atomicTypeConverter;
        }
        else if (ComplexType.class.isAssignableFrom(pClass)) {
            return ComplexTypeConverter.getInstance(pClass);
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
                    return Converter.convertStringToEnum(value, clazz);
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
            return (pObject instanceof String) || clazz.isInstance(pObject);
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
                    throw new RuntimeException(new FaultException(Fault.Common.CLASS_CANNOT_BE_INSTANCED,
                            Converter.getClassShortName(clazz)));
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
     * Identity converter.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class ComplexTypeConverter implements TypeConverter
    {
        private final Class clazz;

        ComplexTypeConverter(Class pClass)
        {
            clazz = pClass;
        }

        @Override
        public boolean isConvertable(Object pObject)
        {
            return pObject == null || clazz.isAssignableFrom(pObject.getClass()) || pObject instanceof Map;
        }

        @Override
        public Object convert(Object pObject)
        {
            if (pObject instanceof Map) {
                try {
                    return Converter.convertMapToObject((Map) pObject, clazz);
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
                return Converter.convertObjectToMap(pObject);
            }
            catch (FaultException exception) {
                throw new RuntimeException(exception);
            }
        }

        public static ComplexTypeConverter getInstance(Class pClass)
        {
            // TODO: Reuse instances for same class
            return new ComplexTypeConverter(pClass);
        }
    }

    private static class MapTypeConverter implements TypeConverter
    {
        @Override
        public boolean isConvertable(Object pObject)
        {
            return pObject == null || Map.class.isAssignableFrom(pObject.getClass())
                    || ComplexType.class.isAssignableFrom(pObject.getClass());
        }

        @Override
        public Object convert(Object pObject)
        {
            if (pObject instanceof ComplexType) {
                try {
                    return Converter.convertObjectToMap(pObject);
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
