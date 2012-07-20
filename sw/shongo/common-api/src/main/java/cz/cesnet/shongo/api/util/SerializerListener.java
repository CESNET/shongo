package cz.cesnet.shongo.api.util;

import cz.cesnet.shongo.api.FaultException;

/**
 * Interface that can be implemented by implementaiton objects to customize value serializing to API objects
 * by {@link Serializer}.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public interface SerializerListener
{
    /**
     * This method is called for every property that exists in API object to retrieve it's value from implementation
     * object. Implementation object can implement this method to perform custom getting. For default implementation
     * {@link Serializer#getApiPropertyValue(Object, String)} can be called.
     *
     * @param propertyName
     * @return value from object
     * @throws FaultException
     */
    public Object getApiPropertyValue(String propertyName) throws FaultException;

    /**
     * This method is called for every property that exists in API object to save it's value to implementation object.
     * Implementation object can implement this method to perform custom setting. For default implementation
     * {@link Serializer#setApiPropertyValue(Object, String, Object)} can be called.
     *
     * @param propertyName
     * @param value
     * @throws FaultException
     */
    public void setApiPropertyValue(String propertyName, Object value) throws FaultException;
}
