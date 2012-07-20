package cz.cesnet.shongo.api.util;

import cz.cesnet.shongo.api.Fault;
import cz.cesnet.shongo.api.FaultException;

import java.util.Collection;

/**
 * Serializer contains static methods for serializing implementation classes to/from API classes.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class Serializer<IMPL, API>
{
    /**
     * @param impl
     * @param propertyName
     * @return property value from given object and property name
     * @throws FaultException
     */
    public static Object getApiPropertyValue(Object impl, String propertyName) throws FaultException
    {
        Property property = Property.getProperty(impl.getClass(), propertyName);
        if (property == null) {
            return null;
        }
        return Property.getPropertyValue(impl, propertyName);
    }

    /**
     * Sets given value to given object property
     *
     * @param impl
     * @param propertyName
     * @param value
     * @throws FaultException
     */
    public static void setApiPropertyValue(Object impl, String propertyName, Object value) throws FaultException
    {
        Property.setPropertyValue(impl, propertyName, value);
    }

    /**
     * @param impl
     * @param apiType
     * @return given object converted to API type
     * @throws FaultException
     */
    public static <IMPL, API> API toApi(IMPL impl, Class<API> apiType) throws FaultException
    {
        API api;
        try {
            api = apiType.newInstance();
        }
        catch (Exception exception) {
            throw new FaultException(Fault.Common.CLASS_CANNOT_BE_INSTANCED, apiType);
        }
        toApi(impl, api);
        return api;
    }

    /**
     * @param allowedTypes
     * @param implValue
     * @return value converted to API by given allowed types
     * @throws FaultException
     */
    private static Object applyAllowedTypes(Class[] allowedTypes, Object implValue) throws FaultException
    {
        Class implValueType = implValue.getClass();
        boolean assignable = false;
        for (Class allowedType : allowedTypes) {
            if (allowedType.isAssignableFrom(implValueType)) {
                assignable = true;
                break;
            }
        }
        if (assignable) {
            return implValue;
        }
        else {
            Class apiValueType;
            try {
                apiValueType = ClassHelper.getClassFromShortName(implValue.getClass().getSimpleName());
            }
            catch (ClassNotFoundException e) {
                throw new FaultException("API class for type '%s' cannot be found.",
                        implValue.getClass().getSimpleName());
            }
            return toApi(implValue, apiValueType);
        }

    }

    /**
     * Convert given implementation object to API object
     *
     * @param impl
     * @param api
     * @throws FaultException
     */
    public static <IMPL, API> void toApi(IMPL impl, API api) throws FaultException
    {
        Class implType = impl.getClass();
        Class apiType = api.getClass();
        String[] propertyNames = Property.getPropertyNames(apiType);
        SerializerListener listener = null;
        if (impl instanceof SerializerListener) {
            listener = (SerializerListener) impl;
        }
        for (String propertyName : propertyNames) {
            Property apiProperty = Property.getProperty(apiType, propertyName);
            Object apiPropertyValue = null;
            Class apiPropertyType = apiProperty.getType();

            // Get value
            Object implPropertyValue;
            if (listener != null) {
                implPropertyValue = listener.getApiPropertyValue(propertyName);
            }
            else {
                implPropertyValue = getApiPropertyValue(impl, propertyName);
            }
            if (implPropertyValue == null) {
                continue;
            }

            // Set it to api
            Class implPropertyType = implPropertyValue.getClass();
            if (apiProperty.isCollection()) {
                Collection implCollection = (Collection) implPropertyValue;
                Collection apiCollection = Converter.createCollection(apiPropertyType, implCollection.size());
                Class[] allowedTypes = apiProperty.getAllowedTypes();
                for (Object implItem : implCollection) {
                    apiCollection.add(applyAllowedTypes(allowedTypes, implItem));
                }
                apiPropertyValue = apiCollection;
            }
            else {
                if (apiPropertyType.isAssignableFrom(implPropertyValue.getClass())) {
                    Class[] allowedTypes = apiProperty.getAllowedTypes();
                    if (allowedTypes != null && allowedTypes.length > 0) {
                        apiPropertyValue = applyAllowedTypes(allowedTypes, implPropertyValue);
                    }
                    else {
                        apiPropertyValue = implPropertyValue;
                    }
                }
                else {
                    if (implPropertyType.equals(Long.class) && apiPropertyType.equals(Integer.class)) {
                        apiPropertyValue = ((Long) implPropertyValue).intValue();
                    }
                }
            }
            if (apiPropertyValue != null) {
                apiProperty.setValue(api, apiPropertyValue, true);
            }
            else {
                throw new FaultException("Cannot convert '%s' to '%s'.", implPropertyType, apiPropertyType);
            }
        }
    }
}
