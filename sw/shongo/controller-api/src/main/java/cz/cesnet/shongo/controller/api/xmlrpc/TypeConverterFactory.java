package cz.cesnet.shongo.controller.api.xmlrpc;

import cz.cesnet.shongo.controller.api.AtomicType;
import cz.cesnet.shongo.controller.api.ComplexType;
import cz.cesnet.shongo.controller.api.Fault;
import cz.cesnet.shongo.controller.api.FaultException;
import cz.cesnet.shongo.controller.api.util.Converter;
import org.apache.xmlrpc.common.TypeConverter;
import org.apache.xmlrpc.common.TypeConverterFactoryImpl;

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
     * Option whether store changes for object when it is converted map.
     */
    private boolean storeChanges;

    /**
     * Converter for {@link AtomicType}.
     */
    private TypeConverter atomicTypeConverter = new AtomicTypeConverter(AtomicType.class);

    /**
     * Converter for {@link Map}.
     */
    private TypeConverter mapTypeConverter;

    /**
     * Constructor.
     *
     * @param storeChanges sets the {@link #storeChanges}
     */
    public TypeConverterFactory(boolean storeChanges)
    {
        this.storeChanges = storeChanges;
        mapTypeConverter = new MapTypeConverter(storeChanges);
    }

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
            return ComplexTypeConverter.getInstance(pClass, storeChanges);
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
                    throw new RuntimeException(new FaultException(Fault.Common.CLASS_CANNOT_BE_INSTANCED, clazz));
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
     * ComplexType converter.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class ComplexTypeConverter implements TypeConverter
    {
        /**
         * Option whether store changes for object when it is converted map.
         */
        private boolean storeChanges;

        /**
         * Type of object.
         */
        private final Class type;

        /**
         * Constructor.
         *
         * @param storeChanges sets the {@link #storeChanges}
         */
        ComplexTypeConverter(Class type, boolean storeChanges)
        {
            this.type = type;
            this.storeChanges = storeChanges;
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
                    return Converter.convertMapToObject((Map) pObject, type);
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
                return Converter.convertObjectToMap(pObject, storeChanges);
            }
            catch (FaultException exception) {
                throw new RuntimeException(exception);
            }
        }

        public static ComplexTypeConverter getInstance(Class pClass, boolean storeChanges)
        {
            // TODO: Reuse instances for same class
            return new ComplexTypeConverter(pClass, storeChanges);
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
        private boolean storeChanges;

        /**
         * Constructor.
         *
         * @param storeChanges sets the {@link #storeChanges}
         */
        public MapTypeConverter(boolean storeChanges)
        {
            this.storeChanges = storeChanges;
        }

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
                    return Converter.convertObjectToMap(pObject, storeChanges);
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
