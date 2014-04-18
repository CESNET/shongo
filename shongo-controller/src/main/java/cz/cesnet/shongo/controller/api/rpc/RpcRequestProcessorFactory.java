package cz.cesnet.shongo.controller.api.rpc;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * We need to override request processor factory which creates instances of handler objects.
 * Default implementation create new instance for each request which is not desireable for
 * our purposes and this class overrides this mechanism to return existing instances
 * for specified class.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
class RpcRequestProcessorFactory
        extends RequestProcessorFactoryFactory.RequestSpecificProcessorFactoryFactory
{
    private Map<Class, Object> instances = new HashMap<Class, Object>();

    public void addInstance(Object object)
    {
        instances.put(object.getClass(), object);
    }

    @Override
    protected Object getRequestProcessor(Class pClass, XmlRpcRequest pRequest) throws XmlRpcException
    {
        if (instances.containsKey(pClass)) {
            return instances.get(pClass);
        }
        return super.getRequestProcessor(pClass, pRequest);
    }
}
