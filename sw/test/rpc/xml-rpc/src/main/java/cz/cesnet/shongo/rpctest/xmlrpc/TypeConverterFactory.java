package cz.cesnet.shongo.rpctest.xmlrpc;

import cz.cesnet.shongo.rpctest.common.API;
import org.apache.xmlrpc.common.TypeConverter;
import org.apache.xmlrpc.common.TypeConverterFactoryImpl;

public class TypeConverterFactory extends TypeConverterFactoryImpl
{
    private static class IdentityTypeConverter implements TypeConverter {
        private final Class clazz;
        IdentityTypeConverter(Class pClass) {
            clazz = pClass;
        }
        public boolean isConvertable(Object pObject) {
            return pObject == null  ||  clazz.isAssignableFrom(pObject.getClass());
        }
        public Object convert(Object pObject) {
            return pObject;
        }
        public Object backConvert(Object pObject) {
            return pObject;
        }
    }

    private static final TypeConverter dateTypeConverter = new IdentityTypeConverter(API.Date.class);
    private static final TypeConverter periodicDateTypeConverter = new IdentityTypeConverter(API.PeriodicDate.class);

    @Override
    public TypeConverter getTypeConverter(Class pClass) {
        if (pClass.isAssignableFrom(API.Date.class)) {
            return dateTypeConverter;
        }
        if (pClass.isAssignableFrom(API.PeriodicDate.class)) {
            return periodicDateTypeConverter;
        }
        return super.getTypeConverter(pClass);
    }
}
