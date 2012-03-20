package cz.cesnet.shongo.common;

import org.apache.xmlrpc.common.TypeConverter;
import org.apache.xmlrpc.common.TypeConverterFactoryImpl;

/**
 * XmlRpcTypeConverterFactory that allows custom classes as method parameters
 *
 * @author Martin Srom
 */
public class XmlRpcTypeConverterFactory extends TypeConverterFactoryImpl
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

    private static final TypeConverter typeConverter = new IdentityTypeConverter(Type.class);

    @Override
    public TypeConverter getTypeConverter(Class pClass) {
        if ( Type.class.isAssignableFrom(pClass) ) {
            return typeConverter;
        }
        return super.getTypeConverter(pClass);
    }
}
