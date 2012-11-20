package cz.cesnet.shongo.controller.api.xmlrpc;

import cz.cesnet.shongo.api.util.ClassHelper;
import cz.cesnet.shongo.api.util.Options;
import cz.cesnet.shongo.api.xmlrpc.Service;
import cz.cesnet.shongo.api.xmlrpc.TypeConverterFactory;
import cz.cesnet.shongo.api.xmlrpc.TypeFactory;
import cz.cesnet.shongo.fault.CommonFault;
import cz.cesnet.shongo.fault.FaultException;
import cz.cesnet.shongo.fault.SerializableException;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.common.TypeConverter;
import org.apache.xmlrpc.common.XmlRpcInvocationException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a client for XML-RPC server.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RpcClient
{
    /**
     * XML-RPC client.
     */
    private XmlRpcClient client;

    /**
     * XML-RPC client factory for creating services.
     */
    private ClientFactory clientFactory;

    /**
     * Map of services by the class.
     */
    private Map<Class<? extends Service>, Service> serviceByClass = new HashMap<Class<? extends Service>, Service>();

    /**
     * @see {@link CommonFault}
     */
    private CommonFault fault = new CommonFault();

    /**
     * Constructor.
     */
    public RpcClient()
    {
    }

    /**
     * Constructor. Automatically perform {@link #connect(String, int)}.
     *
     * @param host
     * @param port
     * @throws Exception
     */
    public RpcClient(String host, int port) throws Exception
    {
        connect(host, port);
    }

    /**
     * @param fault sets the {@link #fault}
     */
    public void setFault(CommonFault fault)
    {
        this.fault = fault;
    }

    /**
     * Connect to a domain controller.
     *
     * @param host
     * @param port
     * @throws Exception
     */
    public void connect(String host, int port) throws Exception
    {
        // Start client
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(String.format("http://%s:%d", host, port)));
        client = new XmlRpcClient();
        client.setConfig(config);
        client.setTypeFactory(new TypeFactory(client, Options.CLIENT));

        // Connect to reservation service
        clientFactory = new ClientFactory(client, new TypeConverterFactory(Options.CLIENT));
    }

    /**
     * @param serviceClass
     * @return service from a domain controller for the given class.
     */
    public <T extends Service> T getService(Class<T> serviceClass)
    {
        Service service = serviceByClass.get(serviceClass);
        if (service == null) {
            service = serviceClass.cast(clientFactory.newInstance(serviceClass));
            serviceByClass.put(serviceClass, service);
        }
        return serviceClass.cast(service);
    }

    /**
     * Execute request on a domain controller.
     *
     * @param method
     * @param params
     * @return result
     * @throws XmlRpcException
     */
    public Object execute(String method, Object[] params) throws XmlRpcException
    {
        return client.execute(method, params);
    }

    /**
     * Execute request on a domain controller.
     *
     * @param method
     * @param params
     * @return result
     * @throws XmlRpcException
     */
    public Object execute(String method, List params) throws XmlRpcException
    {
        return client.execute(method, params);
    }

    /**
     * @param xmlRpcException
     * @return {@code xmlRpcException} converted to {@link FaultException}
     */
    public Exception convertException(XmlRpcException xmlRpcException)
    {
        SerializableException.Content content = new SerializableException.Content();
        content.fromString(xmlRpcException.getMessage());

        Exception exception;
        Class<? extends Exception> type = fault.getClasses().get(xmlRpcException.code);
        if (type != null) {
            exception = ClassHelper.createInstanceFromClassRuntime(type);
            if (exception instanceof SerializableException) {
                content.toException(exception);
            }
        }
        else {
            exception = new FaultException(xmlRpcException.code, content.getMessage(), xmlRpcException.getCause());
        }
        exception.setStackTrace(xmlRpcException.getStackTrace());
        return exception;
    }


    /**
     * Factory for creating {@link Service}s.
     */
    public class ClientFactory extends org.apache.xmlrpc.client.util.ClientFactory
    {
        private final org.apache.xmlrpc.common.TypeConverterFactory typeConverterFactory;

        public ClientFactory(XmlRpcClient pClient, org.apache.xmlrpc.common.TypeConverterFactory pTypeConverterFactory)
        {
            super(pClient, pTypeConverterFactory);

            typeConverterFactory = pTypeConverterFactory;
        }

        /**
         * Creates an object, which is implementing the given interface.
         * The objects methods are internally calling an XML-RPC server
         * by using the factories client.
         *
         * @param pClassLoader The class loader, which is being used for
         *                     loading classes, if required.
         * @param pClass       Interface, which is being implemented.
         * @param pRemoteName  Handler name, which is being used when
         *                     calling the server. This is used for composing the
         *                     method name. For example, if <code>pRemoteName</code>
         *                     is "Foo" and you want to invoke the method "bar" in
         *                     the handler, then the full method name would be "Foo.bar".
         */
        @Override
        public Object newInstance(ClassLoader pClassLoader, final Class pClass, final String pRemoteName)
        {
            return java.lang.reflect.Proxy.newProxyInstance(pClassLoader, new Class[]{pClass}, new InvocationHandler()
            {
                public Object invoke(Object pProxy, Method pMethod, Object[] pArgs) throws Throwable
                {
                    if (isObjectMethodLocal() && pMethod.getDeclaringClass().equals(Object.class)) {
                        return pMethod.invoke(pProxy, pArgs);
                    }
                    final String methodName;
                    if (pRemoteName == null || pRemoteName.length() == 0) {
                        methodName = pMethod.getName();
                    }
                    else {
                        methodName = pRemoteName + "." + pMethod.getName();
                    }
                    Object result;
                    try {
                        result = getClient().execute(methodName, pArgs);
                    }
                    catch (XmlRpcInvocationException e) {
                        Throwable t = e.linkedException;
                        if (t instanceof RuntimeException) {
                            throw t;
                        }
                        else if (t instanceof XmlRpcException) {
                            XmlRpcException xmlRpcException = (XmlRpcException) t;
                            t = new FaultException(xmlRpcException.code, xmlRpcException.getMessage(),
                                    xmlRpcException.getCause());
                        }
                        Class<?>[] exceptionTypes = pMethod.getExceptionTypes();
                        for (int i = 0; i < exceptionTypes.length; i++) {
                            Class<?> c = exceptionTypes[i];
                            if (c.isAssignableFrom(t.getClass())) {
                                throw t;
                            }
                        }
                        throw new UndeclaredThrowableException(t);
                    }
                    catch (XmlRpcException xmlRpcException) {
                        throw convertException(xmlRpcException);
                    }
                    TypeConverter typeConverter = typeConverterFactory.getTypeConverter(pMethod.getReturnType());
                    return typeConverter.convert(result);
                }
            });
        }
    }
}
