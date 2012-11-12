package cz.cesnet.shongo.controller.api.xmlrpc;

import cz.cesnet.shongo.api.util.Options;
import cz.cesnet.shongo.api.xmlrpc.Service;
import cz.cesnet.shongo.fault.Fault;
import cz.cesnet.shongo.fault.SerializableException;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.common.*;
import org.apache.xmlrpc.metadata.Util;
import org.apache.xmlrpc.server.*;
import org.apache.xmlrpc.webserver.Connection;
import org.apache.xmlrpc.webserver.RequestData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Server for XML-RPC with improved type factory.
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class RpcServer extends org.apache.xmlrpc.webserver.WebServer
{
    private static Logger logger = LoggerFactory.getLogger(RpcServer.class);

    /**
     * Handler mapping, provide set of service instances
     * that will handler all XML-RPC requests.
     */
    private PropertyHandlerMapping handlerMapping;

    /**
     * Get host address by name
     *
     * @param host
     * @return host address
     */
    public static java.net.InetAddress getHostByName(String host)
    {
        if (host != null) {
            try {
                return InetAddress.getByName(host);
            }
            catch (UnknownHostException e) {
            }
        }
        return null;
    }

    /**
     * Construct XML-RPC web server
     *
     * @param host
     * @param pPort
     */
    public RpcServer(String host, int pPort)
    {
        super(pPort, getHostByName(host));

        handlerMapping = new HandlerMapping();
        handlerMapping.setTypeConverterFactory(
                new cz.cesnet.shongo.api.xmlrpc.TypeConverterFactory(Options.SERVER));
        handlerMapping.setRequestProcessorFactoryFactory(new RequestProcessorFactory());
        handlerMapping.setVoidMethodEnabled(true);

        XmlRpcServer xmlRpcServer = getXmlRpcServer();
        xmlRpcServer.setHandlerMapping(handlerMapping);

        XmlRpcServerConfigImpl serverConfig = (XmlRpcServerConfigImpl) xmlRpcServer.getConfig();
    }

    @Override
    protected XmlRpcStreamServer newXmlRpcStreamServer()
    {
        XmlRpcStreamServer server = new ConnectionServer();
        server.setTypeFactory(new cz.cesnet.shongo.api.xmlrpc.TypeFactory(server, Options.SERVER));
        return server;
    }

    /**
     * Set handlers from property file.
     * Each line in file should contain pair "handler_name=handler_class".
     *
     * @param mappingFile
     */
    public void setHandlerFromFile(String mappingFile)
    {
        try {
            handlerMapping.load(Thread.currentThread().getContextClassLoader(), mappingFile);
        }
        catch (Exception exception) {
            logger.error("Failed to load handler mappings from file.", exception);
        }
    }

    /**
     * Add handler by instance
     *
     * @param name
     * @param handler
     */
    public void addHandler(String name, Object handler)
    {
        try {
            handlerMapping.addHandler(name, handler.getClass());
        }
        catch (XmlRpcException exception) {
            logger.error("Failed to add new handler.", exception);
        }
        // Add instance to request processory factory
        RequestProcessorFactory factory = (RequestProcessorFactory) handlerMapping.getRequestProcessorFactoryFactory();
        factory.addInstance(handler);
    }

    @Override
    public void log(Throwable pError)
    {
        if (pError instanceof SocketException) {
            if (serverSocket == null) {
                return;
            }
        }
        super.log(pError);
    }

    /**
     * Stop XML-RPC server
     */
    public void stop()
    {
        shutdown();
        try {
            ServerSocket serverSocket = this.serverSocket;
            this.serverSocket = null;
            if (serverSocket != null) {
                serverSocket.close();
            }
        }
        catch (IOException exception) {
            throw new IllegalStateException(exception);
        }

    }

    /**
     * @param fault
     * @return fault converted to {@link XmlRpcException}
     */
    public static XmlRpcException convertException(Fault fault, Throwable cause)
    {
        String message;
        if (fault instanceof SerializableException) {
            Exception exception = (Exception) fault;
            message = SerializableException.Content.fromException(exception).toString();

        }
        else {
            message = fault.getMessage();
        }
        return new XmlRpcException(fault.getCode(), message, cause);
    }

    /**
     * Handler mapping that is able to translate full class name (which extends {@link Service})
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
    private static class HandlerMapping extends PropertyHandlerMapping
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
                    e.printStackTrace();
                }
                throw new XmlRpcNoSuchHandlerException("No such handler: " + pHandlerName);
            }
            return result;
        }

        /**
         * Creates a new instance of {@link XmlRpcHandler}.
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
            return new Handler(this, getTypeConverterFactory(),
                    pClass, factory, pMethods);
        }

        /**
         * {@link ReflectiveXmlRpcHandler} with conversion of {@link cz.cesnet.shongo.fault.Fault} to {@link XmlRpcException}.
         */
        public static class Handler implements XmlRpcHandler
        {
            private static class MethodData
            {
                final Method method;
                final TypeConverter[] typeConverters;

                MethodData(Method pMethod, org.apache.xmlrpc.common.TypeConverterFactory pTypeConverterFactory)
                {
                    method = pMethod;
                    Class[] paramClasses = method.getParameterTypes();
                    typeConverters = new TypeConverter[paramClasses.length];
                    for (int i = 0; i < paramClasses.length; i++) {
                        typeConverters[i] = pTypeConverterFactory.getTypeConverter(paramClasses[i]);
                    }
                }
            }

            private final AbstractReflectiveHandlerMapping mapping;
            private final MethodData[] methods;
            private final Class clazz;
            private final RequestProcessorFactoryFactory.RequestProcessorFactory requestProcessorFactory;

            public Handler(AbstractReflectiveHandlerMapping pMapping,
                    org.apache.xmlrpc.common.TypeConverterFactory pTypeConverterFactory,
                    Class pClass, RequestProcessorFactoryFactory.RequestProcessorFactory pFactory, Method[] pMethods)
            {
                mapping = pMapping;
                clazz = pClass;
                methods = new MethodData[pMethods.length];
                requestProcessorFactory = pFactory;
                for (int i = 0; i < methods.length; i++) {
                    methods[i] = new MethodData(pMethods[i], pTypeConverterFactory);
                }
            }

            private Object getInstance(XmlRpcRequest pRequest) throws XmlRpcException
            {
                return requestProcessorFactory.getRequestProcessor(pRequest);
            }

            public Object execute(XmlRpcRequest pRequest) throws XmlRpcException
            {
                AuthenticationHandler authHandler = mapping.getAuthenticationHandler();
                if (authHandler != null && !authHandler.isAuthorized(pRequest)) {
                    throw new XmlRpcNotAuthorizedException("Not authorized");
                }
                Object[] args = new Object[pRequest.getParameterCount()];
                for (int j = 0; j < args.length; j++) {
                    args[j] = pRequest.getParameter(j);
                }
                Object instance = getInstance(pRequest);
                for (int i = 0; i < methods.length; i++) {
                    MethodData methodData = methods[i];
                    TypeConverter[] converters = methodData.typeConverters;
                    if (args.length == converters.length) {
                        boolean matching = true;
                        for (int j = 0; j < args.length; j++) {
                            if (!converters[j].isConvertable(args[j])) {
                                matching = false;
                                break;
                            }
                        }
                        if (matching) {
                            for (int j = 0; j < args.length; j++) {
                                args[j] = converters[j].convert(args[j]);
                            }
                            return invoke(instance, methodData.method, args);
                        }
                    }
                }
                throw new XmlRpcException("No method matching arguments: " + Util.getSignature(args));
            }

            private Object invoke(Object pInstance, Method pMethod, Object[] pArgs) throws XmlRpcException
            {
                logger.debug("request: ->", pInstance.getClass().getSimpleName(), pMethod.getName());
                logger.debug("request: invoking '{}.{}'...", pInstance.getClass().getSimpleName(), pMethod.getName());
                try {
                    pMethod.setAccessible(true);
                    Object result = pMethod.invoke(pInstance, pArgs);
                    logger.debug("request: <-");
                    return result;
                }
                catch (IllegalAccessException e) {
                    throw new XmlRpcException("Illegal access to method "
                            + pMethod.getName() + " in class "
                            + clazz.getName(), e);
                }
                catch (IllegalArgumentException e) {
                    throw new XmlRpcException("Illegal argument for method "
                            + pMethod.getName() + " in class "
                            + clazz.getName(), e);
                }
                catch (InvocationTargetException e) {
                    Throwable throwable = e.getTargetException();
                    if (throwable instanceof XmlRpcException) {
                        throw (XmlRpcException) throwable;
                    }
                    else if (throwable instanceof Fault) {
                        XmlRpcException xmlRpcException = RpcServer.convertException((Fault) throwable,
                                throwable.getCause());
                        xmlRpcException.setStackTrace(throwable.getStackTrace());
                        throw xmlRpcException;
                    }
                    throw new XmlRpcInvocationException("Failed to invoke method "
                            + pMethod.getName() + " in class "
                            + clazz.getName() + ": "
                            + throwable.getMessage(), throwable);
                }
            }
        }
    }

    /**
     * We need to override request processor factory which creates instances of handler objects.
     * Default implementation create new instance for each request which is not desireable for
     * our purposes and this class overrides this mechanism to return existing instances
     * for specified class.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class RequestProcessorFactory
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

    /**
     * Connection server. Copied default implementation which overrides:
     * 1) convertThrowable method to allow use of cause from runtime exception
     * as fault.
     * 2) getRequest and writeResponse for logging of XML-RPC requests and response XMLs.
     *
     * @author Martin Srom <martin.srom@cesnet.cz>
     */
    private static class ConnectionServer extends XmlRpcHttpServer
    {
        @Override
        protected Throwable convertThrowable(Throwable pError)
        {
            if (pError instanceof RuntimeException || pError instanceof SAXException) {
                Throwable cause = pError.getCause();
                if (cause instanceof Fault) {
                    return RpcServer.convertException((Fault) cause, cause.getCause());
                }
            }
            if (pError instanceof XmlRpcException) {
                XmlRpcException xmlRpcException = (XmlRpcException) pError;
                Throwable cause = xmlRpcException.getCause();
                if (cause != null) {
                    Throwable newCause = convertThrowable(cause);
                    if (newCause != cause) {
                        return newCause;
                    }
                }
            }
            return pError;
        }

        @Override
        protected XmlRpcRequest getRequest(XmlRpcStreamRequestConfig pConfig, InputStream pStream)
                throws XmlRpcException
        {
            if (WebServerXmlLogger.isEnabled()) {
                pStream = WebServerXmlLogger.logRequest(pStream);
            }
            return super.getRequest(pConfig, pStream);
        }

        @Override
        protected void writeResponse(XmlRpcStreamRequestConfig pConfig, OutputStream pStream, Object pResult)
                throws XmlRpcException
        {
            if (WebServerXmlLogger.isEnabled()) {
                pStream = WebServerXmlLogger.logResponse(pStream);
            }
            RequestData data = (RequestData) pConfig;
            try {
                if (data.isByteArrayRequired()) {
                    super.writeResponse(pConfig, pStream, pResult);
                    data.getConnection().writeResponse(data, pStream);
                }
                else {
                    data.getConnection().writeResponseHeader(data, -1);
                    super.writeResponse(pConfig, pStream, pResult);
                    pStream.flush();
                }
            }
            catch (IOException e) {
                throw new XmlRpcException(e.getMessage(), e);
            }
        }

        @Override
        protected void writeError(XmlRpcStreamRequestConfig pConfig, OutputStream pStream,
                Throwable pError) throws XmlRpcException
        {
            RequestData data = (RequestData) pConfig;
            try {
                if (data.isByteArrayRequired()) {
                    super.writeError(pConfig, pStream, pError);
                    data.getConnection().writeError(data, pError, (ByteArrayOutputStream) pStream);
                }
                else {
                    data.getConnection().writeErrorHeader(data, pError, -1);
                    super.writeError(pConfig, pStream, pError);
                    pStream.flush();
                }
            }
            catch (IOException e) {
                throw new XmlRpcException(e.getMessage(), e);
            }
        }

        @Override
        protected void setResponseHeader(ServerStreamConnection pConnection, String pHeader, String pValue)
        {
            ((Connection) pConnection).setResponseHeader(pHeader, pValue);
        }

        @Override
        public void execute(XmlRpcStreamRequestConfig pConfig, ServerStreamConnection pConnection)
                throws XmlRpcException
        {
            super.execute(pConfig, pConnection);
        }
    }
}
