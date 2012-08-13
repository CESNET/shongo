package cz.cesnet.shongo.controller;

import cz.cesnet.shongo.api.util.Options;
import cz.cesnet.shongo.controller.api.ControllerFault;
import cz.cesnet.shongo.controller.api.xmlrpc.TypeConverterFactory;
import cz.cesnet.shongo.controller.api.xmlrpc.TypeFactory;
import cz.cesnet.shongo.fault.FaultException;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.common.TypeConverter;
import org.apache.xmlrpc.common.XmlRpcInvocationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.util.List;

/**
 * Client for a domain controller from Shongo.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class ControllerClient
{
    /**
     * XML-RPC client.
     */
    XmlRpcClient client;

    /**
     * XML-RPC client factory for creating services.
     */
    ClientFactory clientFactory;

    /**
     * @see {@link ControllerFault}
     */
    private static final ControllerFault controllerFault = new ControllerFault();

    /**
     * Constructor.
     */
    public ControllerClient()
    {
    }

    /**
     * Constructor. Automatically perform {@link #connect(String, int)}.
     *
     * @param host
     * @param port
     * @throws Exception
     */
    public ControllerClient(String host, int port) throws Exception
    {
        connect(host, port);
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
    public <T> T getService(Class<T> serviceClass)
    {
        return (T) clientFactory.newInstance(serviceClass);
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
    public static FaultException convertException(XmlRpcException xmlRpcException)
    {
        FaultException.Message message = new FaultException.Message();
        message.fromString(xmlRpcException.getMessage());

        Class<? extends Exception> type = controllerFault.getClasses().get(xmlRpcException.code);
        if (type != null && FaultException.class.isAssignableFrom(type)) {
            @SuppressWarnings("unchecked")
            Class<? extends FaultException> newType = (Class<? extends FaultException>) type;
            try {
                Constructor<? extends FaultException> constructor =
                        newType.getDeclaredConstructor(FaultException.Message.class);
                FaultException faultException = constructor.newInstance(message);
                faultException.setCode(xmlRpcException.code);
                return faultException;
            }
            catch (NoSuchMethodException exception) {
                throw new IllegalStateException("Exception '" + type.getCanonicalName()
                        + "' doesn't have constructor with '" + FaultException.Message.class.getCanonicalName()
                        + "' parameter.", exception);
            }
            catch (Exception exception) {
                throw new IllegalStateException("Cannot instance exception type " + type.getCanonicalName(),
                        exception);
            }
        }
        return new FaultException(xmlRpcException.code, message.getMessage());
    }

    public static class ClientFactory extends org.apache.xmlrpc.client.util.ClientFactory
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
                        Class[] exceptionTypes = pMethod.getExceptionTypes();
                        for (int i = 0; i < exceptionTypes.length; i++) {
                            Class c = exceptionTypes[i];
                            if (c.isAssignableFrom(t.getClass())) {
                                throw t;
                            }
                        }
                        throw new UndeclaredThrowableException(t);
                    }
                    catch (XmlRpcException exception) {
                        FaultException faultException = convertException(exception);
                        faultException.setStackTrace(exception.getStackTrace());
                        throw faultException;
                    }
                    TypeConverter typeConverter = typeConverterFactory.getTypeConverter(pMethod.getReturnType());
                    return typeConverter.convert(result);
                }
            });
        }
    }
}
