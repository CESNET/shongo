package cz.cesnet.shongo.common.xmlrpc;

import org.apache.xmlrpc.common.TypeConverter;
import org.apache.xmlrpc.common.TypeConverterFactoryImpl;

/**
 * TypeConverterFactory that allows classes that extends from Type as method parameters.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class TypeConverterFactory extends TypeConverterFactoryImpl
{
    private static final TypeConverter atomicTypeConverter = new AtomicTypeConverter(AtomicType.class);
    private static final TypeConverter structTypeConverter = new IdentityTypeConverter(StructType.class);

    @Override
    public TypeConverter getTypeConverter(Class pClass)
    {
        if (AtomicType.class.isAssignableFrom(pClass)) {
            return atomicTypeConverter;
        }
        else if (StructType.class.isAssignableFrom(pClass)) {
            return structTypeConverter;
        }
        else if (pClass.isEnum()) {
            return EnumTypeConverter.getInstance(pClass);
        }
        return super.getTypeConverter(pClass);
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
                    throw new RuntimeException(new FaultException(Fault.Common.ClassCannotBeInstanced,
                            BeanUtils.getShortClassName(clazz.getCanonicalName())));
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
    private static class IdentityTypeConverter implements TypeConverter
    {
        private final Class clazz;

        IdentityTypeConverter(Class pClass)
        {
            clazz = pClass;
        }

        @Override
        public boolean isConvertable(Object pObject)
        {
            return pObject == null || clazz.isAssignableFrom(pObject.getClass());
        }

        @Override
        public Object convert(Object pObject)
        {
            return pObject;
        }

        @Override
        public Object backConvert(Object pObject)
        {
            return pObject;
        }
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
                    return Enum.valueOf(clazz, value);
                }
                catch (IllegalArgumentException exception) {
                    throw new RuntimeException(
                            new FaultException(Fault.Common.EnumNotDefined, value, BeanUtils.getShortClassName(
                                    clazz.getCanonicalName()))
                    );
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
}
