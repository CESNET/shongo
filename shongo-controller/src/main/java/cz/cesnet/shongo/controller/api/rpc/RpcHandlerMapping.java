package cz.cesnet.shongo.controller.api.rpc;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.api.rpc.Service;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler mapping that is able to translate full class name (which extends {@link cz.cesnet.shongo.api.rpc.Service})
 * to added handler name.
 * <p/>
 * It is required to be able to do: <p/>
 * <pre>
 * ClientFactory factory = new ClientFactory(client);
 * FooService service = (FooService) factory.newInstance(FooService.class);
 * </pre>
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
class RpcHandlerMapping extends PropertyHandlerMapping
{
    /**
     * Map of handler classes by name.
     */
    Map<String, Class> handlerClassMap = new HashMap<String, Class>();

    @Override
    protected boolean isHandlerMethod(Method pMethod)
    {
        boolean result = super.isHandlerMethod(pMethod);
        if (result) {
            Service.API api = AnnotationUtils.findAnnotation(pMethod, Service.API.class);
            return api != null;
        }
        return false;
    }

    @Override
    public void addHandler(String pKey, Class pClass) throws XmlRpcException
    {
        handlerClassMap.put(pKey, pClass);
        super.addHandler(pKey, pClass);
    }

    @Override
    @SuppressWarnings("unchecked")
    public XmlRpcHandler getHandler(String pHandlerName) throws XmlRpcException
    {
        XmlRpcHandler result = (XmlRpcHandler) handlerMap.get(pHandlerName);
        if (result == null) {
            Exception exception = null;
            try {
                // When no handler is found try to find it by requested class
                int pos = pHandlerName.lastIndexOf(".");
                if (pos != -1) {
                    String requestedClassName = pHandlerName.substring(0, pos);
                    String requestedMethod = pHandlerName.substring(pos + 1, pHandlerName.length());
                    Class requestedClass = Class.forName(requestedClassName);
                    // Lookup only when requested class extends Service
                    if (Service.class.isAssignableFrom(requestedClass)) {
                        // Find handler name for requested class
                        for (String name : handlerClassMap.keySet()) {
                            Class possibleClass = handlerClassMap.get(name);
                            if (requestedClass.isAssignableFrom(possibleClass)) {
                                // Get proper handler
                                pHandlerName = String.format("%s.%s", name, requestedMethod);
                                result = (XmlRpcHandler) handlerMap.get(pHandlerName);
                                if (result != null) {
                                    // Cache the handler to not perform the lookup again
                                    handlerMap.put(pHandlerName, result);
                                    return result;
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                exception = e;
            }
            throw new CommonReportSet.MethodNotDefinedException(exception, pHandlerName);
        }
        return result;
    }

    /**
     * Creates a new instance of {@link org.apache.xmlrpc.XmlRpcHandler}.
     *
     * @param pClass   The class, which was inspected for handler
     *                 methods. This is used for error messages only. Typically,
     *                 it is the same than <pre>pInstance.getClass()</pre>.
     * @param pMethods The method being invoked.
     */
    @Override
    protected XmlRpcHandler newXmlRpcHandler(final Class pClass,
            final Method[] pMethods) throws XmlRpcException
    {
        RequestProcessorFactoryFactory.RequestProcessorFactory factory = getRequestProcessorFactoryFactory()
                .getRequestProcessorFactory(pClass);
        return new RpcHandler(this, getTypeConverterFactory(),
                pClass, factory, pMethods);
    }
}
