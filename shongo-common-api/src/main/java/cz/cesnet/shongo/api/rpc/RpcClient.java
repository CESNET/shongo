package cz.cesnet.shongo.api.rpc;

import cz.cesnet.shongo.CommonReportSet;
import cz.cesnet.shongo.api.ClassHelper;
import cz.cesnet.shongo.report.AbstractReportSet;
import cz.cesnet.shongo.report.ApiFault;
import cz.cesnet.shongo.report.ApiFaultString;
import cz.cesnet.shongo.report.AbstractReport;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.common.TypeConverter;
import org.apache.xmlrpc.common.XmlRpcInvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.ConnectException;
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
    private static Logger logger = LoggerFactory.getLogger(RpcClient.class);

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
     * Collection of {@link ApiFault} classes by fault code.
     */
    private Map<Integer, Class<? extends ApiFault>> apiFaultByCode = new HashMap<Integer, Class<? extends ApiFault>>();

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
     * @return XML-RPC configuration
     */
    public XmlRpcClientConfigImpl getConfiguration()
    {
        return (XmlRpcClientConfigImpl) client.getClientConfig();
    }

    /**
     * @param reportSet to be added to {@link #apiFaultByCode}
     */
    public void addReportSet(AbstractReportSet reportSet)
    {
        for (Class<? extends AbstractReport> reportClass : reportSet.getReportClasses()) {
            if (ApiFault.class.isAssignableFrom(reportClass)) {
                @SuppressWarnings("unchecked")
                Class<? extends ApiFault> apiReportClass = (Class<? extends ApiFault>) reportClass;
                ApiFault apiFault = ClassHelper.createInstanceFromClass(apiReportClass);
                apiFaultByCode.put(apiFault.getFaultCode(), apiReportClass);
            }
        }
    }

    /**
     * @param code
     * @return {@link cz.cesnet.shongo.report.AbstractReport} for given {@code code}
     */
    public Class<? extends ApiFault> getApiFaultClass(int code)
    {
        return apiFaultByCode.get(code);
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
        if (!host.startsWith("http")) {
            host = "http://" + host;
        }

        // Start client
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(host + ":" + port));
        client = new XmlRpcClient();
        client.setConfig(config);
        client.setTypeFactory(new TypeFactory(client));

        // Connect to reservation service
        clientFactory = new ClientFactory(client, new TypeConverterFactory());
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
     * @return {@code xmlRpcException} converted to proper {@link Exception}
     */
    protected Exception convertException(XmlRpcException xmlRpcException)
    {
        if (xmlRpcException.linkedException instanceof ConnectException) {
            return (ConnectException) xmlRpcException.linkedException;
        }
        Class<? extends ApiFault> type = getApiFaultClass(xmlRpcException.code);
        if (type == null) {
            return xmlRpcException;
        }
        ApiFault fault = ClassHelper.createInstanceFromClass(type);

        // Fill message
        String message = xmlRpcException.getMessage();
        if (ApiFaultString.isFaultString(message)) {
            ApiFaultString apiFaultString = new ApiFaultString();
            apiFaultString.parse(message);
            fault.readParameters(apiFaultString);
        }
        else if (fault instanceof CommonReportSet.UnknownErrorReport) {
            ((CommonReportSet.UnknownErrorReport)fault).setDescription(message);
        }
        else {
            logger.warn("Unknown Message: {}", message);
        }

        // Create exception for fault
        Exception exception = fault.getException();
        if (xmlRpcException.linkedException != null) {
            exception.setStackTrace(xmlRpcException.linkedException.getStackTrace());
        }
        else {
            exception.setStackTrace(xmlRpcException.getStackTrace());
        }
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
                        Throwable throwable = e.linkedException;
                        if (throwable instanceof RuntimeException) {
                            throw throwable;
                        }
                        else if (throwable instanceof XmlRpcException) {
                            XmlRpcException xmlRpcException = (XmlRpcException) throwable;
                            throwable = convertException(xmlRpcException);
                        }
                        Class<?>[] exceptionTypes = pMethod.getExceptionTypes();
                        for (int i = 0; i < exceptionTypes.length; i++) {
                            Class<?> c = exceptionTypes[i];
                            if (c.isAssignableFrom(throwable.getClass())) {
                                throw throwable;
                            }
                        }
                        throw new UndeclaredThrowableException(throwable);
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
